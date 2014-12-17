import java.net.*;
import java.nio.*;

/**
 *
 * @author lsphate
 */
public class RUDPPacket
{
	private final static int CHKSUM_INDEX = 17;
	final int MAX_UDP_SIZE = 60 * 1024;
	final int MAX_SEG_SIZE = 2048;

	private byte[] customPacket = null;
	private byte[] sndData_byte = null;
	private byte[] rcvData_byte = null;
	private InetAddress sndAddr = null;
	private int sndPort = 0;
	private InetAddress rcvAddr = null;
	private int rcvPort = 0;
	private int segmentID = 0;
	private int isFIN = 0;
	private int dataLen = 0;

	public RUDPPacket()
	{
	}

	//Extract incoming Datagram
	public RUDPPacket(byte[] RawBytes)
	{
		try
		{
			byte[] sndAddr_byte = new byte[4];
			byte[] sndPort_byte = new byte[4];
			byte[] rcvAddr_byte = new byte[4];
			byte[] rcvPort_byte = new byte[4];
			byte[] control_byte = new byte[4];
			byte[] dataLen_byte = new byte[4];

			System.arraycopy(RawBytes, 0, sndAddr_byte, 0, 4);
			System.arraycopy(RawBytes, 4, sndPort_byte, 0, 4);
			System.arraycopy(RawBytes, 8, rcvAddr_byte, 0, 4);
			System.arraycopy(RawBytes, 12, rcvPort_byte, 0, 4);
			System.arraycopy(RawBytes, 16, control_byte, 0, 4);
			System.arraycopy(RawBytes, 20, dataLen_byte, 0, 4);

			sndAddr = InetAddress.getByAddress(sndAddr_byte);
			sndPort = ByteBuffer.wrap(sndPort_byte).getInt();
			rcvAddr = InetAddress.getByAddress(rcvAddr_byte);
			rcvPort = ByteBuffer.wrap(rcvPort_byte).getInt();
			segmentID = control_byte[0];
			isFIN = control_byte[2];
			dataLen = ByteBuffer.wrap(dataLen_byte).getInt();

			if (dataLen != 0)
			{
				rcvData_byte = new byte[dataLen];
				System.arraycopy(RawBytes, 24, rcvData_byte, 0, dataLen);
			}
			else
			{
				rcvData_byte = null;
			}
		}
		catch (Exception e)
		{
		}
	}

	public byte[] CreateRUDPPacket()
	{
		byte[] sndAddr_byte = sndAddr.getAddress();
		byte[] sndPort_byte = ByteBuffer.allocate(4).putInt(sndPort).array();
		byte[] rcvAddr_byte = rcvAddr.getAddress();
		byte[] rcvPort_byte = ByteBuffer.allocate(4).putInt(rcvPort).array();
		byte[] control_byte =
		{
			(byte) segmentID, //16
			(byte) 0x00, //17, the checksum.
			(byte) isFIN, //18
			(byte) 0x00	//19, would be the sequence no. which is not implement yet.
		};
		byte[] dataLen_byte;

		if (sndData_byte != null)
		{
			dataLen_byte = ByteBuffer.allocate(4).putInt(sndData_byte.length).array();
		}
		else
		{
			dataLen_byte = ByteBuffer.allocate(4).putInt(0x00000000).array();
		}

		customPacket = new byte[MAX_UDP_SIZE];

		System.arraycopy(sndAddr_byte, 0, customPacket, 0, sndAddr_byte.length);
		System.arraycopy(sndPort_byte, 0, customPacket, 4, sndPort_byte.length);
		System.arraycopy(rcvAddr_byte, 0, customPacket, 8, rcvAddr_byte.length);
		System.arraycopy(rcvPort_byte, 0, customPacket, 12, rcvPort_byte.length);
		System.arraycopy(control_byte, 0, customPacket, 16, control_byte.length);
		System.arraycopy(dataLen_byte, 0, customPacket, 20, dataLen_byte.length);

		if (sndData_byte != null)
		{
			
			System.arraycopy(sndData_byte, 0, customPacket, 24, sndData_byte.length);
		}

		customPacket[CHKSUM_INDEX] = ComputeChecksum(customPacket, customPacket.length);

		return customPacket;
	}

	public static byte ComputeChecksum(byte[] Packet, int Size)
	{
		byte checksum = 0x00;
		for (int i = 0; i < Size; ++i)
		{
			if (i == CHKSUM_INDEX)
			{
				checksum ^= 0x00;
			}
			else
			{
				checksum ^= Packet[i];
			}
		}

		return checksum;
	}

	public static boolean VerifyChecksum(DatagramPacket Packet)
	{
		byte[] data = Packet.getData();
		int size = Packet.getLength();

		byte chkSum = ComputeChecksum(data, size);
		return chkSum == data[CHKSUM_INDEX];
	}

	public static boolean VerifyAcknowledgement(DatagramPacket Packet, int SequenceNo)
	{
		byte incomingAck = Packet.getData()[16];
		return incomingAck == SequenceNo;
	}

	public void SetSenderAddress(InetAddress Address)
	{
		sndAddr = Address;
	}

	public InetAddress GetSenderAddress()
	{
		return sndAddr;
	}

	public void SetReceiverAddress(InetAddress Address)
	{
		rcvAddr = Address;
	}

	public InetAddress GetReceiverAddress()
	{
		return rcvAddr;
	}

	public void SetSenderPort(int Port)
	{
		sndPort = Port;
	}

	public int GetSenderPort()
	{
		return sndPort;
	}

	public void SetReceiverPort(int Port)
	{
		rcvPort = Port;
	}

	public int GetReceiverPort()
	{
		return rcvPort;
	}

	public void SetSegmentID(int Number)
	{
		segmentID = Number;
	}

	public int GetSegmentID()
	{
		return segmentID;
	}

	public void SetFIN()
	{
		isFIN = 1;
	}

	public void UnsetFin()
	{
		isFIN = 0;
	}

	public int IsFin()
	{
		return isFIN;
	}

	public void SetSendData(byte[] DataBytes)
	{
		sndData_byte = new byte[DataBytes.length];
		dataLen = DataBytes.length;
		System.arraycopy(DataBytes, 0, sndData_byte, 0, DataBytes.length);
	}
	
	public byte[] GetReceiveData()
	{
		return rcvData_byte;
	}
}
