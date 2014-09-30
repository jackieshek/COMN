/* Jackie Shek s1035578 */
/**Computer Communications and Networks
 * Receiver3
 */

import java.io.*;
import java.net.*;

public class Receiver3 {

	public static void main(String[] args) throws Exception {
		
		if (args.length != 2) {
			System.out.println("Use the program in the format: java Receiver3 <Port> <Filename>");
		} else {
			//Extract parameters
			int port = Integer.parseInt(args[0]);
			File fileToWrite = new File(args[1]);
			
			//Set up streams and sockets
			FileOutputStream fileStream = new FileOutputStream(fileToWrite);
			DatagramSocket socket = new DatagramSocket(port);
			
			byte[] receivedBytes = new byte[1027];
			
			System.out.println("Waiting to receive file...");
			
			boolean receivedAllPackets = false;
			int currentPacket = 0;
			while (!receivedAllPackets) {
				socket.setSoTimeout(0);
				DatagramPacket receivedPacket = new DatagramPacket(receivedBytes, receivedBytes.length);
				socket.receive(receivedPacket);
				
				//Get the sender address and port
				InetAddress senderAddress = receivedPacket.getAddress();
				int senderPort = receivedPacket.getPort();
				
				if (currentPacket == Helper.getHeaderAsInt(receivedBytes)) {  //This would discard duplicates
					//Send acknowledgement back to sender
					DatagramPacket ackPacket = new DatagramPacket(new byte[] {receivedBytes[0],receivedBytes[1]}, 2, senderAddress, senderPort);
					socket.send(ackPacket);
					receivedBytes = receivedPacket.getData();
					
					if (receivedBytes[2] == 0) {
						byte[] actualData = Helper.getData(receivedBytes);
						fileStream.write(actualData);
					} else {
						byte[] actualData = Helper.getLastData(receivedBytes);
						fileStream.write(actualData);
						receivedAllPackets = true;
					}
					
					currentPacket++;  //Move to next packet
					
				} else if (currentPacket < Helper.getHeaderAsInt(receivedBytes) && currentPacket == 0) {
					//Send acknowledgement of the last in-order packet received, as in Go-Back-N
					byte[] curAck = Helper.getHeaderAsBytes(currentPacket-1);
					DatagramPacket ackPacket = new DatagramPacket(curAck, 2, senderAddress, senderPort);
					socket.send(ackPacket);					
				}
				
				else if (currentPacket > Helper.getHeaderAsInt(receivedBytes)) {  //Got this packet already
					//Re-send this ack, original ack must have got lost
					byte[] pastAck = new byte[] {receivedBytes[0],receivedBytes[1]};
					DatagramPacket pastAckPacket = new DatagramPacket(pastAck, 2, senderAddress, senderPort);
					socket.send(pastAckPacket);
				}
				receivedBytes = new byte[1027];  //Flush buffer
			}
			fileStream.close();
			socket.close();
			System.out.println("File was received.");
		}
	}
}