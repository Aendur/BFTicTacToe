package bfttt;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import bftsmart.tom.ServiceProxy;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import org.json.JSONException;
import org.json.JSONObject;

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
