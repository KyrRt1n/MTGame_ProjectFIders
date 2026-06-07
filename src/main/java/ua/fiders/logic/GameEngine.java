package ua.fiders.logic;

import ua.fiders.model.GameState;
import ua.fiders.model.Permanent;
import ua.fiders.model.Player;
import ua.fiders.model.cards.Card;
import ua.fiders.model.enums.Phase;

import java.util.List;
import java.util.Map;

public class GameEngine {

    private final GameState state;
    private final PhaseManager phaseManager;
    private final CombatResolver combatResolver;

    private GameListener listener;
    private boolean landPlayedThisTurn;

    public GameEngine(Player player1, Player player2) {
        this.state = new GameState(player1, player2);
        this.phaseManager = new PhaseManager(state);
        this.combatResolver = new CombatResolver();
        this.landPlayedThisTurn = false;
    }

    public void start() {
        phaseManager.startGame();
        notifyTurnChanged();
    }

    public void nextPhase() {
        Player before = state.getCurrentPlayer();
        phaseManager.advance();
        if (state.getCurrentPlayer() != before) {
            landPlayedThisTurn = false;
            notifyTurnChanged();
        }
        notifyManaChanged(state.getCurrentPlayer());
    }

    public boolean playCard(Card card) {
        Player active = state.getCurrentPlayer();
        if (!active.getHand().contains(card)) {
            return false;
        }

        return switch (card.getType()) {
            case Land     -> playLand(active, card);
            case Creature -> playCreature(active, card);
            case Sorcery  -> playSorcery(active, card);
        };
    }

    private boolean playLand(Player player, Card card) {
        if (landPlayedThisTurn) {
            return false;
        }

        player.setMaxMana(player.getMaxMana() + 1);
        player.setCurrentMana(player.getCurrentMana() + 1);

        player.getHand().remove(card);
        addPermanent(card, player);
        landPlayedThisTurn = true;
        notifyManaChanged(player);
        return true;
    }

    private boolean playCreature(Player player, Card card) {
        if (player.getCurrentMana() < card.getManaCost()) {
            return false;
        }
        player.spendMana(card.getManaCost());
        player.getHand().remove(card);
        addPermanent(card, player);
        notifyManaChanged(player);
        return true;
    }

    private boolean playSorcery(Player player, Card card) {
        if (player.getCurrentMana() < card.getManaCost()) {
            return false;
        }
        player.spendMana(card.getManaCost());
        player.getHand().remove(card);
        notifyManaChanged(player);
        return true;
    }

    private void addPermanent(Card card, Player controller) {
        Permanent permanent = new Permanent(card, controller);
        state.getBattlefield().add(permanent);
        if (listener != null) {
            listener.onPermanentEnteredBattlefield(permanent);
        }
    }

    public void resolveCombat(List<Permanent> attackers, Map<Permanent, Permanent> blocks) {
        combatResolver.resolveCombat(attackers, blocks, state);
    }

    public boolean isGameOver() {
        return state.getPlayer1().getHp() <= 0 || state.getPlayer2().getHp() <= 0;
    }

    public Player getWinner() {
        if (state.getPlayer1().getHp() <= 0) return state.getPlayer2();
        if (state.getPlayer2().getHp() <= 0) return state.getPlayer1();
        return null;
    }

    public void setListener(GameListener listener) {
        this.listener = listener;
    }

    private void notifyTurnChanged() {
        if (listener != null) {
            listener.onTurnChanged(state.getCurrentPlayer());
        }
    }

    private void notifyManaChanged(Player player) {
        if (listener != null) {
            listener.onManaChanged(player);
        }
    }

    public GameState getState() {
        return state;
    }

    public Phase getCurrentPhase() {
        return phaseManager.getCurrentPhase();
    }

    public Player getCurrentPlayer() {
        return state.getCurrentPlayer();
    }
}