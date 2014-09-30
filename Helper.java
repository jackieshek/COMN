/* Jackie Shek s1035578 */
/**Computer Communications and Networks Coursework
 * Helper class, which contains methods for building packets, extracting data etc.
 * These methods are used at various points by SenderX and ReceiverX, and so I decided to
 * to put them into this Helper class, to make things less cluttered.
 */

import java.util.*;

public class Helper {

	/** Method used to create a "regular" packet from the parameters given, i.e.
	 *  i.e. the header bytes, the end-of-file byte and the payload.
	 *  A regular packet is a packet which is not the last packet, and so has 1024 payload.
	 * @param header
	 * @param eOF
	 * @param payload
	 * @return
	 */
	public static byte[] regPacketMaker(int header, byte eOF, byte[] payload) {
		//Create the additional information array, i.e. the header and the end-of-file,
		//as a byte[]
		byte[] infoBytes = new byte[] {
				(byte) (header >> 8), //since only allowed 2 bytes for header, discard the remaining 2 bytes of integer
				(byte) header,
				eOF
			};
		
		//Create the packet from, information array and payload
		byte[] bytePacket = new byte[infoBytes.length+payload.length];
		System.arraycopy(infoBytes, 0, bytePacket, 0, infoBytes.length);
		System.arraycopy(payload, 0, bytePacket, infoBytes.length, payload.length);
		return bytePacket;
	}
	
	/** Method used to create the last packet to be sent over UDP.
	 * 	Returns the actual remaining payload of bytes.
	 * @param header
	 * @param eOF
	 * @param payload
	 * @param actualLength
	 * @return
	 */
	public static byte[] lastPacketMaker(int header, byte eOF, byte[] payload, int actualLength) {
		//Create the additional information array, i.e. the header and the end-of-file,
		//as a byte[]
		byte[] infoBytes = new byte[] {
				(byte) (header >> 8), //since only allowed 2 bytes for header, discard the remaining 2 bytes of integer
				(byte) header,
				eOF
			};
		
		byte[] actualPayload = Arrays.copyOfRange(payload, 0, actualLength);  //check this -1 bit
		byte[] bytePacket = new byte[infoBytes.length+actualLength];
		System.arraycopy(infoBytes, 0, bytePacket, 0, infoBytes.length);
		System.arraycopy(actualPayload, 0, bytePacket, infoBytes.length, actualPayload.length);
		return bytePacket;
	}
	
	/** Extract the payload data from the packet.  Discards the header and end-of-file.
	 * 
	 * @param packet
	 * @return
	 */
	public static byte[] getData(byte[] packet) {
		byte[] data = Arrays.copyOfRange(packet, 3, packet.length);
		return data;		
	}
	
	/** Extract the actual payload of the last packet.  Removes any 0 bytes at the end of data.
	 *  Assumes these 0 bytes, were not part of the file originally.
	 * @param packet
	 * @return
	 */
	public static byte[] getLastData(byte[] packet) {
		byte[] data = Helper.getData(packet);
		int actualSize = 1023;
		for (int i=1023;i>=0;i--) {
			if (data[i] != 0) {
				actualSize = i+1;
				break;
			}
		}
		byte[] actualData = Arrays.copyOfRange(data, 0, actualSize);
		return actualData;
	}
	
	/** Extract the header bytes from a packet, and represent it as an integer.
	 * 
	 * @param packet
	 * @return
	 */
	public static int getHeaderAsInt(byte[] packet) {
		int header = (packet[0] << 8) + (packet[1] & 0xff);
		return header;
	}
	 
	/** Converts a number (header) from an integer to a byte array (byte[2]).
	 * 
	 * @param num
	 * @return
	 */
	public static byte[] getHeaderAsBytes(int num) {
		byte[] header = new byte[] {
				(byte) (num >> 8), //since only allowed 2 bytes for header, discard the remaining 2 bytes of integer
				(byte) num
			};
		return header;
	}
	
	/** Returns the end-of-file byte of a packet.
	 * 
	 * @param packet
	 * @return
	 */
	public static int getEof(byte[] packet) {
		int eof = (int) packet[2];
		return eof;
	}
}
