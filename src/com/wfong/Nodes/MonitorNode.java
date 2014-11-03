package com.wfong.nodes;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wfong.token.STPLPFrame;

/**
 * This class implements Monitor Node behavior and is a subclass of the Node class.
 * @author William
 *
 */
public class MonitorNode extends Node implements Runnable{
	private Map<Integer, Integer> myNetwork;
	private InetAddress myAddress;
	private int port;
	private int timeOut;
	
	public MonitorNode() {
		super();
	}

	/**
	 * This constructor creates a Monitor node with the specified ID (usually 0) and a specified time out period (in milliseconds).
	 * @param NodeName The Node ID.
	 * @param timeOut The time out period in milliseconds.
	 */
	public MonitorNode(int NodeName, int timeOut) {
		super(NodeName);
		this.myAddress = getLocalAddress();
		this.port = this.addServerSocket(myAddress);
		this.timeOut = timeOut;
		this.myNetwork = new HashMap<Integer, Integer>();
		//System.out.println("Monitor Node Listening on Port " + this.port);
	}
	
	/**
	 * Places each node into myNetwork. This is to keep track of whom has completed their transfers.
	 * @param myNetwork A list of all the Nodes in the network.
	 */
	public void placeNetwork(List<RelayNode> myNetwork) {
		for (RelayNode node : myNetwork) {
			this.myNetwork.put(node.getNodeID(), 0);
		}
	}
	
	/**
	 * This method monitors the Network for Garbled and Orphaned Frames. Lost tokens are handled
	 * by the enclosing method.
	 */
	private void MonitorNetwork() {
		STPLPFrame inputFrame;
		while(true) {
			inputFrame = readSocket();
			//System.out.println("MONITOR NODE: Received Frame:\n" + inputFrame.toString());
			//Check for Garbled Frame
			if (!isFrameHealthy(inputFrame)) {
				//Frame is Garbled
				System.out.println("MONITOR NODE: Found garbled frame! :(");
				System.out.println("MONITOR NODE: Draining Ring.......");
				//inputFrame = readSocket(); //Drain the Ring for entire time out period
				try {
					this.getClientSocket().getOutputStream().flush();
				} catch (IOException e) {
					System.out.println("CRITICAL ERROR: Monitor node is not listening on for any client");
				}
				System.out.println("MONITOR NODE: Passing new token...");
				writeToSocket(STPLPFrame.generateToken());
				continue;
			}
			//Check for Orphan Frame
			if (!inputFrame.isToken() && inputFrame.monitorBit()) {
				System.out.println("MONITOR NODE: Found Orphan Frame");
				inputFrame = null; //'Drain' the frame
				continue;
			} else if (!inputFrame.isToken()) {
				//This is the Frame's first encounter with Monitor
				//Set the monitor bit
				inputFrame.setMonitorBit();
			}
			//Check for Transmission Completed Signals
			if (inputFrame.getFrameStatus() == 5) {
				//The frame's source has completed all transmission
				System.out.println("MONITOR NODE: Received Transmission completion signal from Node " + (inputFrame.getSourceAddress() & 0xff));
				this.myNetwork.put(inputFrame.getSourceAddress() & 0xff, 1);
				if (areAllNodesDone()) {
					//Send out the kill signal
					System.out.println("KILLING NETWORK");
					writeToSocket(STPLPFrame.generateKillSig());
				}
				continue;
			}
			if (inputFrame.getFrameStatus() == 4) {
				if (!areAllNodesDone()) {
					System.err.println("CRITICAL ERROR: Kill Signal Received: Not all Nodes Have Completed!");
				}
				System.out.println("MONITOR NODE: Exiting...");
				this.closeNode();
				return;
			}
			//System.out.println("MONITOR NODE: Passing frame");
			writeToSocket(inputFrame);
		}
		//writeToSocket(inputFrame); //Pass the Frame along
	}
	
	/**
	 * This method checks the validity of the Frame.
	 * @param frame The Frame to be checked.
	 * @return True if the Frame is healthy, false if it is garbled.
	 */
	private boolean isFrameHealthy(STPLPFrame frame) {
		//Check Frame Control Health
		if (frame.getFrameControl() > 5) {
			System.out.println("FRAME Error: Frame Control out of bounds: " + frame.getFrameControl());
			return false; //Values above 5 on Frame Control are not accepted
		}
		//Check if Data Length is correct
		int frameLength = frame.getFrame().length;
		int dataSize = frame.getDataSize();
		if (frameLength != dataSize + 6) {
			System.out.println("FRAME Error: Incorrect Data Size");
			return false;
		}
		return true;
	}
	
	/**
	 * Returns the Port this node is listening to.
	 * @return The Port Number.
	 */
	public int getPort() {
		return this.port;
	}
	
	/**
	 * This method determines if all nodes have finished their transmissions
	 * @return True if the network has finished all transmissions
	 */
	private boolean areAllNodesDone() {
		for (int nodeID : this.myNetwork.keySet()) {
			//See if any nodes have not finished transmissions
			if (this.myNetwork.get(nodeID) == 0) {
				//System.out.println("Node " + nodeID + " has not completed signal transmission");
				return false;
			}
		}
		return true; //All nodes have finished
	}
	
	@Override
	public void run() {
		System.out.println("Initialized Monitor Node");
		this.acceptClient();
		MonitorNetwork();
	}
	
}