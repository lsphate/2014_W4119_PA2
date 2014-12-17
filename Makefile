target:
	mkdir -p classes
	javac ./src/RUDPPacket.java -d ./classes
	javac ./src/receiver.java -d ./classes -classpath ./classes
	javac ./src/sender.java -d ./classes -classpath ./classes

clean:
	rm -f ./classes/RUDPPacket.class
	rm -f ./classes/receiver.class
	rm -f ./classes/sender.class
	rm -rf ./classes