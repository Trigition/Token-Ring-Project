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

	public MonitorNode(int NodeName, int timeOut) {
		super(NodeName);
		this.myAddress = getLocalAddress();
		this.port = this.addServerSocket(myAddress);
		this.timeOut = timeOut;
	}
	
	private void MonitorNetwork() {
		STPLPFrame inputFrame;
		inputFrame = readSocket();
		if (!isFrameHealthy(inputFrame)) {
			//Drain the Ring
		}
		if (!inputFrame.isToken() && inputFrame.monitorBit()) {
			inputFrame = null; //'Drain' the frame
		} else if (!inputFrame.isToken()) {
			inputFrame.setMonitorBit();
		}
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
		while(true) {
			MonitorNetwork();
		}
	}
	
}