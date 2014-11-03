package com.wfong.tokenRing;

import java.util.ArrayList;
import java.util.List;

import com.wfong.nodes.MonitorNode;
import com.wfong.nodes.RelayNode;

public class TokenRing {
	private List<RelayNode> Ring;
	private List<Thread> ringThreads;
	private MonitorNode monitor;
	
	public TokenRing() {
		this.Ring = new ArrayList<RelayNode>();
	}
	
	public TokenRing(List<RelayNode> ring) {
		this.Ring = ring;
	}
	
	public TokenRing(int numberOfNodes) {
		this.Ring = new ArrayList<RelayNode>();
		this.ringThreads = new ArrayList<Thread>();
		createRing(numberOfNodes);
	}
	
	public void runRing() {
		for (Thread thread : ringThreads) {
			System.out.println("Starting Node...");
			thread.start();
		}
		//monitor.run();
		//System.out.println("Started Monitor...");
	}
	
	private int createRing (int numberOfNodes) {
		//TODO Change next line to be a Monitor Node
		this.monitor = new MonitorNode(0, 1000000);
		this.ringThreads.add(new Thread(this.monitor));
		for (int i = 1; i <= numberOfNodes; i++) {
			this.Ring.add(new RelayNode("input-file-", i, 100));
			this.ringThreads.add(new Thread(this.Ring.get(i - 1)));
			//Establish connection to created node
			if (i == 1)
				this.monitor.addOutputSocket(this.Ring.get(0).getPort(), this.Ring.get(0).getLocalAddress());
			else
				this.Ring.get(i - 2).addOutputSocket(this.Ring.get(i - 1).getPort(), this.Ring.get(i - 1).getLocalAddress());
		}
		//Complete loop
		this.Ring.get(numberOfNodes - 1).addOutputSocket(this.monitor.getPort(), this.monitor.getLocalAddress());
		monitor.placeNetwork(Ring);
		return 0;
	}
}
