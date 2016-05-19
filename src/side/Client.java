package side;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Scanner;

import model.Message;

public class Client {
	
	/**
	 * Attributs
	 */
	private static final int DISCOVER_PORT = 50000, TIMEOUT = 10000;
	private static final String DISCOVER_RESP = "DISCOVER_SERVER_RESPONSE";
	private ObjectInputStream sInput;
	private ObjectOutputStream sOutput;
	private Socket socket;
	private String server, username;
	private int port;
	private static boolean send = true, discover = true;
	
	/***
	 * Constructeur
	 */
	public Client(){
		this("Anonymous","",0);
	}
	
	public Client(String username){
		this(username, "", 0);
	}
	
	public Client(String server, int port){
		this("Anonymous", server, port);
	}
	
	public Client(String username, String server, int port){
		this.server = server;
		this.port = port;
		this.username = username;
	}
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Démarrage
	 * @param args
	 */
 	public static void main(String[] args) {
		Client cl = new Client();
		Server srv = null;
		
		if((args.length == 1) && (args[0].equals("-s"))){
			srv = new Server();
		}
		
		for(int i = 0; i < args.length -1; i++){
			switch(args[i]){
				case "-h": // affichage de l'aide
					showHelp();
					break;
				case "--help": // affichage de l'aide
					showHelp();
					break;
				case "-p": // spécification du port
					cl.port = Integer.parseInt(args[i+1]);
					break;
				case "-i": // spécification de l'ip du serveur
					cl.server = args[i+1]; 
					discover = false;
					break;
				case "-u": // spécification du username
					cl.username = args[i+1];
					break;
				default:
					;
			}
		}
		
		if(srv != null){
			if (cl.port != 0)
				srv.setPort(cl.port);
			srv.start();
		} else {
			if(discover) cl.discover();
			
			if(!cl.connect()) return;
			
			Scanner scan = new Scanner(System.in);
			while(send){
				System.out.println("> ");
				String msg = scan.nextLine();
				
				if(msg.startsWith("/username")){
					String[] username2 = msg.split(" ");
					if (username2.length >= 2)
					{
						cl.setUsername(username2[1]);
						cl.sendMessage(new Message(Message.USERNAME, username2[1]));
						cl.display("Votre username est maintenant : " + cl.getUsername());
					} else {
						cl.display("Vous devez spécifier un username");
					}
				} else if(msg.equalsIgnoreCase("LOGOUT")){
					//déconnexion
					cl.sendMessage(new Message(Message.LOGOUT, ""));
					send = false;
				} else if (msg.equalsIgnoreCase("WHOISIN")){
					// affiche la liste des personnes connectées
					cl.sendMessage(new Message(Message.WHOISIN, ""));
				} else {
					// envoit le message
					cl.sendMessage(new Message(Message.MESSAGE, msg));
				}
			}
			scan.close();
			cl.disconnect();
		}
	}
	
 	/**
 	 * Affiche le message d'aide.
 	 */
 	private static void showHelp(){
 		System.out.println("Option:");
		System.out.println("-s lancement du serveur");
		System.out.println("-i ip du serveur cible");
		System.out.println("-p port du serveur cible");
		System.out.println("-u username");
		System.out.println("-h affiche cette aide");
		System.out.println("");
		System.out.println("Message chat:");
		System.out.println("/username username	permet de changer de username");
		System.out.println("whoisin	affiche la liste des personnes connectées");
		System.out.println("logout	déconnecte la session client du serveur");
	}
 	
 	/////////// Methods ///////////
 	
 	/**
 	 * Affiche un message d'évènement/erreurs
 	 * @param msg Message à afficher
 	 */
	private void display(String msg){
		System.out.println(msg);
	}
	
	/**
	 * Envoit un message au serveur
	 * @param msg Message à envoyer au serveur
	 */
	private void sendMessage(Message msg){
		try{
			sOutput.writeObject(msg);
		} catch (IOException e){
			display(e.toString());
		}
	}
	
