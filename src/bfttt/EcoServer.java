/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eco;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author eduardo
 */
public class EcoServer extends DefaultSingleRecoverable{

    //private int c = 0;
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
        initialState.put("status", "waitingForPLayers");
        initialState.put("turn", "player0");
        return initialState;
    }

    private void resetGameState() {
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
        initialState.put("status", "waitingForPLayers");
        initialState.put("turn", "player0");
        gameState = initialState;
    }

    public EcoServer(int id) {
        
        new ServiceReplica(id,this,this);
    }

    @Override
    public byte[] appExecuteOrdered(byte[] bytes, MessageContext mc) {
        String request = new String(bytes);
        String name = new String();

        System.out.println("Recebeu requisição: " + request);

        try {
            JSONObject resp = new JSONObject(new String(request));
            String clientId = resp.getString("clientId");
            switch(resp.getInt("action")) {
                case 0:
                    System.out.println("Acao = registrar nome e time");
                    name = resp.getString("name");
                    if(!gameState.has("player0")) gameState.put("player0", clientId);
                    else if(!gameState.has("player1")) gameState.put("player1", clientId);
                    else return ("dc").getBytes();
                    System.out.println("Tabuleiro: "+ gameState.getJSONArray("board"));
                    break;
                case 1:
                    System.out.println("Acao = marcar posicao");
                    name = resp.getString("name");
                    JSONArray board = gameState.getJSONArray("board");
                    int pos = resp.getInt("pos");
                    board.put(pos, 1);
                    System.out.println("Tabuleiro: "+ gameState.getJSONArray("board"));
                    break;
                case 2:
                    System.out.println("Acao = jogador desconectado");
                    System.out.println("ID:" + clientId);
                    resetGameState();
                case 3:
                    System.out.println("Acao = sincronizar");
                    break;
                default:
                    System.out.println("Acao nao reconhecida");
                    break;
            }
        } catch (JSONException e) {
            System.out.println("Erro: Nao foi possivel entender a pergunta do cliente");
        }

        //return ("Resposta servidor: " + request).getBytes();
        return (gameState.toString()).getBytes();
    }

    @Override
    public byte[] appExecuteUnordered(byte[] bytes, MessageContext mc) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public byte[] getSnapshot() {
        //return Integer.toString(c).getBytes();
        return gameState.toString().getBytes();
    }

    @Override
    public void installSnapshot(byte[] bytes) {
        //c = Integer.parseInt(new String(bytes));
        JSONObject gameState = new JSONObject();
        gameState.put("board", new int[] {0, 0, 0, 0, 0, 0, 0, 0 ,0});
        System.out.println("FDSAFDSA: "+gameState.toString());
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here

        new EcoServer(Integer.parseInt(args[0]));
        
    }
    
}
