package bfttt;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import bftsmart.tom.ServiceProxy;

import org.json.JSONObject;

class HandleClient extends Thread {
    private final Socket socket;
    private final ServiceProxy proxy;
    private final String userData;

    public HandleClient(Socket socket, ServiceProxy proxy) {
        this.socket = socket;
        this.proxy = proxy;
        this.userData = this.socket.getRemoteSocketAddress().toString();
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

            return new String(message);
        }
        return null;
    }

    private boolean checkDisconnection(String response) {
        JSONObject responseObj = new JSONObject(response);
        return responseObj.getInt("action") == 4;
    }

    private void handleDisconnect(String e) {
        JSONObject response = new JSONObject();
        JSONObject rawMessage = new JSONObject();
        rawMessage.put("action", 4);
        response.put("userData", this.userData);
        response.put("rawMessage", rawMessage.toString());
        this.proxy.invokeOrdered(response.toString().getBytes());
        this.proxy.close();
        System.out.println("(" + this.userData + "): Desconectado. Motivo: " + e);
    }

    private void readMessages(InputStream inputStream, OutputStream outputStream) throws IOException {
        String request;
        JSONObject requestObj;
        String serverResponse;

        while(this.socket.isConnected()) {
            // Receive message from webclient
            try {
                request = decodeMessage(inputStream);
            } catch (SocketException socketException) {
                this.socket.close();
                break;
            }
            if (request == null) {
                this.socket.close();
                break;
            }

            // Make request object
            System.out.println("(" + this.userData + ")> " + request);
            requestObj = new JSONObject();
            requestObj.put("userData", this.userData);
            requestObj.put("rawMessage", request);

            // Forward client request to server
            byte[] response;
            try {
                response = proxy.invokeOrdered(requestObj.toString().getBytes());
            }
            catch (RuntimeException runtimeException) {
                // Servers offline
                System.out.println("Nao foi possivel conectar aos servidores: " + runtimeException);
                JSONObject offlineResponse = new JSONObject();
                offlineResponse.put("action", 4);
                offlineResponse.put("message", "Servidores offline");
                System.out.println("(" + this.userData + ")< " + offlineResponse);
                outputStream.write(encode(offlineResponse.toString()));
                outputStream.flush();
                return;
            }

            // Forward server response to webclient
            serverResponse = new String(response);
            System.out.println("(" + this.userData + ")< " + serverResponse);
            outputStream.write(encode(serverResponse));
            outputStream.flush();

            // Check if server disconnected the client
            if(checkDisconnection(serverResponse)) {
                this.socket.close();
                break;
            }
        }
        handleDisconnect("Cliente desconectou");
    }

    public void run() {
        System.out.println("Nova conexao: " + this.userData);

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
            readMessages(inputStream, outputStream);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}
