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

class HandleClient extends Thread {
    private final Socket socket;
    private final ServiceProxy proxy;
    private final int lastClientId;
    private String token;
    private final String userData;

    public HandleClient(Socket socket, ServiceProxy proxy, int lastClientId) {
        this.socket = socket;
        this.proxy = proxy;
        this.lastClientId = lastClientId;
        this.token = null;
        this.userData = this.socket.getRemoteSocketAddress().toString();
    }

    private String createToken(String name) {
        try {
            Algorithm algorithm = Algorithm.HMAC256("43214hb3jk2g14h32g1hj432g1j423j");
            return JWT.create()
                    .withIssuer("auth0")
                    .withClaim("clientId", this.lastClientId)
                    .withClaim("name", name)
                    .sign(algorithm);
        } catch (JWTCreationException | UnsupportedEncodingException exception){
            //Invalid Signing configuration / Couldn't convert Claims.
            return "";
        }
    }

    private static void doHandShakeToInitializeWebSocketConnection(InputStream inputStream, OutputStream outputStream) throws UnsupportedEncodingException {
        String data = new Scanner(inputStream,"UTF-8").useDelimiter("\\r\\n\\r\\n").next();

        Matcher get = Pattern.compile("^GET").matcher(data);

        if (get.find()) {
            Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
            match.find();

            byte[] response = null;
            try {
                response = ("HTTP/1.1 101 Switching Protocols\r\n"
                        + "Connection: Upgrade\r\n"
                        + "Upgrade: websocket\r\n"
                        + "Sec-WebSocket-Accept: "
                        + DatatypeConverter.printBase64Binary(
                        MessageDigest
                                .getInstance("SHA-1")
                                .digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                                        .getBytes("UTF-8")))
                        + "\r\n\r\n")
                        .getBytes("UTF-8");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            try {
                outputStream.write(response, 0, response.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
        }
    }

    private static String decodeMessage(InputStream inputStream) throws IOException {
        int len = 0;
        byte[] b = new byte[1024];

        len = inputStream.read(b);
        if(b[0] == -120 && b[1] == -128) return null;
        if(len!=-1) {
            byte rLength = 0;
            int rMaskIndex = 2;
            int rDataStart = 0;

            byte data = b[1];
            byte op = (byte) 127;
            rLength = (byte) (data & op);

            if(rLength==(byte)126) rMaskIndex=4;
            if(rLength==(byte)127) rMaskIndex=10;

            byte[] masks = new byte[4];

            int j=0;
            int i=0;
            for(i=rMaskIndex;i<(rMaskIndex+4);i++){
                masks[j] = b[i];
                j++;
            }

            rDataStart = rMaskIndex + 4;

            int messLen = len - rDataStart;

            byte[] message = new byte[messLen];

            for(i=rDataStart, j=0; i<len; i++, j++){
                message[j] = (byte) (b[i] ^ masks[j % 4]);
            }

            b = new byte[1024];

            return new String(message);
        }
        return null;
    }

    private void waitForAuth(InputStream inputStream, OutputStream outputStream) throws IOException {
        String message;
        while(!this.socket.isClosed() && this.token == null) {
            message = decodeMessage(inputStream);
            if (message == null) {
                handleDisconnect("Cliente encerrou a conexao forcadamente");
                break;
            }
            System.out.println("(" + this.userData + ")> " + message);
            JSONObject request = new JSONObject(message);
            // Send token to client on first request
            if(request.getInt("action") == 2) {
                JSONObject response = new JSONObject();
                this.token = createToken(request.getString(("name")));
                response.put("action", 2);
                response.put("message", "Token de acesso recebido");
                response.put("userData", this.userData);
                response.put("token", this.token);
                sendMessage(outputStream, response.toString()); // Send message to webclient
                readMessages(inputStream, outputStream);
            }
        }
    }

    private JSONObject forwardClientRequest(String message) throws JSONException {
        JSONObject request = new JSONObject(message);
        request.put("userData", this.userData);
        byte[] response = proxy.invokeOrdered(request.toString().getBytes());
        JSONObject responseObject = new JSONObject(new String(response));
        //System.out.println("Resposta recebida: " + replyString); // Print server response
        return responseObject;
    }

    private boolean checkDisconnection(JSONObject serverResponse, OutputStream outputStream) throws IOException {
        if(serverResponse.getInt("action") == 4) {
            JSONObject response = new JSONObject();
            response.put("action", 4);
            response.put("message", serverResponse.getString("message"));
            sendMessage(outputStream, response.toString()); // Send message to webclient
            return true;
        }
        else return false;
    }

    private void handleDisconnect(String e) {
        JSONObject response = new JSONObject();
        response.put("action", 4);
        response.put("userData", this.userData);
        this.proxy.invokeOrdered(response.toString().getBytes());
        System.out.println("(" + this.userData + "): Desconectado. Motivo: " + e);
    }

    private void readMessages(InputStream inputStream, OutputStream outputStream) throws IOException {
        int len = 0;
        byte[] b = new byte[1024];
        String message;
        JSONObject serverResponse;

        while(!this.socket.isClosed()) {
            try {
                // Receive message from webclient
                message = decodeMessage(inputStream);
                if (message == null) {
                    handleDisconnect("Cliente encerrou a conexao forcadamente");
                    break;
                }

                System.out.println("(" + this.userData + ")> " + message);

                // Send message to server
                serverResponse = forwardClientRequest(message);

                // Send disconnect signal to webclient
                if(checkDisconnection(serverResponse, outputStream)) {
                    handleDisconnect(serverResponse.getString("message"));
                    break;
                }

                // Forward server response to webclient
                JSONObject response = serverResponse;
                sendMessage(outputStream, response.toString()); // Send message to webclient

            } catch (JSONException e) {
                handleDisconnect("Cliente encerrou a conexao forcadamente");
                break;
            }
        }
    }

    private void sendMessage(OutputStream outputStream, String message) {
        try {
            System.out.println("(" + this.userData + ")< " + message);
            outputStream.write(encode(message));
            outputStream.flush();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] encode(String mess) throws IOException{
        byte[] rawData = mess.getBytes();

        int frameCount  = 0;
        byte[] frame = new byte[10];

        frame[0] = (byte) 129;

        if(rawData.length <= 125){
            frame[1] = (byte) rawData.length;
            frameCount = 2;
        }else if(rawData.length >= 126 && rawData.length <= 65535){
            frame[1] = (byte) 126;
            int len = rawData.length;
            frame[2] = (byte)((len >> 8 ) & (byte)255);
            frame[3] = (byte)(len & (byte)255);
            frameCount = 4;
        }else{
            frame[1] = (byte) 127;
            int len = rawData.length;
            frame[2] = (byte)((len >> 56 ) & (byte)255);
            frame[3] = (byte)((len >> 48 ) & (byte)255);
            frame[4] = (byte)((len >> 40 ) & (byte)255);
            frame[5] = (byte)((len >> 32 ) & (byte)255);
            frame[6] = (byte)((len >> 24 ) & (byte)255);
            frame[7] = (byte)((len >> 16 ) & (byte)255);
            frame[8] = (byte)((len >> 8 ) & (byte)255);
            frame[9] = (byte)(len & (byte)255);
            frameCount = 10;
        }

        int bLength = frameCount + rawData.length;

        byte[] reply = new byte[bLength];

        int bLim = 0;
        for(int i=0; i<frameCount;i++){
            reply[bLim] = frame[i];
            bLim++;
        }
        for(int i=0; i<rawData.length;i++){
            reply[bLim] = rawData[i];
            bLim++;
        }

        return reply;
    }

    public void run() {
        try {
            System.out.println("Nova conexao: " + this.userData);
            socket.setSoTimeout(30000);
        } catch (SocketException e) {
            handleDisconnect("Timeout");
            e.printStackTrace();
        }

        InputStream inputStream;
        try {
            inputStream  = socket.getInputStream();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao obter o inputstream", e);
        }

        OutputStream outputStream;
        try {
            outputStream  = socket.getOutputStream();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao obter o outputstream", e);
        }

        try {
            doHandShakeToInitializeWebSocketConnection(inputStream, outputStream);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Falha na conexao com o cliente", e);
        }

        try {
            waitForAuth(inputStream, outputStream);
        } catch (IOException e) {
            handleDisconnect("Conexao perdida: " + e);
        }
        finally {
            handleDisconnect("Conexao perdida");
        }
    }
}

public class BFTTTClient{

    public static void main(String[] args) throws IOException {
        int portNumber = 8080;
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
