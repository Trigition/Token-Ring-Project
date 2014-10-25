package com.wfong.nodes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * This class is for sending data to a server Node.
 * @author William Fong
 *
 */
public class SenderNode extends Node implements Runnable {
	private InetAddress address;
	private String configMessage;
	private int port;
	/**
	 * This constructor allows for manual specification of the Node's name and port number
	 * @param NodeName The Node's Name
	 * @param port The Node's Port Number
	 */
	public SenderNode(String NodeName, int port) {
		super(NodeName);
		address = getLocalAddress();
		addOutputSocket(port, address);
	}
	
	/**
	 * This constructor creates a server node using a config file
	 * @param NodeName The Node's Name
	 * @param FilePath The file path to the config file
	 * @throws NumberFormatException Is thrown when the config file did not specify the port number correctly
	 * @throws IOException Is thrown when the config file does not exit (Will not happen if the Node was constructed using the config file)
	 */
	public SenderNode(String NodeName, String FilePath) throws NumberFormatException, IOException {
		super(NodeName);
		address = getLocalAddress();
		configMessage = FilePath;
		FileReader configFile = new FileReader(FilePath);
		BufferedReader configSettings = new BufferedReader(configFile);
		this.port = Integer.parseInt(configSettings.readLine());
		//this.addOutputSocket(port, address);
		configSettings.close();
	}
	
	/**
	 * This method reads the message contained within the Node's Config File
	 * @return A list of strings from the config file (Line by line)
	 * @throws IOException Is thrown when the config file for the Node does not exist (Will not happen if the Node was constructed using the config file)
	 */
	public List<String> readMessage() throws IOException {
		List<String> message = new ArrayList<String>();
		FileReader configFile = new FileReader(this.configMessage);
		Scanner configMessage = new Scanner(configFile);
		//Skip past port specification
		configMessage.nextLine();
		while(configMessage.hasNext()) {
			message.add(configMessage.nextLine());
		}
		configMessage.close();
		return message;
	}
	
	@Override
	public void run() {
		//Read from config file
		this.addOutputSocket(port, address);
		try {
			List<String> message = readMessage();
			for (String s : message) {
				writeToSocket(s);
			}
		} catch (IOException e1) {
			System.err.println("Could not find message!");
			return;
		}
		try {
			killServerConnection();
		} catch (IOException e) {
			System.err.println("Error in closing socket");
			e.printStackTrace();
		}
	}	
}
