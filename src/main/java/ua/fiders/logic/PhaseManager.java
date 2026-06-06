package ua.fiders.logic;

import ua.fiders.model.GameState;
import ua.fiders.model.Permanent;
import ua.fiders.model.Player;
import ua.fiders.model.cards.Card;
import ua.fiders.model.enums.Phase;

public class PhaseManager {

    private final GameState state;
    private Phase currentPhase;
    private int turnNumber;

    public PhaseManager(GameState state) {
        this.state = state;
        this.currentPhase = Phase.START;
        this.turnNumber = 1;
    }

    public void startGame() {
        runCurrentPhase();
    }

    public void advance() {
        if (currentPhase == Phase.END) {
            passTurnToOpponent();
        } else {
            currentPhase = currentPhase.next();
        }
        runCurrentPhase();
    }

    private void runCurrentPhase() {
        switch (currentPhase) {
            case START       -> onStart();
            case MAIN        -> onMain();
            case COMBAT      -> onCombat();
            case SECOND_MAIN -> onSecondMain();
            case END         -> onEnd();
        }
    }

    private void onStart() {
        Player active = state.getCurrentPlayer();

        for (Permanent p : state.getBattlefield()) {
            if (p.getController() == active) {
                p.untap();
            }
        }

        active.refillMana();

        Card drawn = active.drawnCard();
        if (drawn != null) {
            active.getHand().add(drawn);
        }
    }

    private void onMain() {
    }

    private void onCombat() {
    }

    private void onSecondMain() {
    }

    private void onEnd() {
    }

    private void passTurnToOpponent() {
        state.passTurn();
        currentPhase = Phase.START;
        turnNumber++;
    }


    public Phase getCurrentPhase() {
        return currentPhase;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public Player getActivePlayer() {
        return state.getCurrentPlayer();
    }
}