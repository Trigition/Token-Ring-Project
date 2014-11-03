package com.wfong.token;

import java.util.Random;

public class STPLPFrame {
	//Byte Array for holding the Frame of n-Size.
	private byte[] frameValue;

	public STPLPFrame(byte[] frameValue) {
		this.frameValue = frameValue;
	}
	
	/**
	 * This is the main constructor for the STPLP class, it constructs a frame using
	 * a formatted string from an input file.
	 * @param frameString The string containing formatted data for constructing a STPLP Frame.
	 * @param sourceAddress The source address.
	 */
	public STPLPFrame(String frameString, byte sourceAddress) {
		byte accessControl;
		byte frameControl;
		byte destinationAddress;
		byte dataSize;
		byte[] data;
		byte frameStatus;
		//Begin extraction of data from file string
		//System.out.println("Converting String: " + frameString);
		String[] strTok = frameString.split(",");
		destinationAddress = (byte) (Integer.parseInt(strTok[0]) & 0xff);
		//System.out.println("\tDestination Address: " + (destinationAddress & 0xff));
		dataSize = (byte) (Integer.parseInt(strTok[1]) & 0xff);
		data = strTok[2].getBytes();
		//System.out.println("\tData Size: " + (dataSize & 0xff));
		//Set other data fields
		accessControl = 0; //TODO Implement various extra credit schemes
		frameControl = 1; //Frame is NOT a token
		frameStatus = 0; //Frame is newly constructed
		//Construct new Frame Value
		//Any Logical AND with 0xff is to Compensate for Java's naughty signed bit habit.
		this.frameValue = new byte[(dataSize & 0xff) + 6];
		this.frameValue[0] = accessControl;
		this.frameValue[1] = frameControl;
		this.frameValue[2] = destinationAddress;
		this.frameValue[3] = sourceAddress;
		this.frameValue[4] = dataSize;
		//Copy Data
		for (int i = 0; i < (dataSize & 0xff); i++) {
			this.frameValue[i + 5] = data[i];
		}
		this.frameValue[(dataSize & 0xff) + 5] = frameStatus;
	}
	
	/**
	 * Generates a Token.
	 * @return A token.
	 */
	public static STPLPFrame generateToken() {
		byte[] frameValue = new byte[6];
		frameValue[0] = 0x8; //Set token bit
		frameValue[1] = 0x0;
		frameValue[2] = 0x0;
		frameValue[3] = 0x0;
		frameValue[4] = 0x0;
		frameValue[5] = 0x0;
		return new STPLPFrame(frameValue);
	}
	
	/**
	 * Generates a Completion Signal.
	 * @param sourceAddress The source of the signal.
	 * @return A new STPLP Frame.
	 */
	public static STPLPFrame generateCompletedSig(byte sourceAddress) {
		byte[] frameValue = new byte[6];
		frameValue[0] = 0x0;
		frameValue[1] = 0x0;
		frameValue[2] = 0x0;
		frameValue[3] = sourceAddress;
		frameValue[4] = 0x0;
		frameValue[5] = 0x5; //Generate Completed Signal
		return new STPLPFrame(frameValue);
	}
	
	/**
	 * Generates a Kill Signal.
	 * @return A kill signal for the network.
	 */
	public static STPLPFrame generateKillSig() {
		byte[] frameValue = new byte[6];
		frameValue[0] = 0x0;
		frameValue[1] = 0x0;
		frameValue[2] = 0x0;
		frameValue[3] = 0x0;
		frameValue[4] = 0x0;
		frameValue[5] = 0x4; //Generate kill signal
		return new STPLPFrame(frameValue);
	}
	
	/**
	 * Returns the whole frame in a char byte array.
	 * @return The Frame.
	 */
	public byte[] getFrame () {
		return this.frameValue;
	}
	
	/**
	 * This method creates a string containing the information within the frame.
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("\tSource Address:........" + this.getSourceAddress() + "\n");
		builder.append("\tDestination Address:..." + this.getDestinationAddress() + "\n");
		builder.append("\tData Size:............." + this.getDataSize() + "\n");
		//builder.append("\tData:.................." + this.dataToString());
		return builder.toString();
	}
	
	public void printHeader() {
		System.out.println("Byte[0]: " + (this.frameValue[0] & 0xff) +
						   ", Byte[1]: " + (this.frameValue[1] & 0xff) +
						   ", Byte[2]: " + (this.frameValue[2] & 0xff) +
						   ", Byte[3]: " + (this.frameValue[0] & 0xff) +
						   ", Byte[4]: " + (this.frameValue[0] & 0xff));
	}
	
	//Access Control Methods
	/**
	 * Returns the frame priority.
	 * TODO Implement Priority in Node Network.
	 * @return The Frame priority.
	 */
	public int getFramePriority() {
		byte bitMask = 0x7;
		//No need to bit shift
		return frameValue[0] & bitMask;
	}

