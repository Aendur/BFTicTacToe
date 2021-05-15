package bfttt;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import bftsmart.tom.ServiceProxy;

public class BFTTTClient{
    public static void main(String[] args) throws IOException {
        int portNumber = 5001;
        ServerSocket server;
        int lastClientId = 0;

        ServiceProxy proxy = new ServiceProxy(1001);

        server = new ServerSocket(portNumber);
        System.out.println("Proxy online " + server.getLocalSocketAddress());

        Socket clientSocket;
        while (true) {
            clientSocket = server.accept();
            new bfttt.HandleClient(clientSocket, proxy, lastClientId).start();
            lastClientId++;
        }
    }
}
