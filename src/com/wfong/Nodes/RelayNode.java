package com.wfong.nodes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * This class relays data between client and server nodes (Acting as both)
 * @author William Fong
 *
 */
public class RelayNode extends Node implements Runnable {
	private InetAddress myAddress;
	private InetAddress serverAddress;
	private String configMessage;
	private int port;
	
	/**
	 * This constructor allows for specification of the Node's Name as well as the receiving and sending port numbers
	 * @param NodeName The Node's Name
	 * @param myPort The Nodes's Server Socket Port number
	 * @param serverPort The Node's output Port number
	 */
	public RelayNode(int NodeName, int myPort, int serverPort) {
		super(NodeName);
		this.myAddress = getLocalAddress();
		this.serverAddress = getLocalAddress();
		this.addServerSocket(myPort, myAddress);
		this.addOutputSocket(serverPort, serverAddress);
	}
	
	public RelayNode(int NodeName) {
		super(NodeName);
		this.myAddress = getLocalAddress();
		this.serverAddress = getLocalAddress();
		this.port = this.addServerSocket(myAddress);
		System.out.println("Listening on Port: " + this.port);
	}
	
	/**
	 * This constructor creates a node using a specified config file
	 * @param NodeName The Node's Name
	 * @param FilePath The file path to the config file
	 * @throws NumberFormatException Is thrown when the config file's port number section is incorrectly formatted
	 * @throws IOException Is thrown when the config file does not exist
	 */
	public RelayNode(int NodeName, String FilePath) throws NumberFormatException, IOException {
		super(NodeName);
		//Set all of the class variables
		this.myAddress = getLocalAddress();
		this.serverAddress = getLocalAddress();
		this.configMessage = FilePath;
		//Set up the remaining variables using the config file data
		FileReader configFile = new FileReader(FilePath);
		BufferedReader configSettings = new BufferedReader(configFile);
		int myPort;
		//Set port numbers
		myPort = Integer.parseInt(configSettings.readLine());
		this.port = Integer.parseInt(configSettings.readLine());
		//Attempt to create sockets using port numbers
		this.addServerSocket(myPort, myAddress);
		configSettings.close();
	}

	/**
	 * This method reads a message from the Node's config file
	 * @return A list of strings from the config file, line by line
	 * @throws IOException Is thrown when the config file does not exist (will never happen if node was constructed using config file)
	 */
	public List<String> readMessage() throws IOException {
		List<String> message = new ArrayList<String>();
		FileReader configFile = new FileReader(this.configMessage);
		Scanner configMessage = new Scanner(configFile);
		//Skip past port specifications
		configMessage.nextLine();
		configMessage.nextLine();
		while(configMessage.hasNext()) {
			message.add(configMessage.nextLine());
		}
		configMessage.close();
		return message;
	}
	
	/**
	 * Returns the Port this node is listening on
	 * @return A port number
	 */
	public int getPort() {
		return this.port;
	}
	
	@Override
	public void run() {
		readSocket();
		this.addOutputSocket(port, serverAddress);
		//Send message within this node's config file
		try {
			List<String> configMessage = readMessage();
			for (String s : configMessage) {
				writeToSocket(s);
			}
		} catch (IOException e) {
			//Config file does not exist!
			//Should never happen since Node is constructed using the config file
			e.printStackTrace();
		}
		//Kill connection to server
		//TODO Remove this, all nodes must use interprocess communication to exit
		try {
			killServerConnection();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
