package side;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class Client {
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		DatagramSocket sock = null;
		
		switch (args.length) {
		case 1:
			switch (args[0]) {
			case "-s":
				Server srv = new Server();
				break;

			case "-h":
				System.out.println("Option:");
				System.out.println("-s mode serveur");
				System.out.println("-h ip");
				System.out.println("-p port");
				break;
				
			default:
				break;
			}
			break;
			
		case 3:
			int port =0;
			if ((args[0].equals("-s")) && (args[1].equals("-p"))){
				port = Integer.parseInt(args[2]);
			} else if ((args[2].equals("-s")) && (args[0].equals("-p"))){
				port = Integer.parseInt(args[1]);
			}
			if (port != 0) {
				new Server(port);
			} else {
				new Server();
			}
			break;
			
		case 4:
			if ((args[0].equals("-p")) && (args[2].equals("-h"))){
				Connect(args[3], Integer.parseInt(args[1]));
			} else if ((args[2].equals("-p")) && (args[0].equals("-h"))){
				Connect(args[1], Integer.parseInt(args[3]));
			}
			break;
			
		default:
			try{
				sock = new DatagramSocket();
				sock.setBroadcast(true);		
				
				byte[] sendData = "DISCOVER_SERVER_REQUEST".getBytes();

				//Try the 255.255.255.255 first
				try {
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), 50000);
					sock.send(sendPacket);
					System.out.println(">>> Request packet sent to: 255.255.255.255 (DEFAULT)");
				} catch (Exception e) {
				}

				// Broadcast the message over all the network interfaces
				Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
				while (interfaces.hasMoreElements()) {
					NetworkInterface networkInterface = (NetworkInterface) interfaces.nextElement();

					if (networkInterface.isLoopback() || !networkInterface.isUp()) {
						continue; // Don't want to broadcast to the loopback interface
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
							System.out.println(">>> Request packet sent to: " + broadcast.toString());
						} catch (Exception e) {
						}
					}
				}

				System.out.println(">>> Waiting for a reply!");

				//Wait for a response
				byte[] recvBuf = new byte[15000];
				DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
				sock.setSoTimeout(10000);
				sock.receive(receivePacket);

				//We have a response
				System.out.println(">>> Broadcast response from server: " + receivePacket.getAddress().getHostAddress());

				//Check if the message is correct
				String message = new String(receivePacket.getData()).trim();
				if (message.contains("DISCOVER_SERVER_RESPONSE")) {
					String extract = message.substring("DISCOVER_SERVER_RESPONSE".length() + 1);
					Connect(receivePacket.getAddress().getHostAddress(), Integer.parseInt(extract));
				}

				//Close the port!
				sock.close();
			} catch (SocketTimeoutException ste){
				Server srv = new Server();
			} catch (IOException ex) {
			  
			}
			break;
		}
	}
	
	private static void Connect(String serverName, int port){
		System.out.println("Connecting to " + serverName + " on port " + port);
		Socket client = null;
		try {
			client = new Socket(serverName, port);
		} catch (UnknownHostException e) {
			System.err.println("Destination not found : " + e.getLocalizedMessage());
		} catch (IOException e) {
			System.err.println("Socket creation failed : " + e.getLocalizedMessage());
		}
		if( client != null ) {
			System.out.println("Connected to " + client.getRemoteSocketAddress());
			OutputStream outToServer = null;
			try {
				outToServer = client.getOutputStream();
			} catch (IOException e) {
				System.err.println("get output stream failed : " + e.getLocalizedMessage());
			}
			if( outToServer != null ) {
				PrintStream out = new PrintStream(outToServer);
				out.println("Connecting from " + client.getLocalSocketAddress());
				BufferedReader in = null;
				try {
					in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				} catch (IOException e) {
					System.err.println("get input stream failed : " + e.getLocalizedMessage());
				}
				if( in != null ) {
					try {
						System.out.println("Server says " + in.readLine());
					} catch (IOException e) {
						System.err.println("read input failed : " + e.getLocalizedMessage());
					}
				}
			}
			try {
				client.close();
			} catch (IOException e) {
				System.err.println("Socket close failed : " + e.getLocalizedMessage());
			}
		}
	}
	
	private static class GetMessages extends Thread {
		
	}
	
	private static class AddMessage extends Thread {
		
	}
}
