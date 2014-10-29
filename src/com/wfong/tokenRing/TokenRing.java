package com.wfong.tokenRing;

import java.util.ArrayList;
import java.util.List;

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
	
	private int createRing (int numberOfNodes) {
		//TODO Change next line to be a Monitor Node
		this.Ring.add(new RelayNode(0));
		for (int i = 1; i < numberOfNodes; i++) {
			this.Ring.add(new RelayNode(i));
			//Establish connection to created node
			this.Ring.get(i - 1).addOutputSocket(this.Ring.get(i).getPort(), this.Ring.get(i).getLocalAddress());
		}
		//Complete loop
		this.Ring.get(numberOfNodes - 1).addOutputSocket(this.Ring.get(0).getPort(), this.Ring.get(0).getLocalAddress());
		return 0;
	}
}
