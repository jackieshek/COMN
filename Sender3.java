/* Jackie Shek s1035578 */
/**Computer Communications and Networks Coursework
 * Sender 3
 */

//Note to marker, some of the comments are printing statements, for debugging purposes, and may be removed at your leisure.

import java.io.*;
import java.net.*;

public class Sender3 {
	private static int optimalRetransTimeout = 50;  //Note to self
	
	public static void main(String[] args) throws Exception {  //Might not actually be Go-Back-N
		
		if (args.length != 5) {
			System.out.println("Use program in the format: java Sender3 localhost <Port> <Filename> [RetryTimeout] [WindowSize]");
		} else {
			//Extract parameters
			InetAddress address = InetAddress.getByName(args[0]);
			int port = Integer.parseInt(args[1]);
			File fileToSend = new File(args[2]);
			int timeout = Integer.parseInt(args[3]);
			int windowSize = Integer.parseInt(args[4]);
			
			//Set up streams and socket
			FileInputStream fileStream = new FileInputStream(fileToSend);
			DatagramSocket socket = new DatagramSocket();
			
			byte[] payload = new byte[1024];  //Buffer to collect payload
			
			//Decided the easiest way to emulate Go-Back-N is first create an array of the packets we will send.
			//This is because we will need to re-send a group of packets, and thus we need to read a certain payload
			//of data several times.
			//This would obviously be impractical in real life situations.
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
				payload = new byte[1024];
			}
			
			//The Go-Back-N stuff
			System.out.println("Sending file...");
			boolean sentAllPackets = false;
			int currentFirstPacket = 0;
			
			long startTime = System.currentTimeMillis();  //Get start time
			long endTime = 0;
			
			int trans = 0;
			while(!sentAllPackets) {
				if (!(currentFirstPacket + windowSize > numberOfPackets)) {  //Case where we have more or equal to the window size of packets left to send
					//Send windowSize amount of packets
					for (int i=0;i<windowSize;i++) {
						//Send packet
						DatagramPacket packet = new DatagramPacket(packetsArray[i+currentFirstPacket], packetsArray[i+currentFirstPacket].length, address, port);
						socket.send(packet);
						trans++;
						if (currentFirstPacket == (i+currentFirstPacket)) {
							socket.setSoTimeout(timeout);  //Set the timeout for the first packet (of this group) sent for the "receiving ack" while loop (below)
						}
					}
				} else {  //Case where we no longer have enough packets (i.e. less than window size packets) to send
					//Send remaining packets
					windowSize = numberOfPackets-currentFirstPacket;
					for (int i=0;i<windowSize;i++) {
						//Send packet
						DatagramPacket packet = new DatagramPacket(packetsArray[i+currentFirstPacket], packetsArray[i+currentFirstPacket].length, address, port);
						socket.send(packet);
						trans++;
						if (currentFirstPacket == (i+currentFirstPacket)) {
							socket.setSoTimeout(timeout);  //Set the timeout of the first packet (of this group) sent for the "receiving ack" while loop (below)
						}
					}
				}
				int ackCount=0;
				//Receiving the acks for packets sent part
				while (ackCount != windowSize) {	//Loops until we have received windowSize amount of acks or until a timeout.
					try {
						byte[] ackByte = new byte[2];
						DatagramPacket ackPacket = new DatagramPacket(ackByte, ackByte.length);
						socket.receive(ackPacket);
						if (Helper.getHeaderAsInt(ackByte)==currentFirstPacket) {
							currentFirstPacket++;  //Got the ack for our currentFirstPacket, so shift window to next packet
							ackCount++;
						} else ackCount++;  //Not the ack we want, but count it
						if (Helper.getHeaderAsInt(ackByte)==numberOfPackets-1) {  //Received the ack for the last packet.
							sentAllPackets = true;
							break;
						}
						if (ackCount != windowSize) socket.setSoTimeout(timeout);  //Set timeout for next packet of the group
					} catch (SocketException socketExp) {
						//Timed out so return to main loop
						break;
					} catch (IOException ioExp) {
						//Not receiving any ackPackets so break back to main loop
						break;
					}
				}
			}
			endTime = System.currentTimeMillis();  //End time
			
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
}
