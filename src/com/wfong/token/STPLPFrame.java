package com.wfong.token;

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
		String[] strTok = frameString.split(",");
		destinationAddress = (byte) (Integer.getInteger(strTok[0]) & 0xff);
		dataSize = (byte) (Integer.getInteger(strTok[1]) & 0xff);
		data = strTok[2].getBytes();
		//Set other data fields
		accessControl = 0; //TODO Implement various extra credit schemes
		frameControl = 1; //Frame is NOT a token
		frameStatus = 0; //Frame is newly constructed
		//Construct new Frame Value
		this.frameValue = new byte[dataSize + 6];
		this.frameValue[0] = accessControl;
		this.frameValue[1] = frameControl;
		this.frameValue[2] = destinationAddress;
		this.frameValue[3] = sourceAddress;
		this.frameValue[4] = dataSize;
		//Copy Data
		for (int i = 0; i < dataSize; i++) {
			this.frameValue[i + 5] = data[i];
		}
		this.frameValue[dataSize + 5] = frameStatus;
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
		builder.append("Source Address:........" + this.getSourceAddress() + "\n");
		builder.append("Destination Address:..." + this.getDestinationAddress() + "\n");
		builder.append("Data Size:............." + this.getDataSize());
		return builder.toString();
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
		if (frameValue[1] == 0 || tokenBit())
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
	 * This method returns the Reservation Level of the Frame.
	 * TODO Implement for Extra Credit.
	 * @return
	 */
	public int getReservationLevel() {
		return 0;
	}
	
	/**
	 * Returns status of the Frame Control Byte.
	 * @return True if Frame is a token; False if otherwise.
	 */
	public boolean getFrameControl() {
		
		return false;
	}
	
	/**
	 * This method returns the Destination Address Byte of the Frame.
	 * @return The Destination Address Byte.
	 */
	public int getDestinationAddress() {
		return 0;
	}
	
	/**
	 * This method returns the Source Address Byte of the Frame.
	 * @return The Source Address Byte.
	 */
	public int getSourceAddress() {
		return 0;
	}
	
	/**
	 * This method returns the Data Size of the Frame.
	 * @return The Data Size (in Bytes)
	 */
	public int getDataSize() {
		return 0;
	}
	
	/**
	 * Returns Binary Data of length of the specified Data Size.
	 * @return A byte array containing Data.
	 */
	public byte[] getBinaryData() {
		return null;
	}
	
	/**
	 * A static method which determines the Token Bit in the Access Control byte.
	 * @param accessControl The Access Control Byte.
	 * @return True if the Token Bit is flipped.
	 */
	public static boolean isToken(byte accessControl) {
		byte bitMask = 0x10;
		if ((accessControl & bitMask) == 0x10)
			return true;
		return false;
	}
}
