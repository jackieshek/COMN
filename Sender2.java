/* Jackie Shek s1035578 */
/**Computer Communications and Networks Coursework
 * Sender2
 */

//Note to marker, some of the comments are printing statements, for debugging purposes, and may be removed at your leisure.

import java.io.*;
import java.net.*;

public class Sender2 {

	public static void main(String[] args) throws Exception {
		
		if (args.length != 4) {
			System.out.println("Use program in the format: java Sender2 localhost <Port> <Filename> [RetryTimeout]");
		} else {
			//Extract the information from input
			InetAddress address = InetAddress.getByName(args[0]);
			int portNum = Integer.parseInt(args[1]);
			File fileToSend = new File(args[2]);
			int timeout = Integer.parseInt(args[3]);
			
			//Set up stream and socket
			FileInputStream fileStream = new FileInputStream(fileToSend);
			DatagramSocket socket = new DatagramSocket();

			byte[] payload = new byte[1024];  //Buffer for collecting payload

			//Calculate the number of packets needed to send file (below)
			int sizeOfFile = fileStream.available();
			int numberOfPackets = (int) Math.ceil(sizeOfFile/(double)payload.length);

			long startTime = System.currentTimeMillis();  //Get the start time
			long endTime = 0;
			int totalRetrans = 0;  //Set up the retransmission counter
			
			System.out.println("Sending file...");
			//Loop to create packets of 1024 payload and the additional information, and send them (below)
			for (int i=0;i<numberOfPackets;i++) {
				if (i!=numberOfPackets-1) {  //Regular packet cases, i.e. not last packet
					byte eOFcheck = 0;
					fileStream.read(payload);
					byte[] packet = Helper.regPacketMaker(i, eOFcheck, payload);
					
					boolean receivedAck = false;
					while (!receivedAck) {  //Send this packet until we receive ack.
						DatagramPacket datagramPacket = new DatagramPacket(packet, packet.length, address, portNum);
						socket.send(datagramPacket);

						//Get ack (below)
						receivedAck = getAck(socket, i, timeout);  //Uses a method to get the ack (bottom of file)
						if (!receivedAck) totalRetrans++;  //Not received ack so a re-send of the packet is needed.
					}
					
					payload = new byte[1024];  //Flush the buffer
					
				} else {  //Last packet case
					byte eOFcheck = 1;
					int bytesRead = fileStream.read(payload);
					byte[] lastPacket = Helper.lastPacketMaker(i, eOFcheck, payload, bytesRead);
					
					boolean receivedAck = false;
					while (!receivedAck) {  //Send this packet until we receive ack.
						DatagramPacket datagramPacket = new DatagramPacket(lastPacket, lastPacket.length, address, portNum);
						socket.send(datagramPacket);
							
						//Get ack (below)
						receivedAck = getAck(socket, i, timeout);  //Uses a method to get the ack (bottom of file)
						if (!receivedAck) totalRetrans++;  //Not received ack so a re-send of the packet is needed.
					}
					endTime = System.currentTimeMillis();  //End time
				}
			}
			socket.close();
			System.out.println("File was sent successfully. Packets sent: " + numberOfPackets);
//			System.out.println("Start time: " +startTime);
//			System.out.println("End time:   " +endTime);
			double transferTimeInSeconds = ((endTime - startTime)/(double)1000);
			System.out.println("Transfer time: " + transferTimeInSeconds);
			System.out.println("Average throughput: " + (sizeOfFile/(double)1024)/transferTimeInSeconds);
			System.out.println("Number of retransmissions: " + totalRetrans);
		}
	}
	
	
	/** Gets the ack for a given packet.  If received the appropriate ack, then returns true (saying
	 *  the sender can proceed to next packet).  Else it returns false (sender should resend this package).
	 * 
	 * @param socket
	 * @param header
	 * @param timeout
	 * @return
	 * @throws Exception
	 */
	public static boolean getAck(DatagramSocket socket, int header, int timeout) throws Exception {
		boolean ack = false;
		try {
			byte[] ackByte = new byte[2];
			DatagramPacket ackPacket = new DatagramPacket(ackByte, ackByte.length);
			socket.setSoTimeout(timeout);
			socket.receive(ackPacket);
			if (Helper.getHeaderAsInt(ackByte) == header) {  //Checks if ack is right one.
				ack = true;
			} else ack=false;
		} catch (SocketException socketExp) {
			//Socket timed out
			ack=false;
		} catch (IOException ioExp) {
			//Not receiving an ack
			ack=false;
		}
		return ack;
	}
}
