package side;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Server {

	private static final short DEFAULT_PORT = 4200; 
	private static boolean loop = true;
	private static int port = DEFAULT_PORT;
	
	public Server(){
		new Server(DEFAULT_PORT);
	}
	
	public Server(int pPort) {
		// TODO Auto-generated method stub
		
		DiscoveryService discovery = new DiscoveryService();
		discovery.start();
		
		port = pPort;
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println("Server socket creation failed : " + e.getLocalizedMessage());
		}
		if( serverSocket != null ) {
			System.out.println("Listening on port " + port);
			while (loop) {
				Socket serviceSocket = null;
				try {
					serviceSocket = serverSocket.accept();
					new TCPService(serviceSocket).start();
				} catch (IOException e) {
					System.err.println("Accept failed : " + e.getLocalizedMessage());
				}
			}
			try {
				serverSocket.close();
			} catch (IOException e) {
				System.err.println("Server socket close failed : " + e.getLocalizedMessage());
			}
			System.out.println("Server finished");
		}
	}
	
	private static class TCPService extends Thread {
		private Socket serviceSocket;
		
		public TCPService(Socket s) {
			serviceSocket = s;
		}

		@Override
		public void run() {
			
			System.out.println("Connected to client : " + serviceSocket.getInetAddress().getHostAddress());
			BufferedReader in;
			try {
				in = new BufferedReader(new InputStreamReader(serviceSocket.getInputStream()));
				PrintStream out;
				try {
					out = new PrintStream(serviceSocket.getOutputStream());
					String message;
					try {
						message = in.readLine();
						if( message.equalsIgnoreCase("stop") ) {
							out.println("Server Stop");
							loop = false;
						} else {
							out.println("Hi your connection source port is : " + serviceSocket.getPort());
						}
					} catch (IOException e) {
						System.err.println("Read from socket failed : " + e.getLocalizedMessage());
						loop = false;
					}
				} catch (IOException e) {
					System.err.println("Input reader creation failed : " + e.getLocalizedMessage());
					loop = false;
				}
			} catch (IOException e) {
				System.err.println("Output reader creation failed : " + e.getLocalizedMessage());
				loop = false;
			}
			try {
				serviceSocket.close();
			} catch (IOException e) {
				System.err.println("Close socket failed : " + e.getLocalizedMessage());
				loop = false;
			}
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
						// TODO Auto-generated catch block
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
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
			        }
				}
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
}
