/**
 * 
 */
package com.wfong.nodes;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import com.wfong.token.STPLPFrame;


/**
 * This class is the Node Superclass. It contains all the methods to receive and transmit data.
 * @author William Fong
 */
public class Node{
	private int NodeID;
	private ServerSocket inputSocket;
	private List<Socket> outputSockets;
	
	/**
	 * This is the default constructor for a Node
	 */
	protected Node() {
		super();
		this.outputSockets = new ArrayList<Socket>();
		//Run this instantiated object in a new thread
	}
	
	/**
	 * This constructor allows for the specification of the Node's name.
	 * @param NodeName The Node Name
	 */
	protected Node(int NodeName) {
		System.out.println("Creating Node: " + NodeName);
		this.NodeID = NodeName;
		this.outputSockets = new ArrayList<Socket>();
	}
	
	/**
	 * This method handles getting the LocalHost address
	 * @return The LocalHost IP
	 * @TODO Move this method to the Node Superclass, it is used by all of the subclasses
	 */
	public InetAddress getLocalAddress() {
		try {
			return InetAddress.getByName("::1");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * This method attempts to add a Server Socket to the Node.
	 * @param port The port to attempt to bind the Socket to
	 * @param address The address associated with the Socket (Right now is LocalHost)
	 */
	public void addServerSocket(int port, InetAddress address) {
		try {
			this.inputSocket = new ServerSocket(port, 50, address);
			
		} catch (IOException e) {
			System.err.println("Error! " + this.NodeID + " had a port number conflict!");
		} catch (IllegalArgumentException e) {
			System.err.println("Error! " + this.NodeID + " could not create Socket with port number: " + port + ", port number is out of range!");
		}
	}
	
	/**
	 * This method attempts to create a Server Socket to the node.
	 * @param address
	 * @return The port number the Socket is listening to.
	 */
	public int addServerSocket(InetAddress address) {
		//Iterate through all non-system critical ports until we find a free port
		for (int i = 1025; i < 49151; i++) {
			//System.out.println("Node " + this.NodeName + ": Attempting port " + i);
			try {
				this.inputSocket = new ServerSocket(i, 50, address);
				return i;
			} catch (IOException e) {
				//System.err.println("Error! " + this.NodeName + " had a port number conflict...");
				continue;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
		//Severe error
		System.err.println("ERROR! " + this.NodeID + " could not find a free port number!");
		return 0;
	}
	
	/**
	 * This method attempts to create a socket that is bound to a specified Port with the associated IP address
	 * @param port The port to attempt to bind the socket to
	 * @param address The IP address associated with the socket (Right now is LocalHost)
	 */
	public void addOutputSocket(int port, InetAddress address) {
		try {
			this.outputSockets.add(new Socket(address, port));
			System.out.println("Node " + this.NodeID + " created output socket to " + this.outputSockets.get(0).toString());
		} catch (UnknownHostException e) {
			System.err.println(this.NodeID + ": Could not resolve IP!");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println(this.NodeID + ": Could not create socket! Timeout...");
			try {
				Thread.sleep(1000);
				addOutputSocket(port, address);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}
	
	
	/**
	 * This method listens to the first inputSocket for accept calls.
	 * This method DOES NOT close the connected socket, it must be closed by another method.
	 * @return A socket to the connected client
	 */
	@SuppressWarnings("resource")
	private Socket acceptClient() {
		if (!this.inputSocket.isBound()) {
			System.err.println(this.NodeID + " Cannot accept call! Listening sockets do not exist!");
			return null;
		}
		Socket clientSocket = new Socket();
		while(!clientSocket.isBound()) {
			try {
				System.out.println(this.NodeID + " listening to connection requests...");
				clientSocket = this.inputSocket.accept();
				System.out.println(this.NodeID + ": Client accepted on Socket: " + clientSocket.toString());
				//Connection to a client has now been established
				return clientSocket;
			} catch (SocketTimeoutException T){
				System.out.println("Server listen timeout...");
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}
		return clientSocket;
	}
	
	/**
	 * This method returns a read STPLP Frame from the socket
	 * @return The received STPLP Frame
	 */
	public STPLPFrame readSocket() {
		Socket clientSocket = acceptClient();
		DataInputStream input = null;
		try {
			input = new DataInputStream(clientSocket.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		STPLPFrame frame;
		//Create buffer of maximum possible frame size
		byte[] buffer = new byte[260];
		try {
			input.read(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		frame = new STPLPFrame(buffer);
		return frame;
	}
	
	/**
	 * This method attempts to write a String to a Socket output stream.
	 * @param message The message (a string) to be written to the socket
	 * @param outputSocket The output socket
	 */
	@Deprecated
	public void writeToSocket(String message) {
		//System.out.println(this.NodeName + ": Attempting to write to socket...");
		Socket outputSocket = this.outputSockets.get(0);
		PrintStream outputStream;
		try {
			//Create output stream
			outputStream = new PrintStream(outputSocket.getOutputStream());
			//Send message
			outputStream.println(message);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error! Node: " + this.NodeID + " could not write to socket!");
			return;
		}
		return;
	}
	
	/**
	 * This method attempts to write a STPLP Frame to the output socket.
	 * @param frame The STPLP frame to be transmitted
	 * @return Returns 0 upon successful transmission
	 */
	public int writeToSocket(STPLPFrame frame) {
		Socket outputSocket = this.outputSockets.get(0);
		DataOutputStream outputStream;
		try {
			//Create output stream
			outputStream = new DataOutputStream(outputSocket.getOutputStream());
			//Send message
			outputStream.write(frame.getFrame());
			outputStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error! Node: " + this.NodeID + " could not write to socket!");
			return 1;
		}
		return 0;
	}
	
	/**
	 * Kills a connection to the server and properly closes all streams and sockets.
	 * @throws IOException 
	 */
	public void killServerConnection() throws IOException {
		Socket outputSocket = this.outputSockets.get(0);
		if(outputSocket.isClosed()) {
			System.err.println(this.NodeID + ": Error! Could not close Socket: Socket is already closed...");
		} else {
			if(outputSocket.isConnected()) {
				writeToSocket("terminate");
				outputSocket.shutdownOutput();
			}
			outputSocket.close();
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Node [NodeName=");
		builder.append(NodeID);
		builder.append(", inputSocket=");
		builder.append(inputSocket);
		builder.append(", outputSockets=");
		builder.append(outputSockets);
		builder.append("]");
		return builder.toString();
	}
}
