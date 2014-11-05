package com.wfong.tokenRing;


/**
 * This class is for testing the Node network
 * @author William Fong
 *
 */
public class Test {
	
	public static void main(String[] args) {
		if (args.length == 0 || args.length > 3 || Integer.valueOf(args[0]) < 2 || Integer.valueOf(args[0]) > 254) {
			System.out.println("Usage: ");
			System.out.println("./wfong_p2.jar [number of Nodes to execute (2-254)] [Optional - THT] [Optional - Timeout Multiplier]");
			return;
		}
		TokenRing testRing = null;
		
		if (args.length == 1)
			testRing = new TokenRing(Integer.valueOf(args[0]));
		else if (args.length == 2)
			testRing = new TokenRing(Integer.valueOf(args[0]), Integer.valueOf(args[1]));
		else if (args.length == 3)
			testRing = new TokenRing(Integer.valueOf(args[0]), Integer.valueOf(args[1]), Integer.valueOf(args[2]));		
		testRing.runRing();	
	}

}
