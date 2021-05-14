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
    public int turn = -1;
    public int idPlayer1 = -1;
    public int idPlayer2 = -1;
    public String userDataPlayer1 = "";
    public String userDataPlayer2 = "";

    public GameBoard(int id1, int id2) {
        this.idPlayer1 = id1;
        this.idPlayer2 = id2;
    }

    public GameBoard(int id1, int id2, String data1, String data2) {
        this.idPlayer1 = id1;
        this.idPlayer2 = id2;
        this.userDataPlayer1 = data1;
        this.userDataPlayer2 = data2;
    }

    public void reset() {
        //for (int i = 0; i < this.board.length; ++i) { this.board[i] = 0; }
        Arrays.fill(this.board, 0);
        this.status = 0;
        this.turn = -1;
        // this.idPlayer1 =
        // this.idPlayer2 =
        // this.userDataPlayer1 =
        // this.userDataPlayer2 =
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
}