	/**
	 * This method looks at the token bit and determines if the frame passed is a token.
	 * @return True, if the frame is a token.
	 */
	public boolean isToken() {
		//Checks to see if any of the Token specifiers are true
		if (getFrameControl() == 0 || tokenBit())
			return true;
		return false;
	}
	
	/**
	 * This method checks the token bit.
	 * @return True if the token bit is flipped; false is otherwise.
	 */
	private boolean tokenBit() {
		byte bitMask = 0x8;
		byte tmp = (byte) (frameValue[0] & bitMask);
		//If token bit is flipped, value of tmp will be 8
		if (tmp == 0x8)
			return true;
		return false;
	}
	
	/**
	 * This method determines if the monitor bit is flipped or not.
	 * @return True, if the monitor bit has been flipped.
	 */
	public boolean monitorBit() {
		byte bitMask = 0x10;
		byte tmp = (byte) (frameValue[0] & bitMask);
		//If monitor bit is flipped, value will be 16, 0 if otherwise
		if (tmp == 0x10)
			return true;
		return false;
	}
	
	/**
	 * Sets the Monitor Bit to be 1
	 */
	public void setMonitorBit() {
		this.frameValue[0] = (byte) (this.frameValue[0] | 0x10);
	}
	
	/**
	 * Sets the Monitor Bit to be 0
	 */
	public void zeroMonitorBit() {
		this.frameValue[0] = (byte) (this.frameValue[0] & 0x0);
	}
	
	/**
	 * This method returns the Reservation Level of the Frame.
	 * TODO Implement for Extra Credit.
	 * @return
	 */
	public int getReservationLevel() {
		return 0;
	}
	
	/**
	 * Returns status of the Frame Control Byte.
	 * @return Returns 0 if Frame is a Token<br>
	 * Returns 1 if Frame is not a Token<br>
	 * Returns 2 if Frame is a Kill Signal
	 */
	public int getFrameControl() {
		return (this.frameValue[1] & 0xff);
	}
	
	/**
	 * This method returns the Destination Address Byte of the Frame.
	 * @return The Destination Address Byte.
	 */
	public int getDestinationAddress() {
		return (this.frameValue[2] & 0xff);
	}
	
	/**
	 * This method returns the Source Address Byte of the Frame.
	 * @return The Source Address Byte.
	 */
	public int getSourceAddress() {
		return (this.frameValue[3] & 0xff);
	}
	
	/**
	 * This method returns the Data Size of the Frame.
	 * @return The Data Size (in Bytes)
	 */
	public int getDataSize() {
		return (this.frameValue[4] & 0xff);
	}
	
	/**
	 * Returns Binary Data of length of the specified Data Size.
	 * @return A byte array containing Data.
	 */
	public byte[] getBinaryData() {
		byte[] data = new byte[getDataSize()];
		for (int i = 0; i < getDataSize(); i++) {
			data[i] = this.frameValue[i + 5];
		}
		return data;
	}
	
	/**
	 * This method returns a string representation of the Data within the Frame.
	 * @return A String representation of the Frame Data
	 */
	public String dataToString() {
		return new String(getBinaryData());
	}
	
	/**
	 * <p>This method sets the Frame Status byte.</p>
	 * <p>There's a 20% chance the Frame Status will be set so that the Frame will be <i>rejected</i>.
	 * The other remaining 80% will result in the Frame Status being set so the Frame is <i>accepted</i>.</p>
	 */
	public void generateFrameStatus() {
		int sizeOfFrame = this.frameValue.length;
		Random i = new Random();
		if(i.nextInt(100) < 90) {
			//Frame Rejected
			this.frameValue[sizeOfFrame - 1] = 0x3;
			return;
		}
		this.frameValue[sizeOfFrame - 1] = 0x2;
	}
	
	/**
	 * Sets the Frame Status to val.
	 * @param val
	 */
	public void setFrameStatus(byte val) {
		int sizeOfFrame = this.frameValue.length;
		this.frameValue[sizeOfFrame - 1] = val;
	}
	
	/**
	 * This method returns the Frame Status.
	 * @return Return 2 means Frame Acceptance<br>
	 * Return 3 means Frame Rejection<br>
	 * Return 4 means Kill Signal<br>
	 * Return 4 means a Node has finished all transmissions
	 */
	public byte getFrameStatus() {
		int sizeOfFrame = this.frameValue.length;
		return this.frameValue[sizeOfFrame - 1];
	}
}
