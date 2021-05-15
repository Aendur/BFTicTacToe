package bfttt;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;

public class GameBoard {
    public static int[] board = {0, 0, 0, 0, 0, 0, 0, 0, 0};

    // 0 = waiting
    // 1 = ongoing
    // 2 = tie
    // 3 = p1 win
    // 4 = p2 win
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
            this.idPlayer2 = clientId;
            this.userDataPlayer2 = userData;
            this.namePlayer2 = name;
            this.status = 1;
            this.turn = 1;
            return true;
        }
        return false;
    }

    public boolean disconnectPlayer(String userData) {
        if(userData.equals(this.userDataPlayer1)) {
            this.idPlayer1 = -1;
            this.namePlayer1 = "";
            this.userDataPlayer1 = "";
            this.reset();
            return true;
        }
        if(userData.equals(this.userDataPlayer2)) {
            this.idPlayer2 = -1;
            this.namePlayer2 = "";
            this.userDataPlayer2 = "";
            this.reset();
            return true;
        }
        return false;
    }

    private void reset() {
        Arrays.fill(board, 0);
        this.status = 0;
        this.turn = 0;
    }

    public void markPosition(int position, int value) {
        board[position] = value;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public JSONObject getJSON() {
        JSONObject json = new JSONObject();
        JSONArray jsonboard = new JSONArray(board);
        json.put("board", jsonboard);
        json.put("status", this.status);
        json.put("turn", this.turn);
        json.put("idPlayer1", this.idPlayer1);
        json.put("idPlayer2", this.idPlayer2);
        json.put("namePlayer1", this.namePlayer1);
        json.put("namePlayer2", this.namePlayer2);
        json.put("userDataPlayer1", this.userDataPlayer1);
        json.put("userDataPlayer2", this.userDataPlayer2);
        return json;
    }

    public JSONArray getBoardJSON() {
        return new JSONArray(board);
    }
}
