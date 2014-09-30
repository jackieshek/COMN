/* Jackie Shek s1035578 */
/**Computer Communications and Networks Coursework
 * Receiver2
 */

import java.io.*;
import java.net.*;

public class Receiver2 {
	
	public static void main(String[] args) throws Exception {

		if (args.length != 2) {
			System.out.println("Use program in the format: java Receiver2 <Port> <Filename>");
		} else {
			//Extract information
			int port = Integer.parseInt(args[0]);
			File fileToWrite = new File(args[1]);
			
			//Set up the streams and socket
			FileOutputStream fileStream = new FileOutputStream(fileToWrite);
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			DatagramSocket socket = new DatagramSocket(port);
			
			byte[] receivedBytes = new byte[1027];  //Buffer to receive the packet
			
			System.out.println("Waiting to receive file...");
			
			boolean receivedAllPackets = false;
			int currentPacket = 0;
			while (!receivedAllPackets) {
				socket.setSoTimeout(0);
				DatagramPacket receivedPacket = new DatagramPacket(receivedBytes, receivedBytes.length);
				socket.receive(receivedPacket);
				
				//Get the sender address and port (below)
				InetAddress senderAddress = receivedPacket.getAddress();
				int senderPort = receivedPacket.getPort();
				
				//Send acknowledgement back to sender (below)
				DatagramPacket ackPacket = new DatagramPacket(new byte[] {receivedBytes[0],receivedBytes[1]}, 2, senderAddress, senderPort);
				socket.send(ackPacket);
				
				if (currentPacket == Helper.getHeaderAsInt(receivedBytes)) {  //Only write to byteStream if its the right packet, should discard any duplicates
					receivedBytes = receivedPacket.getData();
					if (receivedBytes[2] == 0) {  //Checks to see if the packet is the last packet
						byte[] actualData = Helper.getData(receivedBytes);
						byteStream.write(actualData);
					} else {
						byte[] actualData = Helper.getLastData(receivedBytes);
						byteStream.write(actualData);
						for (int i=0;i<10;i++) {  //Spam last ack to ensure sender stops
							socket.send(ackPacket);
						}
						receivedAllPackets = true;
					}
					currentPacket++;
				}
				receivedBytes = new byte[1027];  //Reset the buffer
			}
			byteStream.writeTo(fileStream);
			byteStream.close();
			fileStream.close();
			socket.close();
			System.out.println("File was received.");
		}
	}
}
