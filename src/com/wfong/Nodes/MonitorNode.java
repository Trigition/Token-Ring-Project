package com.wfong.nodes;

import java.net.InetAddress;

import com.wfong.token.STPLPFrame;

/**
 * This class implements Monitor Node behavior and is a subclass of the Node class.
 * @author William
 *
 */
public class MonitorNode extends Node implements Runnable{
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
	}
	
	/**
	 * This method monitors the Network for Garbled and Orphaned Frames. Lost tokens are handled
	 * by the enclosing method.
	 */
	private void MonitorNetwork() {
		STPLPFrame inputFrame;
		inputFrame = readSocket();
		//Check for Garbled Frame
		if (!isFrameHealthy(inputFrame)) {
			//Frame is Garbled
			for (long sysTime = System.currentTimeMillis(); System.currentTimeMillis() - sysTime < timeOut;) {
				inputFrame = readSocket(); //Drain the Ring for entire time out period
			}
			writeToSocket(STPLPFrame.generateToken());
		}
		//Check for Orphan Frame
		if (!inputFrame.isToken() && inputFrame.monitorBit()) {
			inputFrame = null; //'Drain' the frame
		} else if (!inputFrame.isToken()) {
			//This is the Frame's first encounter with Monitor
			//Set the monitor bit
			inputFrame.setMonitorBit();
		}
		return;
	}

	/**
	 * This method checks the validity of the Frame.
	 * @param frame The Frame to be checked.
	 * @return True if the Frame is healthy, false if it is garbled.
	 */
	private boolean isFrameHealthy(STPLPFrame frame) {
		//Check Frame Control Health
		if (frame.getFrameControl() > 3)
			return false; //Values above 3 on Frame Control are not accepted
		if (frame.getDestinationAddress() == frame.getSourceAddress())
			return false; //Nodes cannot send messages to themselves
		
		//Check if Data Length is correct
		int frameLength = frame.getBinaryData().length;
		int dataSize = frame.getDataSize();
		if (frameLength != dataSize + 6)
			return false;
		return true;
	}
	
	/**
	 * Returns the Port this node is listening to.
	 * @return The Port Number.
	 */
	public int getPort() {
		return this.port;
	}
	
	@Override
	public void run() {
		//Define Timeout Thread
		Thread timeOut = new Thread(new Runnable() {
			@Override
			public void run() {
				MonitorNetwork();
			}
			
		});
		long startTime = System.currentTimeMillis();
		long endTime = System.currentTimeMillis();
		while(true) {		
			timeOut.start();
			//While the Thread is alive, see if time elapsed is more than Time Out period.
			while(timeOut.isAlive()) {
				if ((endTime - startTime) > this.timeOut) {
					System.out.println("Monitor Node Timed Out. Re-issueing Token...");
					writeToSocket(STPLPFrame.generateToken());
					break;
				}
				endTime = System.nanoTime();
			}
		}
	}
	
}