package ua.fiders.model;

import java.util.*;

public class GameState {

    private final Player player1;
    private final Player player2;
    private Player currentPlayer;

    private final List<Permanent> battlefield;


    public GameState(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.currentPlayer = player1;
        this.battlefield = new ArrayList<>();
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public List<Permanent> getBattlefield() {
        return battlefield;
    }

    public void passTurn() {
        if (currentPlayer == player1) {
            currentPlayer = player2;
        } else {
            currentPlayer = player1;
        }
    }

    public Player getOpponent(Player player) {
        return (player == player1) ? player2 : player1;
    }
}