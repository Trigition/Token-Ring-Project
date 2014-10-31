package com.wfong.nodes;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.wfong.token.STPLPFrame;

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
	private FileReader inputFile;
	
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
	
	public RelayNode(String filePattern, int NodeName) {
		super(NodeName);
		this.myAddress = getLocalAddress();
		this.serverAddress = getLocalAddress();
		this.port = this.addServerSocket(myAddress);
		try {
			this.inputFile = new FileReader(filePattern + NodeName);
		} catch (FileNotFoundException e) {
			//File does not exist
			this.inputFile = null;
		}
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

	public int Listen() {
		STPLPFrame inputFrame;
		inputFrame = readSocket();
		if(inputFrame.isToken()) {
			return 0; //Go to Transmit State
		}
		if (inputFrame.getDestinationAddress() == this.getNodeID()) {
			//Frame has reached its destination
			
		}
		return 1;
	}
	
	public void Transmit() {
		
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
		while(true) {
			//Listen State
			//Transmit State
		}
	}
}
