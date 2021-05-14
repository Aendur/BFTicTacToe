package eco;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import bftsmart.tom.ServiceProxy;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import org.json.JSONObject;

class HandleClient extends Thread {
    private final Socket socket;
    private final ServiceProxy proxy;
    private final int lastClientId;
    private final String token;

    public HandleClient(Socket socket, ServiceProxy proxy, int lastClientId) throws UnsupportedEncodingException {
        this.socket = socket;
        this.proxy = proxy;
        this.lastClientId = lastClientId;
        this.token = createToken();
    }

    private String createToken() {
        try {
            Algorithm algorithm = Algorithm.HMAC256("secret");
            Map<String, Object> headerClaims = new HashMap();
            headerClaims.put("clientId", this.lastClientId);
            String token = JWT.create()
                    .withIssuer("auth0")
                    .withHeader(headerClaims)
                    .sign(algorithm);
            return token;
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

    private void disconnectClient(OutputStream outputStream, String reason) throws IOException {
        System.out.println("(" + socket.getRemoteSocketAddress() + "): Desconectado. Motivo: " + reason);
        JSONObject response = new JSONObject();
        response.put("token", this.token);
        response.put("success", false);
        response.put("message", reason);
        sendMessage(outputStream, response.toString());
        handleDisconnect();
    }

    private void handleDisconnect() throws IOException {
        JSONObject response = new JSONObject();
        response.put("token", this.token);
        response.put("action", 2);
        response.put("clientId", socket.getRemoteSocketAddress());
        byte[] reply = proxy.invokeOrdered(response.toString().getBytes());
        socket.close();
    }

    private void readMessages(InputStream inputStream, OutputStream outputStream) throws IOException {
        int len = 0;
        byte[] b = new byte[1024];
        String message;

        while(!socket.isClosed()) {
            try {
                // Receive message from webclient
                message = decodeMessage(inputStream);
                if (message == null) {
                    handleDisconnect();
                    break;
                }

                System.out.println("(" + socket.getRemoteSocketAddress() + ")> " + message);

                // Send message to server
                JSONObject request = new JSONObject(message);
                request.put("token", this.token);
                request.put("clientId", socket.getRemoteSocketAddress());
                byte[] reply = proxy.invokeOrdered(request.toString().getBytes());
                String replyString = new String(reply);
                //System.out.println("Resposta recebida: " + replyString); // Print server response

                if(replyString.equals("dc")) {
                    disconnectClient(outputStream, "Ja existe um jogo em andamento");
                    break;
                }

                JSONObject response = new JSONObject();
                request.put("token", this.token);
                response.put("success", true);
                response.put("message", "");
                response.put("gameState", replyString.toString());
                sendMessage(outputStream, response.toString()); // Send message to webclient

            } catch (Exception e) {
                handleDisconnect();
                break;
            }
        }
    }

    private void sendMessage(OutputStream outputStream, String message) throws IOException {
        try {
            System.out.println("(" + socket.getRemoteSocketAddress() + ")< " + message);
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
        System.out.println("Nova conexao: " + socket.getRemoteSocketAddress());
        /*try {
            System.out.println("Nova conexao: " + socket.getRemoteSocketAddress());
            socket.setSoTimeout(30000);
        } catch (SocketException e) {
            e.printStackTrace();
        }*/

        InputStream inputStream;
        try {
            inputStream  = socket.getInputStream();
        } catch (IOException inputStreamException) {
            throw new IllegalStateException("Falha ao pegar o inputstream", inputStreamException);
        }

        OutputStream outputStream;
        try {
            outputStream  = socket.getOutputStream();
        } catch (IOException inputStreamException) {
            throw new IllegalStateException("Falha ao pegar o outputstream", inputStreamException);
        }

        try {
            doHandShakeToInitializeWebSocketConnection(inputStream, outputStream);
        } catch (UnsupportedEncodingException handShakeException) {
            throw new IllegalStateException("Falha na conexao com o cliente", handShakeException);
        }

        try {
            readMessages(inputStream, outputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Conexao perdida com " + socket.getRemoteSocketAddress(), e);
        }
    }
}

public class EcoClient{

    public static void main(String[] args) throws UnsupportedEncodingException {
        int portNumber = 8080;
        ServerSocket server;
        int lastClientId = 0;

        ServiceProxy proxy = new ServiceProxy(1001);

        try {
            server = new ServerSocket(portNumber);
            System.out.println("Servidor online " + server.getLocalSocketAddress());
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel criar o websocket", exception);
        }

        Socket clientSocket;
        while (true) {
            try {
                clientSocket = server.accept();
            } catch (IOException waitException) {
                throw new IllegalStateException("Tentativa de conexao com o cliente excedeu o tempo limite", waitException);
            }
            new eco.HandleClient(clientSocket, proxy, lastClientId).start();
            lastClientId++;

        }
    }
}
