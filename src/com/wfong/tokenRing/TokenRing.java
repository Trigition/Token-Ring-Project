package com.wfong.tokenRing;

import java.util.ArrayList;
import java.util.List;

import com.wfong.nodes.MonitorNode;
import com.wfong.nodes.RelayNode;

/**
 * This class is for creating and holding the network
 * @author william
 *
 */
public class TokenRing {
	private List<RelayNode> Ring;
	private List<Thread> ringThreads;
	private MonitorNode monitor;
	private int globalTHT;
	private int globalTimeOut;
	private int numberNodes; //Used for THT experiment
	
	/**
	 * Default constructor
	 */
	public TokenRing() {
		this.Ring = new ArrayList<RelayNode>();
	}
	
	/**
	 * This constructor creates a ring with a list of Nodes
	 * @param ring The list of nodes
	 */
	public TokenRing(List<RelayNode> ring) {
		this.Ring = ring;
	}
	
	/**
	 * This constructor creates a ring with the number of nodes specified
	 * @param numberOfNodes The number of nodes the network will have
	 */
	public TokenRing(int numberOfNodes) {
		this.Ring = new ArrayList<RelayNode>();
		this.ringThreads = new ArrayList<Thread>();
		this.globalTHT = 150;
		this.globalTimeOut = 10;
		this.numberNodes = numberOfNodes;
		createRing(numberOfNodes);
	}
	
	/**
	 * This constructor creates a ring with the number of nodes specified and specifies the THT allowed
	 * @param numberOfNodes The number of nodes the network will have.
	 * @param globalTHT The THT for the network.
	 */
	public TokenRing(int numberOfNodes, int globalTHT) {
		this.Ring = new ArrayList<RelayNode>();
		this.ringThreads = new ArrayList<Thread>();
		this.globalTHT = globalTHT;
		this.globalTimeOut = 10;
		this.numberNodes = numberOfNodes;
		createRing(numberOfNodes);
	}
	
	/**
	 * This constructor creates a ring with the number of the nodes specified and specifies the THT allowed and timeout period
	 * @param numberOfNodes The number of nodes the network will have.
	 * @param globalTHT The THT for the network.
	 * @param globalTimeOut The time out multiplier for the network.
	 */
	public TokenRing(int numberOfNodes, int globalTHT, int globalTimeOut) {
		this.Ring = new ArrayList<RelayNode>();
		this.ringThreads = new ArrayList<Thread>();
		this.globalTHT = globalTHT;
		this.globalTimeOut = globalTimeOut;
		this.numberNodes = numberOfNodes;
		createRing(numberOfNodes);
	}
	
	/**
	 * This method runs the ring by starting each node sequentially
	 */
	public void runRing() {
		//System.out.print(this.numberNodes + " " + this.globalTHT + " " + this.globalTimeOut + " ");
		for (Thread thread : ringThreads) {
			thread.start();
		}
	}
	
	/**
	 * This method creates the ring by creating each node and monitor node individually and connecting them via sockets.
	 * @param numberOfNodes The number of nodes for the network.
	 * @return Returns 0
	 */
	private int createRing (int numberOfNodes) {
		this.monitor = new MonitorNode(0, (numberOfNodes * this.globalTimeOut));
		this.ringThreads.add(new Thread(this.monitor));
		for (int i = 1; i <= numberOfNodes; i++) {
			//Create node with input file pattern, THT, and timeout period
			this.Ring.add(new RelayNode("input-file-", i, this.globalTHT, (numberOfNodes * this.globalTimeOut)));
			//Connect put the node in it's own thread
			this.ringThreads.add(new Thread(this.Ring.get(i - 1)));
			//Establish connection to 'previous node'
			if (i == 1)
				this.monitor.addOutputSocket(this.Ring.get(0).getPort(), this.Ring.get(0).getLocalAddress());
			else
				this.Ring.get(i - 2).addOutputSocket(this.Ring.get(i - 1).getPort(), this.Ring.get(i - 1).getLocalAddress());
		}
		//Complete loop
		this.Ring.get(numberOfNodes - 1).addOutputSocket(this.monitor.getPort(), this.monitor.getLocalAddress());
		//Inform the Monitor of who is in the Network
		monitor.placeNetwork(Ring);
		return 0;
	}
}
