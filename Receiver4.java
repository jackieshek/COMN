/* Jackie Shek s1035578 */
/**Computer Communications and Networks Coursework
 * Receiver4
 */

import java.io.*;
import java.net.*;

public class Receiver4 {

	public static void main(String[] args) throws Exception {  //This needs a lot of work!!!
		
		if (args.length != 3) {
			System.out.println("Use program in the format: <Port> <Filename> [WindowSize]");
		} else {
			//Extract parameters
			int port = Integer.parseInt(args[0]);
			File fileToWrite = new File(args[1]);
			int windowSize = Integer.parseInt(args[2]);
			
			//Set up streams and socket
			FileOutputStream fileStream = new FileOutputStream(fileToWrite);
			DatagramSocket socket = new DatagramSocket(port);
			
			byte[] receivedBytes = new byte[1027];
			
			//In a sense these are the receiver windows (below)
			byte[][] tempStorage = new byte[windowSize][1027];  //Array for keeping packets which are ahead of currentFirstPacket
			boolean[] tempStorageFilled = new boolean[windowSize];
			
			boolean receivedAllPackets = false;
			int currentFirstPacket = 0;
			System.out.println("Waiting to receive file...");
			while (!receivedAllPackets) {  //
				socket.setSoTimeout(0);
				DatagramPacket receivedPacket = new DatagramPacket(receivedBytes, receivedBytes.length);
				socket.receive(receivedPacket);
				//Get the sender address and port
				InetAddress senderAddress = receivedPacket.getAddress();
				int senderPort = receivedPacket.getPort();
				
				if (currentFirstPacket == Helper.getHeaderAsInt(receivedBytes)) {  //This would discard duplicates
					receivedBytes = receivedPacket.getData();
					tempStorage[0] = receivedBytes;
					tempStorageFilled[0] = true;
					
					//Send acknowledgement of receipt of packet
					DatagramPacket ackPacket = new DatagramPacket(new byte[] {receivedBytes[0],receivedBytes[1]}, 2, senderAddress, senderPort);
					socket.send(ackPacket);
					
					//Write window to byteStream
					int addOn = getFalse(tempStorageFilled);  //Gets the first place in the window that is not filled
					for (int i=0;i<addOn;i++){
						if (Helper.getEof(receivedBytes)==0) {
							byte[] tempdata = Helper.getData(tempStorage[i]);
							fileStream.write(tempdata);
						} else {
							receivedAllPackets = true;
							byte[] tempdata = Helper.getLastData(tempStorage[i]);
							fileStream.write(tempdata);
						}
					}
					//Update window arrays
					byte[][] newTempStorage = shiftByteArray(tempStorage,addOn);
					boolean[] newTempStorageFilled = shiftBoolArray(tempStorageFilled,addOn);
					tempStorage = newTempStorage;
					tempStorageFilled = newTempStorageFilled;
					
					//Update currentFirstPacket, and so shifts window to the next place in window that has not been filled
					currentFirstPacket += addOn;
					
					
				} else if (currentFirstPacket < Helper.getHeaderAsInt(receivedBytes) && Helper.getHeaderAsInt(receivedBytes) < (currentFirstPacket+windowSize) 
						&& currentFirstPacket == 0) {  //Even if we have received file before it should not matter
					//Send acknowledgement back to sender
					DatagramPacket ackPacket = new DatagramPacket(new byte[] {receivedBytes[0],receivedBytes[1]}, 2, senderAddress, senderPort);
					socket.send(ackPacket);
					
					receivedBytes = receivedPacket.getData();
					int where = windowSize-((currentFirstPacket+windowSize)-Helper.getHeaderAsInt(receivedBytes));
					tempStorage[where] = receivedBytes;
					tempStorageFilled[where] = true;
					
				} else if (currentFirstPacket > Helper.getHeaderAsInt(receivedBytes)) {
					//Already had this packet, ack got lost, so send again.
					DatagramPacket ackPacket = new DatagramPacket(new byte[] {receivedBytes[0],receivedBytes[1]}, 2, senderAddress, senderPort);
					socket.send(ackPacket);
				}
				
				if(receivedAllPackets) {  //We have all of file. Yay! Now...
					for (int i=0;i<10;i++) {  //Spam the ack for the last received packet.
						DatagramPacket ackPacket = new DatagramPacket(new byte[] {receivedBytes[0],receivedBytes[1]}, 2, senderAddress, senderPort);
						socket.send(ackPacket);
					}
				}
				
				receivedBytes = new byte[1027];  //flush buffer
			}
			fileStream.close();
			socket.close();
			System.out.println("File was received.");
		}
	}
	
	/** Gets the location of the first false in an array of booleans.
	 *  Returns the length of the array of booleans if all values in the array are true.
	 * @param bools
	 * @return
	 */
	public static int getFalse(boolean[] bools) {
		for (int i=0;i<bools.length;i++) {
			if(bools[i]==false) return i;
		}
		return bools.length;
	}
	
	/** Method for shifting the receiver window by the amount specified.
	 * 
	 * @param temp
	 * @param shift
	 * @return
	 */
	public static byte[][] shiftByteArray(byte[][] temp, int shift) {
		if (shift < temp.length) {
			for (int i=0;i<temp.length-shift;i++) {
				temp[i] = temp[i+shift];
			}
			for (int j=shift+1;j<temp.length;j++) {
				temp[j] = new byte[1024];
			}
			return temp;
		} else
			return new byte[temp.length][];
	}
	
	/** Method for shifting the boolean window by the amount specified.
	 * 
	 * @param temp
	 * @param shift
	 * @return
	 */
	public static boolean[] shiftBoolArray(boolean[] temp, int shift) {
		if (shift < temp.length) {
			for (int i=0;i<temp.length-shift;i++) {
				temp[i] = temp[i+shift];
			}
			for (int j=shift+1;j<temp.length;j++) {
				temp[j] = false;
			}
			return temp;
		} else
			return new boolean[temp.length];
	}
}