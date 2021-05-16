package bfttt;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

import bftsmart.tom.ServiceProxy;

public class BFTTTClient{
    public static void main(String[] args) throws IOException {
        int portNumber = 5001;
        ServerSocket server;
        Random rng = new Random();

        server = new ServerSocket(portNumber);
        System.out.println("Proxy online " + server.getLocalSocketAddress());

        Socket clientSocket;

        //noinspection InfiniteLoopStatement
        while (true) {
            clientSocket = server.accept();
            ServiceProxy proxy = new ServiceProxy(1001 + rng.nextInt(2000000000)); //lastProxyId++);
            new bfttt.HandleClient(clientSocket, proxy).start();
        }
    }
}
