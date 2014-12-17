A. Brief description

	My program contains three classes, first one is called RUDPPacket, which is used for creating custom packet that contained a header and the following data chunks; The header has 4 bytes for sender IP, 4 bytes for sender Port, 4 bytes for receiver IP, 4 bytes for receiver IP, 4 bytes for controler(including seqment #, checksum, FIN and the sequence # which is not inplement yet), and 4 bytes for the data chunk length.

	Then the receiver, which wait the incoming UDP packets, use RUDPPacket class to extract them, compare the segment number, send Acks, then combine them in to single file which named by user's argument.

	The third one is called sender, which reads the file in the main directory(where the Makefile at), turn it into bytes ,use RUDPPacket class to wrap them into datagram packet, then send and wait the receiver's Acks.

	Both receiver and sender will log their workflow into logfile specified by user.

B. Details on development environment

	It is hard to install JAVA 1.6 on Mac OSX 10.9 Maverick since Apple has it’s own JAVA version update control, so I use JAVA 1.7.0_67 to build my program. I’ve worked very carefully to avoid any API that has not been involved in JDK 1.6, but available in 1.7. And I’ve also test my code on a 1.6 machine as well.
	I used Netbeans 8.0.1 as my IDE, Mac/ubuntu Terminal and Windows CMD for run and test the JAVA program.

C. Instructions on how to run your code

	Just simply type make, and the .class file should be placed in ./classes directory. The runnable .class file is receiver.class and sender.class
	Use "java receiver <receive_file_name> <receiver_port> <sender_ip> <sender_port> <logfile_name>" to invoke receiver application.
	Use "java sender <send_file_name> <receiver_ip> <receiver_port> <sender_port> <logfile_name>" to invoke sender application.
	Any lack of arguments will receive exceptions and terminate the processes.

	The file to be sent need to be put at where the Makefile located, and the received file and logs will also be there.

	The sender will print timeout message if the Ack from receiver doesn't come back within 1000ms; The finish message will be printed after the delivery job is done.

D. More

	According to the instruction, the implementation of window size is optional, so I didn't implement that. This is a simple send-and-wait mechanism, the window size is default to 1 and cannot be modified.