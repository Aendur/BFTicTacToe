/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bfttt;

import java.util.Map;
import java.io.File;
import java.io.IOException;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.auth0.jwt.JWT;

public class BFTTTServer extends DefaultSingleRecoverable{
    private int id;
    private String dbPath;
    private JSONObject gameState = setInitialState();

    private JSONObject setInitialState() {
        JSONObject initialState = new JSONObject();
        JSONArray board = new JSONArray();
        board.put(0);
        board.put(0);
        board.put(0);
        board.put(0);
        board.put(0);
        board.put(0);
        board.put(0);
        board.put(0);
        board.put(0);
        initialState.put("board", board);
        initialState.put("status", 0);
        initialState.put("turn", -1);
        initialState.put("idPlayer1", -1);
        initialState.put("idPlayer2", -1);
        initialState.put("userDataPlayer1", "");
        initialState.put("userDataPlayer2", "");
        return initialState;
    }

    private void resetGameState() {
        JSONArray board = new JSONArray();
        board.put(0);
        board.put(0);
        board.put(0);
        board.put(0);
        board.put(0);
        board.put(0);
        board.put(0);
        board.put(0);
        board.put(0);
        this.gameState.put("board", board);
        this.gameState.put("status", 0);
        this.gameState.put("turn", -1);
        this.gameState.put("idPlayer1", this.gameState.getInt("idPlayer1"));
        this.gameState.put("idPlayer2", this.gameState.getInt("idPlayer2"));
        this.gameState.put("userDataPlayer1", this.gameState.getString("userDataPlayer1"));
        this.gameState.put("userDataPlayer2", this.gameState.getString("userDataPlayer2"));
    }

    private int getClientId(String token) {
        DecodedJWT jwt = JWT.decode(token);
        Map<String, Claim> claims = jwt.getClaims();
        Claim clientId = claims.get("clientId");
        return clientId.asInt();
    }

    private String getClientName(String token) {
        DecodedJWT jwt = JWT.decode(token);
        Map<String, Claim> claims = jwt.getClaims();
        Claim name = claims.get("name");
        return name.toString();
    }

    private boolean handleNewPlayer(int clientId, String userData) {
        if(this.gameState.getInt("idPlayer1") == -1) {
            this.gameState.put("idPlayer1", clientId);
            this.gameState.put("userDataPlayer1", userData);
            return true;
        }
        if(this.gameState.getInt("idPlayer2") == -1) {
            this.gameState.put("idPlayer2", clientId);
            this.gameState.put("userDataPlayer2", userData);
            this.gameState.put("status", 1);
            return true;
        }
        return false;
    }

    private boolean handleDisconnect(String userData) {
        if(userData.equals(this.gameState.getString(("userDataPlayer1")))) {
            this.gameState.put("idPlayer1", -1);
            this.gameState.put("userDataPlayer1", "");
            resetGameState();
            return true;
        }
        if(userData.equals(this.gameState.getString(("userDataPlayer2")))) {
            this.gameState.put("idPlayer2", -1);
            this.gameState.put("userDataPlayer2", "");
            resetGameState();
            return true;
        }
        return false;
    }

    private JSONObject secureGameState() {
        JSONObject safeGameState = new JSONObject(this.gameState, JSONObject.getNames(this.gameState));
        safeGameState.remove("userDataPlayer1");
        safeGameState.remove("userDataPlayer2");
        return safeGameState;
    }

    private void loadDBFile() {
        File dbFile = new File(this.dbPath);
        if (dbFile.exists()) {
            System.out.println("found db file " + this.dbPath);
            // load file

        } else {
            // create a new file if one does not exist
            System.out.println("creating new db file " + this.dbPath);
            try {
                dbFile.createNewFile();
            } catch (IOException e) {
                System.out.println("failed to create db file " + this.dbPath);
                System.out.println(e.toString());
            }
        }
    }

    public BFTTTServer(int id) {
        this.id = id;
        this.dbPath = "serverdata" + id + ".json";

        // load ongoing games to server
        this.loadDBFile();

        // initialize BFT
        new ServiceReplica(id,this,this);
    }

    @Override
    public byte[] appExecuteOrdered(byte[] bytes, MessageContext mc) {
        String request = new String(bytes);
        try {
            JSONObject requestObj = new JSONObject(request);
            int action = requestObj.getInt("action");
            String userData = requestObj.getString("userData");
            String token = requestObj.getString("token");
            String name = getClientName(token);
            int clientId = getClientId(token);
            JSONObject response = new JSONObject();

            switch(requestObj.getInt("action")) {
                case 3:
                    System.out.println("(ClientId: "+ clientId +")Acao = pedir para entrar no jogo");
                    if(!handleNewPlayer(clientId, userData)) {
                        response.put("action", 4);
                        response.put("message", "Ja existe um jogo em andamento");
                        return (response.toString()).getBytes();
                    }
                    System.out.println("Jogador " + clientId + " entrou no jogo");
                    break;
                case 4:
                    System.out.println("(ClientId: " + clientId + ")Acao = jogador desconectado");
                    response.put("action", 4);
                    response.put("message", "Cliente solicitou a desconexao");
                    handleDisconnect(userData);
                    return (response.toString()).getBytes();
                case 5:
                    System.out.println("(ClientId: "+ clientId +")Acao = marcar posicao");
                    JSONArray board = this.gameState.getJSONArray("board");
                    int pos = requestObj.getInt("pos");
                    board.put(pos, 1);
                    System.out.println("Tabuleiro: "+ gameState.getJSONArray("board"));
                    break;
                case 6:
                    //System.out.println("(ClientId: "+ clientId +")Acao = sincronizar");
                    break;
                default:
                    System.out.println("(ClientId: "+ clientId +")Acao nao reconhecida");
                    break;
            }

            response.put("action", action);
            response.put("gameState", secureGameState());
            return (response.toString()).getBytes();
        } catch (JSONException e) {
            JSONObject response = new JSONObject();
            response.put("action", 4);
            response.put("message", "Mensagem nao reconhecida");
            handleDisconnect(new JSONObject(request).getString("userData"));
            System.out.println("Mensagem nao reconhecida");
            return (response.toString()).getBytes();
        }
    }

    @Override
    public byte[] appExecuteUnordered(byte[] bytes, MessageContext mc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public byte[] getSnapshot() {
        return this.gameState.toString().getBytes();
    }

    @Override
    public void installSnapshot(byte[] bytes) {
        //c = Integer.parseInt(new String(bytes));
        JSONObject gameState = new JSONObject();
        gameState.put("board", new int[] {0, 0, 0, 0, 0, 0, 0, 0 ,0});
    }

    public static void main(String[] args) {
        new BFTTTServer(Integer.parseInt(args[0]));
    }
}
