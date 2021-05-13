server:
	javac -cp lib/* -d . src/bfttt/BFTTTServer.java

client:
	javac -cp lib/* -d . src/bfttt/BFTTTClient.java

all: server client
	jar cvf bfttt/bfttt.jar bfttt/BFTTTServer.class bfttt/BFTTTClient.class
	del bfttt\BFTTTServer.class
	del bfttt\BFTTTClient.class
	