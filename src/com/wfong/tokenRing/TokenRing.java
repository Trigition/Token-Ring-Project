package com.wfong.tokenRing;

import java.util.ArrayList;
import java.util.List;

import com.wfong.nodes.MonitorNode;
import com.wfong.nodes.RelayNode;

public class TokenRing {
	private List<RelayNode> Ring;
	
	public TokenRing() {
		this.Ring = new ArrayList<RelayNode>();
	}
	
	public TokenRing(List<RelayNode> ring) {
		this.Ring = ring;
	}
	
	public TokenRing(int numberOfNodes) {
		this.Ring = new ArrayList<RelayNode>();
		createRing(numberOfNodes);
	}
	
	public void runRing() {
		for (RelayNode node : Ring) {
			node.run();
		}
	}
	
	private int createRing (int numberOfNodes) {
		//TODO Change next line to be a Monitor Node
		MonitorNode monitor;
		monitor = new MonitorNode(0, 1000);
		for (int i = 1; i < numberOfNodes; i++) {
			this.Ring.add(new RelayNode("input-file-", i, 1000));
			//Establish connection to created node
			if (i == 1)
				monitor.addOutputSocket(this.Ring.get(1).getPort(), this.Ring.get(1).getLocalAddress());
			else
				this.Ring.get(i - 1).addOutputSocket(this.Ring.get(i).getPort(), this.Ring.get(i).getLocalAddress());
		}
		//Complete loop
		this.Ring.get(numberOfNodes - 1).addOutputSocket(monitor.getPort(), monitor.getLocalAddress());
		return 0;
	}
}
