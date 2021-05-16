package bfttt;

import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import bftsmart.tom.ServiceProxy;

import org.json.JSONException;
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

    private JSONObject forwardClientRequest(String message) throws JSONException {
        JSONObject request = new JSONObject(message);
        request.put("userData", this.userData);
        byte[] response = proxy.invokeOrdered(request.toString().getBytes());
        return new JSONObject(new String(response));
    }

    private boolean checkDisconnection(JSONObject serverResponse) {
        return serverResponse.getInt("action") == 4;
    }

    private void handleDisconnect(String e) {
        JSONObject response = new JSONObject();
        response.put("action", 0);
        response.put("userData", this.userData);
        this.proxy.invokeOrdered(response.toString().getBytes());
        this.proxy.close();
        System.out.println("(" + this.userData + "): Desconectado. Motivo: " + e);
    }

    private void readMessages(InputStream inputStream, OutputStream outputStream) throws IOException {
        String message;
        JSONObject serverResponse;

        while(!this.socket.isClosed()) {
            // Receive message from webclient
            message = decodeMessage(inputStream);
            if (message == null) {
                break;
            }

            // Forward client request to server
            System.out.println("(" + this.userData + ")> " + message);
            serverResponse = forwardClientRequest(message);

            // Forward server response to webclient
            System.out.println("(" + this.userData + ")< " + serverResponse);
            sendMessage(outputStream, serverResponse.toString());

            // Check if client has disconnected
            if(checkDisconnection(serverResponse)) {
                this.socket.close();
                break;
            }
        }
        handleDisconnect("Cliente desconectou");
    }

    private void sendMessage(OutputStream outputStream, String message) {
        try {
            outputStream.write(encode(message));
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        } catch (Exception e) {
            handleDisconnect("Cliente desconectou de forma inesperada: " + e);
        }
    }
}
