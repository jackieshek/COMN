/* Jackie Shek s1035578 */
/**Computer Communications and Networks Coursework
 * Sender 1
 */

import java.io.*;
import java.net.*;

public class Sender1 {

	public static void main(String[] args) throws Exception {
		
		if (args.length != 3) {
			System.out.println("Use program in the format: java Sender1 localhost <Port> <Filename>");
		} else {
			//Extract parameters
			InetAddress address = InetAddress.getByName(args[0]);
			int portNum = Integer.parseInt(args[1]);
			File fileToSend = new File(args[2]);
			
			//Set up stream and socket
			FileInputStream fileStream = new FileInputStream(fileToSend);
			DatagramSocket socket = new DatagramSocket();

			byte[] payload = new byte[1024];  //Buffer for collecting payload

			//Calculate the number of packets needed to send file (below)
			int sizeOfFile = fileStream.available();
			int numberOfPackets = (int) Math.ceil(sizeOfFile/(double)payload.length);

			System.out.println("Sending file...");
			//Loop to create packets of 1024 payload and the additional information, and send them (below)
			for (int i=0;i<numberOfPackets;i++) { //Regular packet cases, i.e. not last packet
				if (i!=numberOfPackets-1) {
					byte eOFcheck = 0;
					fileStream.read(payload);
					byte[] packet = Helper.regPacketMaker(i, eOFcheck, payload);  //Calls a class in Helper to make the packet
					DatagramPacket datagramPacket = new DatagramPacket(packet, packet.length, address, portNum);
					socket.send(datagramPacket);
					//Sleep for testing purposes (below)
//					Thread.sleep(10);
				} else { //Last packet case
					byte eOFcheck = 1;
					int bytesRead = fileStream.read(payload);
					byte[] lastPacket = Helper.lastPacketMaker(i, eOFcheck, payload, bytesRead);  //Calls a class in Helper to make last packet
					DatagramPacket datagramPacket = new DatagramPacket(lastPacket, lastPacket.length, address, portNum);
					socket.send(datagramPacket);
				}
			}
			fileStream.close();
			socket.close();
			System.out.println("File was sent successfully.");			
		}
	}
}

