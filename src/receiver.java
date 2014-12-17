/**
 *
 * @author lsphate
 */
import java.net.*;
import java.io.*;
import java.sql.Timestamp;
import java.util.Date;

public class receiver
{
	final int MAX_SEG_SIZE = 2048;
	final int MAX_UDP_SIZE = 60 * 1024;

	DatagramSocket dgSkt = null;
	DatagramPacket incomingPkt = null;
	InetAddress sndIP;

	String filename = "../";
	File fileToReceive = null;
	FileOutputStream fOutStream = null;

	String logname = "../";
	File log = null;
	FileOutputStream logStream = null;
	PrintStream logTextout = null;

	int sndPort;
	int rcvPort;
	long filesize = 0;
	byte[] fileToReceive_byte;
	byte[] rcvBuff_byte;
	byte[] incomingData_byte;
	Date date = new Date();

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args)
	{
		receiver RDTReceiver = new receiver();
		RDTReceiver.ArgsCheck(args);
		RDTReceiver.WaitReceive();

	}

	public receiver()
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
		fileToReceive = new File(filename);
		try
		{
			fOutStream = new FileOutputStream(fileToReceive);
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
			sndIP = InetAddress.getByName(args[2]);
		}
		catch (UnknownHostException e)
		{
			System.out.println("Error: Bad server address.");
			System.exit(-1);
		}

		try
		{
			rcvPort = Integer.parseInt(args[1]);
			sndPort = Integer.parseInt(args[3]);
			
			dgSkt = new DatagramSocket(rcvPort);
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
//		System.out.println(rcvPort + sndPort);
	}

	public void WaitReceive()
	{
		incomingData_byte = new byte[MAX_UDP_SIZE];
		incomingPkt = new DatagramPacket(incomingData_byte, incomingData_byte.length);
		int orderExcept = 0;
		RUDPPacket rudpBack = new RUDPPacket();
		try
		{
			rudpBack.SetReceiverAddress(InetAddress.getLocalHost());
		}
		catch (UnknownHostException ex)
		{
			System.out.println("Error: Bad address.");
		}
		rudpBack.SetReceiverPort(rcvPort);
		rudpBack.SetSenderAddress(sndIP);
		rudpBack.SetSenderPort(sndPort);
		byte[] ackPacket_byte;

		while (true)
		{
			try
			{
				dgSkt.receive(incomingPkt);
				RUDPPacket rudpIn = new RUDPPacket(incomingPkt.getData());
				logTextout.println("Timestamp: " + (new Timestamp(date.getTime()))
						+ " Source: " + rudpIn.GetSenderAddress().toString()
						+ " Destination: " + rudpIn.GetReceiverAddress().toString()
						+ " Segment #: " + rudpIn.GetSegmentID()
						+ " Ack #: " + orderExcept
						+ " FIN: " + rudpIn.IsFin());

				if (orderExcept != rudpIn.GetSegmentID()
						|| RUDPPacket.VerifyChecksum(incomingPkt) == false
						|| RUDPPacket.VerifyAcknowledgement(incomingPkt, orderExcept) == false)
				{
					rudpBack.SetSegmentID(orderExcept - 1);
					ackPacket_byte = rudpBack.CreateRUDPPacket();
					DatagramPacket outgoingPkt = new DatagramPacket(ackPacket_byte, ackPacket_byte.length, sndIP, sndPort);
					dgSkt.send(outgoingPkt);
					continue;
				}

				rudpBack.SetSegmentID(orderExcept);
				ackPacket_byte = rudpBack.CreateRUDPPacket();
				DatagramPacket outgoingPkt = new DatagramPacket(ackPacket_byte, ackPacket_byte.length, sndIP, sndPort);
				dgSkt.send(outgoingPkt);

				fOutStream.write(rudpIn.GetReceiveData());
				if (rudpIn.IsFin() == 1)
				{
					break;
				}
				orderExcept++;
			}
			catch (Exception e)
			{
				System.out.println("Error: I/O Exception.");
			}

		}
		System.out.println("Delivery completed successfully.");
	}

}
