/**
 *
 * @author lsphate
 */
import java.net.*;
import java.io.*;
import java.sql.Timestamp;
import java.util.Date;

public class sender
{
	final int MAX_SEG_SIZE = 2048;
	final int MAX_UDP_SIZE = 60 * 1024;

	DatagramSocket dgSkt = null;
	DatagramPacket incomingPkt = null;
	InetAddress rcvIP;

	String filename = "../";
	File fileToSend = null;
	FileInputStream fInStream = null;

	String logname = "../";
	File log = null;
	FileOutputStream logStream = null;
	PrintStream logTextout = null;

	int sndPort;
	int rcvPort;
	long filesize = 0;
	byte[] fileToSend_byte;
	byte[] sndBuff_byte;
	byte[] incomingData_byte;
	int reTrans = 0;
	int totalLen = 0;
	Date date = new Date();
	long startTime = 0;
	long endTime = 0;
	long eRTT = 0;

	public static void main(String[] args)
	{
		sender RDTsender = new sender();
		RDTsender.ArgsCheck(args);
		RDTsender.TrySend();

	}

	public sender()
	{
	}

	private void ArgsCheck(String[] args)
	{
		if (args.length < 5)
		{
			System.out.println("Error: Invalid invoke format.");
			System.exit(-1);
		}

		filename += args[0];
		fileToSend = new File(filename);
		try
		{
			fInStream = new FileInputStream(fileToSend);
		}
		catch (FileNotFoundException e)
		{
			System.out.println("Error: File not found. Program Terminating.");
			System.exit(-1);
		}

		logname += args[4];
		if (logname.equals("stdout"))
		{
			logTextout = System.out;
		}
		else
		{
			log = new File(logname);
			try
			{
				logStream = new FileOutputStream(log);
				logTextout = new PrintStream(logStream);
			}
			catch (FileNotFoundException e)
			{
				System.out.println("Error: File not found. Program Terminating.");
				System.exit(-1);
			}
		}

		try
		{
			rcvIP = InetAddress.getByName(args[1]);
		}
		catch (UnknownHostException e)
		{
			System.out.println("Error: Bad address.");
			System.exit(-1);
		}

		try
		{
			rcvPort = Integer.parseInt(args[2]);
			sndPort = Integer.parseInt(args[3]);

			dgSkt = new DatagramSocket(sndPort);
			dgSkt.setSoTimeout(1000);
		}
		catch (NumberFormatException e)
		{
			System.out.println("Error: Invaild port number.");
			System.exit(-1);
		}
		catch (SocketException ex)
		{
			System.out.println("Error: Socket creatation failed.");
		}
	}

	//Try stop-and-wait sending.
	public void TrySend()
	{
		fileToSend_byte = new byte[(int) fileToSend.length()];
		try
		{
			fInStream.read(fileToSend_byte);
			fInStream.close();
		}
		catch (Exception e)
		{
		}

		//Packet for waiting Acks.
		incomingData_byte = new byte[MAX_UDP_SIZE];
		incomingPkt = new DatagramPacket(incomingData_byte, MAX_UDP_SIZE);

		sndBuff_byte = new byte[MAX_SEG_SIZE];
		int count = 0;
		int segID = 0;
		boolean isFin = false;

		for (int i = 0; i < fileToSend_byte.length; i++)
		{
			sndBuff_byte[count] = fileToSend_byte[i];
			count++;
			if (count == (MAX_SEG_SIZE - 1) || i == (fileToSend_byte.length - 1))
			{
				if (i == (fileToSend_byte.length - 1))
				{
					isFin = true;
					for (int j = count; j < (MAX_SEG_SIZE - 1); j++)
					{
						sndBuff_byte[j] = 0x00;
					}
				}
				byte[] chunkWithHeader = WrapPacket(segID, isFin);
				DatagramPacket outgoingPkt = new DatagramPacket(chunkWithHeader, chunkWithHeader.length, rcvIP, rcvPort);

				while (true)
				{
					try
					{
						startTime = System.currentTimeMillis();
						dgSkt.send(outgoingPkt);
						reTrans++;
						totalLen = totalLen + outgoingPkt.getData().length;
						dgSkt.receive(incomingPkt);
						if (RUDPPacket.VerifyChecksum(incomingPkt) == true && RUDPPacket.VerifyAcknowledgement(incomingPkt, segID) == true)
						{
							endTime = System.currentTimeMillis();
							reTrans--;
							break;
						}
					}
					catch (Exception e)
					{
						logTextout.println("Timestamp: " + (new Timestamp(date.getTime()))
							+ " Timeout, retrying.");
						System.out.println("Error: Timeout, retrying.");
					}
				}

				eRTT = (long) ((eRTT * 0.875) + (endTime - startTime) * 0.125);

				try
				{
					logTextout.println("Timestamp: " + (new Timestamp(date.getTime()))
							+ " Source: " + InetAddress.getLocalHost().toString()
							+ " Destination: " + rcvIP.toString()
							+ " Segment #: " + segID
							+ " Ack #: " + incomingPkt.getData()[16]
							+ " FIN: " + isFin
							+ " Estimated RTT: " + eRTT);
				}
				catch (UnknownHostException ex)
				{
					System.out.println("Error: Bad address.");
				}
				count = 0;
				segID++;
			}
		}
		System.out.println("Delivery completed successfully.");
		System.out.println("Total bytes sent = " + totalLen);
		System.out.println("Segments sent = " + segID);
		System.out.println("Segments retransmitted = " + reTrans);
	}

	//Use RUDPPacket class to combine custom header and sndData segments.
	public byte[] WrapPacket(int SegmentID, boolean isFin)
	{
		RUDPPacket rudp = new RUDPPacket();
		rudp.SetReceiverAddress(rcvIP);
		rudp.SetReceiverPort(rcvPort);
		try
		{
			rudp.SetSenderAddress(InetAddress.getLocalHost());
		}
		catch (UnknownHostException e)
		{
			System.out.println("Error: Bad address.");
		}
		rudp.SetSenderPort(sndPort);
		rudp.SetSegmentID(SegmentID);
		rudp.SetSendData(sndBuff_byte);
		if (isFin)
		{
			rudp.SetFIN();
		}
		else
		{
			rudp.UnsetFin();
		}
		return rudp.CreateRUDPPacket();
	}

}
