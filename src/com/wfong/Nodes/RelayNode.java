package com.wfong.nodes;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.wfong.token.STPLPFrame;

/**
 * This class relays data between client and server nodes (Acting as both)
 * @author William Fong
 *
 */
public class RelayNode extends Node implements Runnable {
	private InetAddress myAddress;
	private InetAddress serverAddress;
	private int port;
	private BufferedReader inputFile;
	private PrintWriter outputFile;
	private int THT;
	private List<STPLPFrame> frameBuffer;
	private boolean hasSentComplete;
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
		this.hasSentComplete = false;
		try {
			outputFile = new PrintWriter("output-file-" + this.getNodeID(), "UTF-8");
		} catch (FileNotFoundException e1) {
			System.err.println("Could not open output file...");
		} catch (UnsupportedEncodingException e1) {
			System.err.println("Coult not use specified encoding scheme...");
		}
		this.THT = THT;
		Path path = Paths.get(filePattern + NodeName);
		try {
			this.inputFile = Files.newBufferedReader(path, StandardCharsets.US_ASCII);
		} catch (IOException e) {
			//File does not exist
			this.inputFile = null;
		}
		//System.out.println("Node " + this.getNodeID() + " Listening on Port: " + this.port);
	}
	
	/**
	 * This method represents the Listen state of the Node.
	 * @return Returns 0 upon reception of a Token Frame, or 1 upon Kill Signal Reception
	 */
	public int Listen() {
		//System.out.println("Node " + this.getNodeID() + " is entering Listen State!");
		STPLPFrame inputFrame;
		while(true) {
			//System.out.println("Node " + this.getNodeID() + " Waiting for data input.");
			inputFrame = readSocket();
			//System.out.println("Node " + this.getNodeID() + " Received data...");

			//Check to see if Frame is Kill Signal
			if (inputFrame.getFrameStatus() == 4) {
				//Kill Network Signal has been received
				writeToSocket(inputFrame); //Pass Kill Signal
				return 1;
			}
			
			//Check to see if Frame is Completion Signal
			if (inputFrame.getFrameStatus() == 5) {
				writeToSocket(inputFrame);
				continue;
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
				if(inputFrame.getFrameStatus() == 3) {
					//Reject Frame
					writeToSocket(inputFrame);
					continue;
				} else if (inputFrame.getFrameStatus() == 2) {
					this.outputFile.println(inputFrame.getSourceAddress() + 
							"," + inputFrame.getDestinationAddress() + 
							"," + inputFrame.getDataSize() + 
							"," + inputFrame.dataToString());
					writeToSocket(inputFrame); //Pass Frame to return back to Sender
				}
			}
			//Check to see if Frame was rejected
			if (inputFrame.getSourceAddress() == this.getNodeID()) {
				if (inputFrame.getFrameStatus() == 3) {
					//Frame was rejected
					inputFrame.setFrameStatus((byte) 0);
					inputFrame.zeroMonitorBit();
					this.frameBuffer.add(inputFrame);
					continue;
				}
				//Check to see if Frame was accepted
				if (inputFrame.getFrameStatus() == 2) {
					//Drain Buffer
					inputFrame = null;
					continue;
				}

			}
			writeToSocket(inputFrame);
		}
	}
	
	/**
	 * This method represents the transmission state of the Node. This is only active
	 * while the Node has a the token and has not gone beyond the total THT.
	 * @return Returns 0 when THT has been depleted, or 1 when there is no more data to transmit
	 */
	public int Transmit(){
		//System.out.println("Node " + this.getNodeID() + " is entering Transmit State!");
		int currentTHT = 0;
		STPLPFrame currentFrame;
		String buffer;
		while (currentTHT < this.THT) {
			try {
				if (this.frameBuffer.isEmpty()) {
					//No Frames in buffer, can continue transmission
					buffer = this.inputFile.readLine();
					if (buffer == null) {
						if (hasSentComplete == false) {
							writeToSocket(STPLPFrame.generateCompletedSig((byte) (this.getNodeID() & 0xff)));
							hasSentComplete = true;
						}
						writeToSocket(STPLPFrame.generateToken());
						return 1;
					}
					currentFrame = new STPLPFrame(buffer, (byte) this.getNodeID());
					currentTHT += currentFrame.getDataSize();
					//System.out.println("Node " + this.getNodeID() + " is transmitting " + currentFrame.toString());
					writeToSocket(currentFrame);
				} else {
					//Frames in buffer for retransmission
					//System.out.println("Node " + this.getNodeID() + ": Retransmitting Rejected Frame...");
					currentFrame = this.frameBuffer.remove(0);
					//System.out.println(this.frameBuffer.size() + " remain in retransmission buffer...");
					currentTHT += currentFrame.getDataSize();
					writeToSocket(currentFrame);
				}
			} catch (IOException e) {
				//Either no data file or no data to transmit
				System.out.println("No data to transmit");
				writeToSocket(STPLPFrame.generateCompletedSig((byte) this.getNodeID()));
				writeToSocket(STPLPFrame.generateToken()); //Pass the Token
				return 1;
			}
		}
		//System.out.println("Node: " + this.getNodeID() + " Has surpassed THT. Passing token..");
		writeToSocket(STPLPFrame.generateToken()); //Pass the Token
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
		System.out.println("Initializing Node " + this.getNodeID());
		this.acceptClient();
		if (this.getNodeID() == 1) {
			System.out.println("\tGenerating token");
			writeToSocket(STPLPFrame.generateToken());
		}
		while(true) {
			if (Listen() == 1) {
				//Kill Signal has been received
				System.out.println("Node: " + this.getNodeID() + " has received Kill Order 66");
				this.closeNode();
				this.outputFile.close();
				return;
			}
			Transmit();
		}
	}
}
