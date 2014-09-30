/* Jackie Shek s1035578 */
/**Computer Communications and Networks Coursework
 * Receiver 1
 */

import java.io.*;
import java.net.*;

public class Receiver1 {

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("Use program in the format: java Receiver1 <Port> <Filename>");
		} else {
			//Extract parameters given
			int port = Integer.parseInt(args[0]);
			File fileToWrite = new File(args[1]);
			
			//Set up streams and socket
			FileOutputStream fileStream = new FileOutputStream(fileToWrite);
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();  //Writing to fileStream to slow, therefore write to byteStream first
			DatagramSocket socket = new DatagramSocket(port);
			
			byte[] receivedBytes = new byte[1027];  //Buffer to receive the packet
			
			System.out.println("Waiting to receive file...");
			
			boolean receivedAllPackets = false;
			while (!receivedAllPackets) {  //Loop until we have received the last packet
				socket.setSoTimeout(0);
				//Receive packets sent by Sender (below)
				DatagramPacket receivedPacket = new DatagramPacket(receivedBytes, receivedBytes.length);
				socket.receive(receivedPacket);
				receivedBytes = receivedPacket.getData();
				if (receivedBytes[2] == 0) {  //Checks to see if received packet is not the end-of-file
					byte[] actualData = Helper.getData(receivedBytes);
					byteStream.write(actualData);
				} else {  //End-of-file case
					byte[] actualData = Helper.getLastData(receivedBytes);
					byteStream.write(actualData);
					receivedAllPackets = true;  //So break out of loop
				}
				receivedBytes = new byte[1027];  //Reset the buffer, may not be necessary
			}
			fileStream.write(byteStream.toByteArray());
			fileStream.close();
			byteStream.close();
			socket.close();
			System.out.println("File was received.");
		}
	}
	
}
