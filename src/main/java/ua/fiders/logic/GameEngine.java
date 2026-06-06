package ua.fiders.logic;

import ua.fiders.model.*;
import ua.fiders.model.cards.*;

public class GameEngine {

    private final GameState gameState;
    private GameListener listener;

    public GameEngine(GameState gameState) {
        this.gameState = gameState;
    }

    public void setListener(GameListener listener) {
        this.listener = listener;
    }

    public boolean playCardFromHand(Player player, Card card) {
        if (gameState.getCurrentPlayer() != player) {
            System.out.println("Зараз не ваш ход!");
            return false;
        }

        if (!player.getHand().contains(card)) return false;


        if (player.getCurrentMana() < card.getManaCost()) {
            System.out.println("Недостатньо мани!");
            return false;
        }

        player.spendMana(card.getManaCost());

        if (listener != null)
            listener.onManaChanged(player);

        player.getHand().remove(card);

        if (card.getType() == Type.Creature || card.getType() == Type.Land) {
            Permanent permanent = new Permanent(card, player);
            gameState.getBattlefield().add(permanent);

            System.out.println(player.getName() + " розіграв " + card.getName());

            if (listener != null)
                listener.onPermanentEnteredBattlefield(permanent);

        } else if (card.getType() == Type.Sorcery) {
            System.out.println(player.getName() + " кастує закляття " + card.getName());
            // TODO: EffectExecutor
        }

        return true;
    }

    public void passTurn() {
        gameState.passTurn();
        if (listener != null)
            listener.onTurnChanged(gameState.getCurrentPlayer());
        System.out.println("Хід передано. Нинішній гравець: " + gameState.getCurrentPlayer().getName());
    }
}