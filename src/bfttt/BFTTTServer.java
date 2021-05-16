package bfttt;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.Map;

public class BFTTTServer extends DefaultSingleRecoverable{
    private final int id;
    private int lastClientId;
    private final String secret;
    private final String dbPath;
    private GameBoard gameState; // = new GameBoard();

    public BFTTTServer(int id) {
        this.id = id;
        this.secret = "4f89321u89fj2398f1h432fjr093yfh19823hf";
        this.lastClientId = 1;
        this.dbPath = "serverdata" + id + ".json";

        // load ongoing games to server
        // this.loadDBFile();

        // initialize game board
        this.gameState = new GameBoard();

        // initialize BFT
        new ServiceReplica(this.id,this,this);
    }

    private String createToken(String name) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(this.secret);
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

    private String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(this.secret);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("auth0")
                    .build();
            DecodedJWT jwt = verifier.verify(token);
            return jwt.toString();
        } catch (JWTVerificationException | UnsupportedEncodingException exception){
            //Invalid signature/claims
            return null;
        }
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
        return name.asString();
    }

    private boolean handleNewPlayer(int clientId, String userData, String name) {
        if(this.gameState.addNewPlayer(clientId, userData, name)) {
            this.lastClientId++;
            return true;
        }
        return false;
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
                e.printStackTrace();
            }
        }
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

    private void handleDisconnect(String userData) {
        this.gameState.disconnectPlayer((userData));
    }

    private JSONObject makeDisconnectResponse(String reason) {
        System.out.println("Jogador desconectado. Motivo: " + reason);
        JSONObject response = new JSONObject();
        response.put("action", 4);
        response.put("message", reason);
        return response;
    }

    @Override
    public byte[] appExecuteOrdered(byte[] bytes, MessageContext mc) {
        String request = new String(bytes);
        JSONObject requestObj;
        JSONObject decodedRequest;
        JSONObject response = new JSONObject();

        String userData;
        String rawMessage;
        int action;
        String token;
        String name;
        int clientId;

        try {
            requestObj = new JSONObject(request);
            userData = requestObj.getString("userData");
            rawMessage = requestObj.getString("rawMessage");
        } catch (JSONException jsonException) {
            handleDisconnect(null);
            response = makeDisconnectResponse("Nao foi possivel decodificar a requisicao do proxy");
            return response.toString().getBytes();
        }

        if(rawMessage.equals("\u0003�")) {
            handleDisconnect(userData);
            response = makeDisconnectResponse("Cliente desconectou de uma forma inesperada");
            return response.toString().getBytes();
        }

        try {
            decodedRequest = new JSONObject(rawMessage);
        } catch (JSONException jsonException) {
            response.put("action", 1);
            response.put("message", "Requisicao nao reconhecida");
            return response.toString().getBytes();
        }

        if(decodedRequest.has("action")) action = decodedRequest.getInt("action");
        else {
            action = 1;
        }

        if(action == 2) {
            response.put("action", 2);
            response.put("message", "Token de acesso recebido");
            response.put("token", createToken(decodedRequest.getString("name")));
            return response.toString().getBytes();
        }

        try {
            token = decodedRequest.getString("token");
            if(validateToken(token) == null) {
                handleDisconnect(userData);
                response = makeDisconnectResponse("Token invalido");
                return response.toString().getBytes();
            }
            name = getClientName(token);
            clientId = getClientId(token);
        } catch (JSONException jsonException) {
            handleDisconnect(userData);
            response = makeDisconnectResponse("Token nao encontrado");
            return response.toString().getBytes();
        }

        if(decodedRequest.getInt("action") == 4) {
            handleDisconnect(userData);
            response = makeDisconnectResponse("Cliente solicitou a desconexao");
            return response.toString().getBytes();
        }

        // Here all requests have a valid token and userData
        switch(action) {
            case 3:
                System.out.println("(ClientId: "+ clientId +")Acao = pedir para entrar no jogo");
                if(!handleNewPlayer(clientId, userData, name)) {
                    response = makeDisconnectResponse("Ja existe um jogo em andamento");
                    return response.toString().getBytes();
                }
                System.out.println("Jogador " + name + "(ClientId: " + clientId + ")" + " entrou no jogo");
                break;
            case 5:
                System.out.println("(ClientId: "+ clientId +")Acao = marcar posicao");
                if(this.gameState.getStatus() != 1) {
                    response.put("action", 5);
                    response.put("message", "O jogo nao esta em andamento!");
                    return response.toString().getBytes();
                }
                if(!this.gameState.checkPlayerTurn(userData)) {
                    response.put("action", 5);
                    response.put("message", "Nao e a sua vez de jogar!");
                    return response.toString().getBytes();
                }
                int pos = decodedRequest.getInt("pos");
                if(!this.gameState.checkEmptySpace(pos)) {
                    response.put("action", 5);
                    response.put("message", "Essa posicao ja foi marcada!");
                    return response.toString().getBytes();
                }
                int playerNum = this.gameState.getPlayerNum(userData);
                this.gameState.markPosition(pos, playerNum);
                if(this.gameState.isGameOver(playerNum)){
                    if(playerNum == 1) {
                        response.put("message", "O jogador 1 venceu!");
                        this.gameState.setStatus(3);
                    } else if(playerNum == 2) {
                        response.put("message", "O jogador 2 venceu!");
                        this.gameState.setStatus(4);
                    }
                    response.put("action", 5);
                    return response.toString().getBytes();
                }
                if (!this.gameState.isGameOver(playerNum) && noZeroes()) {
                    response.put("message", "O jogo empatou!");
                    this.gameState.setStatus(2);
                }
                this.gameState.setNextTurn();
                break;
            case 6:
                //System.out.println("(ClientId: "+ clientId +")Acao = sincronizar");
                break;
            default:
                System.out.println("(ClientId: "+ clientId +")Acao nao reconhecida");
                break;
        }
        response.put("action", action);
        response.put("gameState", this.gameState.secureGameState());
        return response.toString().getBytes();
    }

    @Override
    public byte[] appExecuteUnordered(byte[] bytes, MessageContext mc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public byte[] getSnapshot() {
        System.out.println("getSnapshot: " + this.gameState.getJSON().toString());
        return this.gameState.getJSON().toString().getBytes();
    }

    @Override
    public void installSnapshot(byte[] bytes) {
        JSONObject snap = new JSONObject(bytes);
        System.out.println("installSnapshot: " + snap);
        this.gameState = new GameBoard(snap);
    }

    public static void main(String[] args) {
        new BFTTTServer(Integer.parseInt(args[0]));
    }
}
