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
	
	
	/**
	 * Démarrage
	 * @param args
	 */
 	public static void main(String[] args) {
		Client cl = new Client();
		DatagramSocket sock = null;
		Server srv = null;
		
		int test = args.length;
		
		if((args.length == 1) && (args[0].equals("-s"))){
			srv = new Server();
		}
		
		for(int i = 0; i < args.length -1; i++){
			switch(args[i]){
				case "-h":
					showHelp();
					break;
				case "--help":
					showHelp();
					break;
				case "-p":
					cl.port = Integer.parseInt(args[i+1]);
					break;
				case "-i":
					cl.server = args[i+1];
					discover = false;
					break;
				case "-u":
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
				
				if(msg.equalsIgnoreCase("LOGOUT")){
					cl.sendMessage(new Message(Message.LOGOUT, ""));
					send = false;
				} else if (msg.equalsIgnoreCase("WHOISIN")){
					cl.sendMessage(new Message(Message.WHOISIN, ""));
				} else {
					cl.sendMessage(new Message(Message.MESSAGE, msg));
				}
			}
			cl.disconnect();
		}
	}
	
 	private static void showHelp(){
 		System.out.println("Option:");
		System.out.println("-s lancement du serveur");
		System.out.println("-i ip du serveur cible");
		System.out.println("-p port du serveur cible");
		System.out.println("-u username");
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
	 * @param cl
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

			// Broadcast the message over all the network interfaces
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

					// Send the broadcast package!
					try {
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 8888);
						sock.send(sendPacket);
						display(">>> Request packet sent to: " + broadcast.toString());
					} catch (Exception e) {
					}
				}
			}

			display(">>> Waiting for a reply!");

			//Wait for a response
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

			//Close the port!
			sock.close();
		} catch (SocketTimeoutException ste){
			Server srv = new Server();
			display("Aucun serveur découvert, lancement du serveur");
			srv.start();
		} catch (IOException ex) {}
	}
	
	private boolean connect(){
		display("Connecting to " + server + " on port " + port);
		Socket soc = null;
		try {
			soc = new Socket(server, port);
		} catch (UnknownHostException e) {
			display("Destination not found : " + e.getLocalizedMessage());
			return false;
		} catch (IOException e) {
			display("Socket creation failed : " + e.getLocalizedMessage());
			return false;
		}
		if( soc != null ) {
			display("Connected to " + soc.getRemoteSocketAddress());
			try {
				sInput = new ObjectInputStream(soc.getInputStream());
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
					String msg = (String) sInput.readObject();

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
