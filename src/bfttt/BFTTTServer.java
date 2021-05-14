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
    private GameBoard gameState; // = new GameBoard();

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
        System.out.println("new player " + clientId + ": " + userData);
        return this.gameState.addNewPlayer(clientId, userData);
    }

    private boolean handleDisconnect(String userData) {
        System.out.println("disconnect: " + userData);
        return this.gameState.disconnectPlayer((userData));
    }

    private JSONObject secureGameState() {
        //JSONObject safeGameState = new JSONObject(this.gameState, JSONObject.getNames(this.gameState));
        JSONObject safeGameState = this.gameState.getJSON(); // this function already creates a new object
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
        // this.loadDBFile();

        // initialize game board
        this.gameState = new GameBoard();

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
                    int pos = requestObj.getInt("pos");
                    this.gameState.markPosition(pos, 1);
                    System.out.println("Tabuleiro: "+ this.gameState.getBoardJSON());
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
        return this.gameState.getJSON().toString().getBytes();
    }

    @Override
    public void installSnapshot(byte[] bytes) {
        this.gameState = new GameBoard();
    }

    public static void main(String[] args) {
        new BFTTTServer(Integer.parseInt(args[0]));
    }
}
