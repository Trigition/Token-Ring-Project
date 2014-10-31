package com.wfong.nodes;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
	private BufferedReader inputFile;
	private int THT;
	private List<STPLPFrame> frameBuffer;
	
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
	
	public RelayNode(String filePattern, int NodeName, int THT) {
		super(NodeName);
		this.myAddress = getLocalAddress();
		this.serverAddress = getLocalAddress();
		this.port = this.addServerSocket(myAddress);
		this.frameBuffer = new ArrayList<STPLPFrame>();
		this.THT = THT;
		Path path = Paths.get(filePattern + NodeName);
		try {
			this.inputFile = Files.newBufferedReader(path, StandardCharsets.US_ASCII);
		} catch (IOException e) {
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

	
	/**
	 * This method represents the Listen state of the Node.
	 * @return Returns 0 upon reception of a Token Frame, or 1 upon Kill Signal Reception
	 */
	public int Listen() {
		STPLPFrame inputFrame;
		while(true) {
			inputFrame = readSocket();
			//Check if Frame is Kill Signal
			if (inputFrame.getFrameControl() == 2) {
				writeToSocket(inputFrame); //Pass the Kill Signal to the next node
				return 1;
			}
			//Check if Frame is Token
			if(inputFrame.isToken()) {
				return 0; //Go to Transmit State
			}
			//If Frame was intended for this Node
			if (inputFrame.getDestinationAddress() == this.getNodeID()) {
				//Frame has reached its destination
				inputFrame.generateFrameStatus();
				//Determine if frame needs to be rejected or received
				if(inputFrame.getFrameStatus() == 0) {
					//Reject Frame
					writeToSocket(inputFrame);
					continue;
				} else if (inputFrame.getFrameStatus() == 1) {
					System.out.println("Node " + this.getNodeID() + ": Received from "
																	+ inputFrame.getSourceAddress() 
																	+ ": " + inputFrame.dataToString());
					writeToSocket(inputFrame); //Pass Frame to return back to Sender
				}
			}
			//Check to see if Frame was rejected
			if (inputFrame.getSourceAddress() == this.getNodeID()) {
				if (inputFrame.getFrameStatus() == 0) {
					//Frame was rejected
					this.frameBuffer.add(inputFrame);
					continue;
				}
				//Check to see if Frame was accepted
				if (inputFrame.getFrameStatus() == 1) {
					//Drain Buffer
					inputFrame = null;
					continue;
				}
				if (inputFrame.getFrameStatus() == 4) {
					//Kill Network Signal has been received
					writeToSocket(inputFrame);
					return 1;
				}
			}
		}
	}
	
	/**
	 * This method represents the transmission state of the Node. This is only active
	 * while the Node has a the token and has not gone beyond the total THT.
	 * @return Returns 0 when THT has been depleted, or 1 when there is no more data to transmit
	 */
	public int Transmit(){
		int currentTHT = 0;
		STPLPFrame currentFrame;
		String buffer;
		while (currentTHT < this.THT) {
			try {
				buffer = this.inputFile.readLine();
				currentFrame = new STPLPFrame(buffer, (byte) this.getNodeID());
				currentTHT += currentFrame.getDataSize();
				writeToSocket(currentFrame);
			} catch (IOException e) {
				//Either no data file or no data to transmit
				System.out.println("No data to transmit");
				return 1;
			}
		}
		return 0;
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
			if (Listen() == 1) {
				//Kill Signal has been received
				this.closeNode();
				return;
			}
			//Token has been received
			Transmit();
		}
	}
}
