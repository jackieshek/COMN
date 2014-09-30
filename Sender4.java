/* Jackie Shek s1035578 */
/**Computer Communications and Networks Coursework
 * Sender4
 */

//Note to marker, some of the comments are printing statements, for debugging purposes, and may be removed at your leisure.

import java.io.*;
import java.net.*;

public class Sender4 {
	private static int optimalRetransTimeout = 50;  //Subject to change

	public static void main(String[] args) throws Exception {
		
		if (args.length != 5) {
			System.out.println("Use the program in the format: java Sender4 localhost <Port> <Filename> [RetryTimeout] [WindowSize]");
		} else {
			//Extract parameters
			InetAddress address = InetAddress.getByName(args[0]);
			int port = Integer.parseInt(args[1]);
			File fileToSend = new File(args[2]);
			int timeout = Integer.parseInt(args[3]);
			int windowSize = Integer.parseInt(args[4]);
			
			//Set up stream and socket
			FileInputStream fileStream = new FileInputStream(fileToSend);
			DatagramSocket socket = new DatagramSocket();
			
			byte[] payload = new byte[1024];
			
			//Like my Go-Back-N implementation, store all data in an array of array of bytes.
			//This is so that we can read a payload of data several times.
			//We will need to send them several times, if the packets get lost in transmission.
			//This would not be ideal for bigger files.
			int sizeOfFile = fileStream.available();
			int numberOfPackets = (int) Math.ceil(sizeOfFile/(double)payload.length);
			
			//The array of packets
			byte[][] packetsArray = new byte[numberOfPackets][1027];
			for (int i=0;i<numberOfPackets;i++) {
				if (i!=numberOfPackets-1) {
					byte eOFcheck = 0;
					fileStream.read(payload);
					packetsArray[i] = Helper.regPacketMaker(i, eOFcheck, payload);
				} else {
					byte eOFcheck = 1;
					int bytesRead = fileStream.read(payload);
					packetsArray[i] = Helper.lastPacketMaker(i, eOFcheck, payload, bytesRead);
				}
			}
			
			//A boolean array to keep track of packets the sender knows it has sent to
			//receiver correctly (below)
			boolean[] receivedAck = new boolean[numberOfPackets];
			
			System.out.println("Sending file...");
			boolean sentAllPackets = false;
			int currentFirstPacket = 0;
			
			long startTime = System.currentTimeMillis();
			long endTime = 0;
			
			int successPacketsSent = 0;
			int trans = 0;
			while(!sentAllPackets || currentFirstPacket != numberOfPackets) {  //Only break out when we have all acks
				if (!(currentFirstPacket + windowSize > numberOfPackets)) {  //Case where we have more or equal to the window size of packets left to send
					//Send windowSize amount of packets
					for (int i=0;i<windowSize;i++) {
						if (!receivedAck[i+currentFirstPacket]) {  //If receiver has not received this packet, send it
							DatagramPacket packet = new DatagramPacket(packetsArray[i+currentFirstPacket], packetsArray[i+currentFirstPacket].length, address, port);
							socket.send(packet);
							trans++;
						}
						if ((i+currentFirstPacket)==(windowSize+currentFirstPacket)) {
							socket.setSoTimeout(timeout);  //Set the timeout for the last packet (of this group) sent for the "receiving ack" while loop (below)
						}
					}
				} else {  //Case where we no longer have enough packets (i.e. less than window size packets) to send
					//Send remaining packets
					windowSize = numberOfPackets-currentFirstPacket;
					for (int i=0;i<windowSize;i++) {
						if (!receivedAck[i+currentFirstPacket]) {  //If receiver has not received this packet, send it
							DatagramPacket packet = new DatagramPacket(packetsArray[i+currentFirstPacket], packetsArray[i+currentFirstPacket].length, address, port);
							socket.send(packet);
							trans++;
						}
						if ((i+currentFirstPacket)==(windowSize+currentFirstPacket)) {
							socket.setSoTimeout(timeout);  //Set the timeout of the first packet (of this group) sent for the "receiving ack" while loop (below)
						}
					}
				}
				int ackCount=0;
				//Receiving the acks for packets sent part
				while (ackCount != windowSize) {  //Loops until we have received windowSize amount of acks until timeouts.
					try {
						byte[] ackByte = new byte[2];
						DatagramPacket ackPacket = new DatagramPacket(ackByte, ackByte.length);
						socket.receive(ackPacket);
						
						if (Helper.getHeaderAsInt(ackByte)==currentFirstPacket) {
							if (receivedAck[Helper.getHeaderAsInt(ackByte)]==false) {  //If we have not received the ack for this packet yet
								receivedAck[Helper.getHeaderAsInt(ackByte)] = true;
								currentFirstPacket = updateCurFirstPacket(currentFirstPacket, receivedAck);  //Got the ack for our currentFirstPacket, so shift window to next packet
																											//that we have not received an ack for.
								ackCount++;
								successPacketsSent++;
							}
						} else if (Helper.getHeaderAsInt(ackByte)>currentFirstPacket 
								&& Helper.getHeaderAsInt(ackByte)<(currentFirstPacket+windowSize)){  //If we have not received the ack for this packet yet
							if (receivedAck[Helper.getHeaderAsInt(ackByte)]==false) {
								receivedAck[Helper.getHeaderAsInt(ackByte)] = true;
								ackCount++;
								successPacketsSent++;
							}
						}
						if (ackCount != windowSize) socket.setSoTimeout(timeout);  //Set timeout for next packet of the group
					} catch (SocketException socketExp) {
						//Timed out so return to main loop
						break;
					} catch (IOException ioExp) {
						//Not receiving any ackPackets
						break;
					}
				}
				if (successPacketsSent == numberOfPackets) sentAllPackets = true;
			}
			endTime = System.currentTimeMillis();
			
			socket.close();
			System.out.println("File sent successfully. Packets sent: " + numberOfPackets);
//			System.out.println("Start time: " +startTime);
//			System.out.println("End time:   " +endTime);
			double transferTimeInSeconds = ((endTime - startTime)/(double)1000);
			System.out.println("Transfer time: " + transferTimeInSeconds);
			System.out.println("Average throughput: " + (sizeOfFile/(double)1024)/transferTimeInSeconds);
			System.out.println("Number of retransmissions: " + (trans-numberOfPackets));
			
		}
	}
	
	/** Updates the current head position for the window.
	 * 
	 * @param cur
	 * @param bools
	 * @return
	 */
	public static int updateCurFirstPacket(int cur, boolean[] bools) {
		for (int i=cur;i<bools.length;i++) {
			if (bools[i]==false) {
				return i;
			}
		}
		return bools.length;
	}
}
