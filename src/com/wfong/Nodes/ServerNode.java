package com.wfong.Nodes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;

import com.wfong.project1.Node;

/**
 * This class reads data from incoming client connections and prints it out to System out
 * @author William Fong
 *
 */
public class ServerNode extends Node implements Runnable {
	private InetAddress serverAddress;
	
	/**
	 * This constructor allows manual specification of the Node Name and Port for the Server Socket
	 * @param nodeName The Node's Name
	 * @param port The Server Port number for connection requests
	 */
	public ServerNode(String nodeName, int port) {
		super(nodeName);
		serverAddress = getLocalAddress();
		this.addServerSocket(port, serverAddress);
		
	}
	
	/**
	 * This constructor creates a server node using a config file
	 * @param nodeName The Node's Name
	 * @param FilePath The file path to the config file
	 * @throws NumberFormatException Is thrown when the config file's port number section is incorrectly formatted
	 * @throws IOException Is thrown when the config file does not exist
	 */
	public ServerNode(String nodeName, String FilePath) throws NumberFormatException, IOException {
		super(nodeName);
		serverAddress = getLocalAddress();
		FileReader configFile = new FileReader(FilePath);
		BufferedReader configSettings = new BufferedReader(configFile);
		int port = Integer.parseInt(configSettings.readLine());
		this.addServerSocket(port, serverAddress);
		configSettings.close();
	}

	@Override
	public void run() {
		readSocket();
	}
}
