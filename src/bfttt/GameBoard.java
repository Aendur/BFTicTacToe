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
import java.util.Arrays;


public class GameBoard {
    public int[] board = {0, 0, 0, 0, 0, 0, 0, 0, 0};

    // 0 = ongoing
    // 1 = p1 win
    // 2 = p2 win
    // 3 = tie
    public int status = 0;

    // 1 or 2
    public int turn = 0;
    public int idPlayer1 = -1;
    public int idPlayer2 = -1;
    public String namePlayer1 = "";
    public String namePlayer2 = "";
    public String userDataPlayer1 = "";
    public String userDataPlayer2 = "";

    public GameBoard() {}

    public boolean addNewPlayer(int clientId, String userData, String name) {
        if(this.idPlayer1 == -1) {
            this.reset();
            this.idPlayer1 = clientId;
            this.userDataPlayer1 = userData;
            this.namePlayer1 = name;
            return true;
        }
        if(this.idPlayer2 == -1) {
            this.reset();
            this.idPlayer1 = clientId;
            this.userDataPlayer1 = userData;
            this.namePlayer1 = name;
            this.status = 1;
            this.turn = 1;
            return true;
        }
        return false;
    }

    public boolean disconnectPlayer(String userData) {
        if(userData.equals(this.userDataPlayer1)) {
            this.idPlayer1 = -1;
            this.userDataPlayer1 = "";
            this.reset();
            return true;
        }
        if(userData.equals(this.userDataPlayer2)) {
            this.idPlayer2 = -1;
            this.userDataPlayer2 = "";
            this.reset();
            return true;
        }
        return false;
    }

    private void reset() {
        Arrays.fill(this.board, 0);
        this.status = 0;
        this.turn = 0;
    }

    public void markPosition(int position, int value) {
        this.board[position] = value;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public JSONObject getJSON() {
        JSONObject json = new JSONObject();
        JSONArray jsonboard = new JSONArray(board);
        json.put("board", jsonboard);
        json.put("status", this.status);
        json.put("turn", this.turn);
        json.put("idPlayer1", this.idPlayer1);
        json.put("idPlayer2", this.idPlayer2);
        json.put("userDataPlayer1", this.userDataPlayer1);
        json.put("userDataPlayer2", this.userDataPlayer2);
        return json;
    }

    public JSONArray getBoardJSON() {
        JSONArray jsonboard = new JSONArray(board);
        return jsonboard;
    }
}
