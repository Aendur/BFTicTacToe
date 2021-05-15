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
        return name.asString();
    }

    private boolean handleNewPlayer(int clientId, String userData, String name) {
        return this.gameState.addNewPlayer(clientId, userData, name);
    }

    private boolean handleDisconnect(String userData) {
        return this.gameState.disconnectPlayer((userData));
    }

    private JSONObject secureGameState() {
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

    private int getPlayerNum(String userData) {
        System.out.println("TESTE1: " + this.gameState.getJSON().getString("userDataPlayer1"));
        System.out.println("TESTE2: " + this.gameState.getJSON().getString("userDataPlayer2"));
        if(userData.equals(this.gameState.getJSON().getString("userDataPlayer1"))) return 1;
        if(userData.equals(this.gameState.getJSON().getString("userDataPlayer2"))) return 2;
        return 0;
    }

    private boolean checkPlayerTurn(String userData) {
        if(this.gameState.getJSON().getInt("turn") == getPlayerNum(userData)) return true;
        return false;
    }

    private boolean checkEmptySpace(int pos) {
        if(this.gameState.getBoardJSON().getInt(pos) != 0) return false;
        return true;
    }

    private int nextTurn() {
        if(this.gameState.getJSON().getInt("turn") == 1) return 2;
        if(this.gameState.getJSON().getInt("turn") == 2) return 1;
        return 0;
    }

    private boolean isGameOver(int X_or_O) {
        int[] gameArray = GameBoard.board;

        if (checkHorizontal(X_or_O, gameArray)) return true;
        if (checkVertical(X_or_O, gameArray)) return true;
        if (checkCrossed(X_or_O, gameArray)) return true;

        return false;
    }

    private boolean checkVertical (int X_or_O, int[] array) {
        for (int i = 0; i < 3; i++) {
            if (array[i] == X_or_O && array[i+3] == X_or_O && array[i+6] == X_or_O) return true;
        }
        return false;
    }

    private boolean checkHorizontal (int X_or_O, int[] array) {
        for (int i = 0; i < 9; i = i + 3) {
            if (array[i] == X_or_O && array[i+1] == X_or_O && array[i+2] == X_or_O) return true;
        }
        return false;
    }

    private boolean checkCrossed (int X_or_O, int[] array) {
        if (array[0] == X_or_O && array[4] == X_or_O && array[8] == X_or_O) return true;
        if (array[2] == X_or_O && array[4] == X_or_O && array[6] == X_or_O) return true;

        return false;
    }

    private boolean noZeroes () {
        int[] gameArray = GameBoard.board;

        for (int i = 0; i < 9; i++) {
            if (gameArray[i] == 0) {
                return false;
            }
        }
        return true;
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

            switch(action) {
                case 3:
                    System.out.println("(ClientId: "+ clientId +")Acao = pedir para entrar no jogo");
                    if(!handleNewPlayer(clientId, userData, name)) {
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
                    JSONArray board = this.gameState.getBoardJSON();
                    if(this.gameState.getJSON().getInt("status") != 1) {
                        response.put("action", action);
                        response.put("message", "O jogo nao esta em andamento!");
                        return (response.toString()).getBytes();
                    }
                    if(!checkPlayerTurn(userData)) {
                        response.put("action", action);
                        response.put("message", "Nao e a sua vez de jogar!");
                        return (response.toString()).getBytes();
                    }
                    int pos = requestObj.getInt("pos");
                    if(!checkEmptySpace(pos)) {
                        response.put("action", action);
                        response.put("message", "Essa posicao ja foi marcada!");
                        return (response.toString()).getBytes();
                    }
                    int playerNum = getPlayerNum(userData);
                    this.gameState.markPosition(pos, playerNum);
                    if(isGameOver(playerNum)){
                        if(playerNum == 1) {
                            response.put("action", action);
                            response.put("message", "O jogador 1 venceu!");
                            this.gameState.setStatus(3);
                        } else if(playerNum == 2) {
                            response.put("action", action);
                            response.put("message", "O jogador 2 venceu!");
                            this.gameState.setStatus(4);
                        }
                        return (response.toString()).getBytes();
                    }
                    if (!isGameOver(playerNum) && noZeroes()) {
                        response.put("action", action);
                        response.put("message", "O jogo empatou!");
                        this.gameState.setStatus(2);
                    }
                    this.gameState.setTurn(nextTurn());
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
            System.out.println("Mensagem nao reconhecida: " + e);
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