	/**
	 * Ferme toutes les connexions du client
	 */
	private void disconnect(){
		try{
			if(sInput != null) sInput.close();
		} catch (IOException e){}
		try{
			if(sOutput != null) sOutput.close();
		} catch (IOException e){}
		try{
			if(socket != null) socket.close();
		} catch (IOException e){}
	}

	/**
	 * Permet de recherche un serveur sur le réseau
	 */
	private void discover(){
		try{
			DatagramSocket sock = new DatagramSocket();
			sock.setBroadcast(true);		
			
			byte[] sendData = "DISCOVER_SERVER_REQUEST".getBytes();

			//Try the 255.255.255.255 first
			try {
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), DISCOVER_PORT);
				sock.send(sendPacket);
				display(">>> Request packet sent to: 255.255.255.255 (DEFAULT)");
			} catch (Exception e) {
			}

			// Broadcast sur toutes les interfaces de l'ordinateur
			Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface networkInterface = (NetworkInterface) interfaces.nextElement();

				if (!networkInterface.isUp()) {
					continue; // on ne fait rien si l'interface est éteinte
				}
				
				for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
					InetAddress broadcast = interfaceAddress.getBroadcast();
					if (broadcast == null) {
						continue;
					}

					// Envoit du packet de broadcast
					try {
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 8888);
						sock.send(sendPacket);
						display(">>> Request packet sent to: " + broadcast.toString());
					} catch (Exception e) {
					}
				}
			}

			display(">>> Waiting for a reply!");

			//En attente de réponse
			byte[] recvBuf = new byte[15000];
			DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
			sock.setSoTimeout(TIMEOUT);
			sock.receive(receivePacket);

			//On a une réponse du serveur
			display(">>> Broadcast response from server: " + receivePacket.getAddress().getHostAddress());

			//Vérification du message de retour
			String message = new String(receivePacket.getData()).trim();
			if (message.contains(DISCOVER_RESP)) {
				String extract = message.substring(DISCOVER_RESP.length() + 1);
				this.server = receivePacket.getAddress().getHostAddress();
				this.port = Integer.parseInt(extract);
			}

			//Fermeture socket
			sock.close();
		} catch (SocketTimeoutException ste){
			Server srv = new Server();
			display("Aucun serveur découvert, lancement du serveur");
			srv.start();
		} catch (IOException ex) {}
	}
	
	/**
	 * Lance la connexion au serveur
	 * @return
	 */
	private boolean connect(){
		display("Connecting to " + server + " on port " + port);
		Socket soc = null;
		try {
			//création socket de connexion
			soc = new Socket(server, port);
		} catch (UnknownHostException e) {
			display("Destination not found : " + e.getLocalizedMessage());
			return false;
		} catch (IOException e) {
			display("Socket creation failed : " + e.getLocalizedMessage());
			return false;
		}
		if( soc != null ) {
			// Affichage d'un messgae si connexion réussie
			display("Connected to " + soc.getRemoteSocketAddress());
			try {
				// Entrée (serveur -> client)
				sInput = new ObjectInputStream(soc.getInputStream());
				//Sortie (client -> serveur)
				sOutput = new ObjectOutputStream(soc.getOutputStream());
			} catch (IOException e){
				display(e.toString());
				return false;
			}
			
			// Ecoute des messages envoyés par le serveur
			new ListenMessage().start();
			
			try{
				sOutput.writeObject(username);
			} catch (IOException e){
				display(e.toString());
				disconnect();
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Classe permettant l'écoute sur le socket des messages envoyés par le serveur
	 * @author damienveugeois
	 *
	 */
	private class ListenMessage extends Thread{

		private boolean listen = true;
		
		public void run(){
			while(listen){
				try{
					// lit le message du serveur
					String msg = (String) sInput.readObject();

					//affiche le message
					System.out.println(msg);
					System.out.print("> ");
					
				} catch (IOException e){
					display(e.toString());
				} catch (ClassNotFoundException e2){
					display(e2.toString());
				}
			}
		}
	}
	
}
