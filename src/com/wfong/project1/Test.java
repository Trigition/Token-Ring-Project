package com.wfong.project1;

import java.io.IOException;
import java.lang.Thread;

/**
 * This class is for testing the Node network
 * @author William Fong
 *
 */
public class Test {

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		
		ServerNode server;
		RelayNode relay;
		SenderNode client;
		try {
			
			Thread RelayThread = new Thread(relay  = new RelayNode("NodeB", "confB.txt"));
			RelayThread.start();
			Thread ServerThread = new Thread(server = new ServerNode("NodeC", "confC.txt"));
			ServerThread.start();
			Thread ClientThread = new Thread(client = new SenderNode("NodeA", "confA.txt"));
			ClientThread.start();


		} catch (NumberFormatException e) {
			System.err.println("Error! Could not construct nodes, improper Config Files present");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Error! Could not find config files!");
			e.printStackTrace();
		}

	}

}
