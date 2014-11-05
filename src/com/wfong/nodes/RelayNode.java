package com.wfong.nodes;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
	private List<STPLPFrame> waitingFrames;
	private boolean hasSentComplete;
	/**
	 * This constructor allows for specification of the Node's Name as well as the receiving and sending port numbers
	 * @param NodeName The Node's Name
	 * @param myPort The Nodes's Server Socket Port number
	 * @param serverPort The Node's output Port number
	 */
	public RelayNode(int NodeName, int myPort, int serverPort, int timeOutPeriod) {
		super(NodeName, timeOutPeriod);
		this.myAddress = getLocalAddress();
		this.serverAddress = getLocalAddress();
		this.addServerSocket(myPort, myAddress);
		this.addOutputSocket(serverPort, serverAddress);
	}
	
	/**
	 * This creates a node with a specified Node ID, THT, timeout period and an input file to read<br>
	 * frames from.
	 * @param filePattern The file pattern associated with the node.<br>
	 * It will access "input-file-n" where n is the Node ID.
	 * @param NodeName The Node ID number to be associated with the node.
	 * @param THT The THT for the Node, this determines when the node<br>
	 * should end transmission and enter the listen state.
	 * @param timeOutPeriod The time out period for the node (is not used)<br>
	 */
	public RelayNode(String filePattern, int NodeName, int THT, int timeOutPeriod) {
		super(NodeName, timeOutPeriod);
		this.myAddress = getLocalAddress();
		this.serverAddress = getLocalAddress();
		this.port = this.addServerSocket(myAddress);
		this.frameBuffer = new ArrayList<STPLPFrame>();
		this.waitingFrames = new ArrayList<STPLPFrame> ();
		this.hasSentComplete = false;
		//Attempt to open output file
		try {
			outputFile = new PrintWriter("output-file-" + this.getNodeID(), "UTF-8");
		} catch (FileNotFoundException e1) {
			System.err.println("Could not open output file...");
		} catch (UnsupportedEncodingException e1) {
			System.err.println("Coult not use specified encoding scheme...");
		}
		this.THT = THT;
		Path path = Paths.get(filePattern + NodeName);
		//Attempt to open input file
		try {
			this.inputFile = Files.newBufferedReader(path, StandardCharsets.US_ASCII);
		} catch (IOException e) {
			//File does not exist
			this.inputFile = null;
		}
	}
	
	/**
	 * This method represents the Listen state of the Node.
	 * @return Returns 0 upon reception of a Token Frame, or 1 upon Kill Signal Reception
	 */
	public int Listen() {
		STPLPFrame inputFrame;
		while(true) {
			//Read from input socket a new frame
			try {
				inputFrame = readSocket();
			} catch (SocketTimeoutException e) {
				System.out.println("Node " + this.getNodeID() + " timed out...");
				continue;
			}
			//Check to see if the incoming frame is empty
			if (inputFrame == null) {
				//Bad frame
				continue;
			}
			//Check to see if frame is healthy
			//This should pass any garbled frames however corruption of
			//data size caused this node to be unable to differentiate
			//when a new frame started
			if (!MonitorNode.isFrameHealthy(inputFrame)) {
				writeToSocket(inputFrame);
				continue;
			}
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
			
			//If Frame was intended for this Node
			if (inputFrame.getDestinationAddress() == this.getNodeID() && inputFrame.getFrameStatus() == 0) {
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
			this.waitingFrames.remove(inputFrame); //Frame has not been lost in the network
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
					Random rand = new Random();
						if (rand.nextInt(100) < 2) {
							writeToSocket(inputFrame); //Create Orphan Frame
						}
					continue;
				}

			}		
			//Check if Frame is Token
			if(inputFrame.isToken()) {
				return 0; //Go to Transmit State
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
		int currentTHT = 0;
		STPLPFrame currentFrame;
		String buffer = null;
		//While the THT has not been surpassed
		while (currentTHT < this.THT) {
			try {
				//Retransmit any frames that have failed to reach their destinations
				if (!this.frameBuffer.isEmpty()){
					//Frames in buffer for retransmission
					currentFrame = this.frameBuffer.remove(0);
					currentTHT += currentFrame.getDataSize();
					writeToSocket(currentFrame.garbleFrame());
					this.waitingFrames.add(currentFrame);
					continue;
				}
				//If no frames need retransmission, resume reading the input file
				if (this.frameBuffer.isEmpty()) {
					buffer = this.inputFile.readLine();
					//Check see if at EOF
					if (buffer == null) {
						//Node has successfully transmitted all of its OWN data
						if (this.hasSentComplete == false && this.waitingFrames.isEmpty()) {
							//Notify monitor of completion
							writeToSocket(STPLPFrame.generateCompletedSig((byte) this.getNodeID()));
							this.hasSentComplete = true;
						}
						//No longer needs to transmit
						writeToSocket(STPLPFrame.generateToken());
						return 1;
					}
					//Transmit the next line in input file
					currentFrame = new STPLPFrame(buffer, (byte) this.getNodeID());
					currentTHT += currentFrame.getDataSize();
					writeToSocket(currentFrame.garbleFrame());
					this.waitingFrames.add(currentFrame);
					continue;
				}
			} catch (IOException e) {
				//Either no data file or no data to transmit
				System.out.println("No data to transmit");
				writeToSocket(STPLPFrame.generateCompletedSig((byte) this.getNodeID()));
				writeToSocket(STPLPFrame.generateToken()); //Pass the Token
				return 1;
			}
		}
		Random rand = new Random();
		//95% Chance to successfully transmit the token
		if (rand.nextInt(100) < 95)
			writeToSocket(STPLPFrame.generateToken()); //Pass the Token
//		else
//			System.out.println("Node " + this.getNodeID() + " lost the token!");
		return 0;
	}
	
	/**
	 * Returns the Port this node is listening on
	 * @return A port number
	 */
	public int getPort() {
		return this.port;
	}
	
	/**
	 * This functions is implementing the required Runnable method
	 */
	@Override
	public void run() {
		this.acceptClient();
		if (this.getNodeID() == 1) {
			//Node 1 generates the token and passes it to the neighboring node
			writeToSocket(STPLPFrame.generateToken());
		}
		while(true) {
			if (Listen() == 1) {
				//Kill Signal has been received
				//System.out.println("Node: " + this.getNodeID() + " has received Kill Order 66");
				this.closeNode();
				this.outputFile.close();
				return;
			}
			//Any frames that have not been ACK nor NAK are added for retransmission
			this.frameBuffer.addAll(waitingFrames);
			this.waitingFrames.clear();
			Transmit();
		}
	}
}
