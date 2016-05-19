package side;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import model.Message;

public class Server {
	/**
	 * Attributs
	 */
	private static short PORT_NUMBER_SEND = 10876;
	private static final short DEFAULT_PORT = 4200; 
	private static boolean loop;
	private static int port = DEFAULT_PORT;
	private static ArrayList<ClientThread> clientList;
	private SimpleDateFormat sdf;
	private DiscoveryService discovery;
	
	/**
	 * Constructeur
	 */
	public Server(){
		new Server(DEFAULT_PORT);
	}
	
	public Server(int pPort) {
		//Liste des clients
		clientList = new ArrayList<ClientThread>();
		port = pPort;
		// formattage de la date
		sdf = new SimpleDateFormat("HH:mm:ss");
		//activation du mode discover
		discovery = new DiscoveryService();
	}
	
	public void setPort(int port){
		this.port = port;
	}
	
	public void start() {
		loop = true;
		if(discovery == null){
			discovery = new DiscoveryService();
		}
		discovery.start();
		
		ServerSocket serverSocket = null;
		
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println("Server socket creation failed : " + e.getLocalizedMessage());
		}
		if( serverSocket != null ) {
			System.out.println("Listening on port " + port);
			while (loop) {
				try {
					Socket clientSocket = serverSocket.accept();
					
					ClientThread clientTh = new ClientThread(clientSocket);
					clientList.add(clientTh);
					clientTh.start();
					
				} catch (IOException e) {
					System.err.println("Accept failed : " + e.getLocalizedMessage());
				}
			}
			// Arret en cours: loop est à faux.
			try {
				serverSocket.close();
				for(int i = 0; i < clientList.size(); i++) 
					clientList.get(i).close();
			} catch (IOException e) {
				System.err.println("Server socket close failed : " + e.getLocalizedMessage());
			}
			System.out.println("Server finished");
		}
	}

	/**
	 * Affiche d'un évènement au serveur
	 * @param message Event à afficher
	 */
	private void display(String message){
		if(sdf == null) sdf = new SimpleDateFormat("HH:mm:ss");
		
		String time = sdf.format(new Date()) + " " + message;
		System.out.println(time);
	}
	
	private synchronized void broadcast(String message){
		String time = sdf.format(new Date());
		String messageBis = time + " " + message + "\n";
		
		System.out.println(messageBis);
		
		for(int i = clientList.size(); i > 0; i--){
			ClientThread ct = clientList.get(i-1);
			
			if(!ct.writeMsg(messageBis)){
				clientList.remove(ct);
				display(ct.username + " supprimé de la liste client");
			}
		}
	}
	
	private synchronized void remove(ClientThread ct){
		clientList.remove(ct);
	}
	
	private class ClientThread extends Thread {
		Socket socket;
		private ObjectInputStream sInput;
		private ObjectOutputStream sOutput;
		
		private String username;
		private Message msg;
		private String date;
		
		public ClientThread(Socket socket){
			//id = ++uniqueId;
			this.socket = socket;
			
			System.out.println("Thread / Creation Input/Output Streams");
			
			try{
				// Crétation du Output
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				//Création du Input
				sInput = new ObjectInputStream(socket.getInputStream());
				//read the username
				username = (String) sInput.readObject();
				display(username + " just connected");
			} catch (IOException e) {
				display("Exception during creation of new Input/Output stream: " + e);
				return;
			} catch (ClassNotFoundException e2){
				return;
			}
			
			date = new Date().toString() + "\n";
		}
		
		public void run(){
			boolean loop = true;
			while(loop){
				try{
					msg = (Message) sInput.readObject();
				} catch (IOException e){
					display(username + " Exception reading Streams: " + e);
					break;
				} catch (ClassNotFoundException e2) {
					break;
				}
				
				String message = msg.getMessage();
				switch(msg.getType()){
				case Message.MESSAGE:
					broadcast(username + ": " + message);
					break;
				case Message.LOGOUT:
					broadcast(username + " déconnecté.");
					loop = false;
					break;
				case Message.WHOISIN:
					writeMsg("Liste des utilisateurs connecté à " + sdf.format(new Date()) + "\n");
					for(int i = 0; i < clientList.size(); ++i){
						ClientThread ct = clientList.get(i);
						
						writeMsg((i+1) + "- " + ct.username + " depuis " + ct.date);
					}
					break;
				case Message.USERNAME:
					this.username = message;
					break;
				}
			}
			remove(this);
		}
		
		private void close(){
			try{
				if(sOutput != null) sOutput.close();
			} catch (Exception e) {}
			try{
				if(sInput != null) sInput.close();
			} catch (Exception e) {}
			try{
				if(socket != null) socket.close();
			} catch (Exception e) {}
		}
		
		private boolean writeMsg(String msg){
			if(!socket.isConnected()){
				close();
				return false;
			}
			try{
				sOutput.writeObject(msg);
			} catch (IOException e){
				display("Erreur de l'envoi à l'utilisateur: " + username);
				display(e.toString());
			}
			return true;
		}

	}
	
	private static class DiscoveryService extends Thread {
		
		private DatagramSocket sockserv = null;
		
		@Override
		public void run() {
			try {
				sockserv = new DatagramSocket(50000, InetAddress.getLocalHost());
				sockserv.setBroadcast(true);
				
				while (true) {
			        System.out.println(">>>Ready to receive broadcast packets!");
			        
			        //Receive a packet
			        byte[] recvBuf = new byte[15000];
			        DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
			        try {
						sockserv.receive(packet);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
			        if ( packet != null){
			        	//Packet received
						System.out.println(getClass().getName() + ">>>Discovery packet received from: " + packet.getAddress().getHostAddress());
				        
						//See if the packet holds the right command (message)
				        String message = new String(packet.getData()).trim();
				        if (message.equals("DISCOVER_SERVER_REQUEST")) {
					        byte[] sendData = ("DISCOVER_SERVER_RESPONSE_" + port ).getBytes();
					        
					        //Send a response
					        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
					        try {
								sockserv.send(sendPacket);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
			        }
				}
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		
	}
}
