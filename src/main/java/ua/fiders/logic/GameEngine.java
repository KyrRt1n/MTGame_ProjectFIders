package ua.fiders.logic;

import ua.fiders.model.GameState;
import ua.fiders.model.Permanent;
import ua.fiders.model.Player;
import ua.fiders.model.cards.Card;
import ua.fiders.model.cards.CreatureCard;
import ua.fiders.model.cards.SpellCard;
import ua.fiders.model.effects.CardEffect;
import ua.fiders.model.enums.Phase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GameEngine {

    private final GameState state;
    private final PhaseManager phaseManager;
    private final CombatResolver combatResolver;

    private GameListener listener;
    private boolean landPlayedThisTurn;
    private boolean gameOver;

    private final Set<Permanent> declaredAttackers = new LinkedHashSet<>();
    private final Map<Permanent, Permanent> declaredBlocks = new LinkedHashMap<>();

    private final EffectExecutor effectExecutor = new EffectExecutor();

    public GameEngine(Player player1, Player player2) {
        this.state = new GameState(player1, player2);
        this.phaseManager = new PhaseManager(this, state);
        this.combatResolver = new CombatResolver();
        this.landPlayedThisTurn = false;
        this.gameOver = false;
    }

    public void start() {
        phaseManager.startGame();
        notifyTurnChanged();
    }

    public void nextPhase() {
        if (gameOver) {
            return;
        }
        clearCombatSelections();
        Player before = state.getCurrentPlayer();
        phaseManager.advance();
        if (state.getCurrentPlayer() != before) {
            landPlayedThisTurn = false;
            notifyTurnChanged();
        }
        notifyManaChanged(state.getCurrentPlayer());
    }

    public boolean playCard(Card card) {
        if (gameOver) {
            return false;
        }
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

        if (card instanceof SpellCard spell) {
            System.out.println(player.getName() + " чаклує: " + spell.getName());
            for (CardEffect effect : spell.getEffects()) {
                effectExecutor.execute(effect, player, state);
            }
        }

        player.getGraveyard().add(card);

        notifyManaChanged(player);
        notifyHpChanged(state.getOpponent(state.getCurrentPlayer()));
        checkGameOver();
        return true;
    }

    private void addPermanent(Card card, Player controller) {
        Permanent permanent = new Permanent(card, controller);
        state.getBattlefield().add(permanent);
        if (listener != null) {
            listener.onPermanentEnteredBattlefield(permanent);
        }
    }

    public boolean canDeclareAttacker(Permanent permanent) {
        return !gameOver
                && getCurrentPhase() == Phase.COMBAT
                && permanent.getBaseCard() instanceof CreatureCard
                && permanent.getController() == state.getCurrentPlayer()
                && !permanent.isTapped()
                && state.getBattlefield().contains(permanent);
    }

    public boolean toggleAttacker(Permanent permanent) {
        if (declaredAttackers.remove(permanent)) {
            declaredBlocks.remove(permanent);
            return false;
        }
        if (canDeclareAttacker(permanent)) {
            declaredAttackers.add(permanent);
            return true;
        }
        return false;
    }

    public boolean canDeclareBlocker(Permanent attacker, Permanent blocker) {
        return !gameOver
                && getCurrentPhase() == Phase.COMBAT
                && declaredAttackers.contains(attacker)
                && blocker.getBaseCard() instanceof CreatureCard
                && blocker.getController() != state.getCurrentPlayer()
                && !blocker.isTapped()
                && !declaredBlocks.containsValue(blocker)
                && state.getBattlefield().contains(blocker)
                && combatResolver.canBlock(blocker, attacker);
    }

    public boolean assignBlocker(Permanent attacker, Permanent blocker) {
        if (!canDeclareBlocker(attacker, blocker)) {
            return false;
        }
        declaredBlocks.put(attacker, blocker);
        return true;
    }

    public void removeBlocker(Permanent attacker) {
        declaredBlocks.remove(attacker);
    }

    public Set<Permanent> getDeclaredAttackers() {
        return Collections.unmodifiableSet(declaredAttackers);
    }

    public Map<Permanent, Permanent> getDeclaredBlocks() {
        return Collections.unmodifiableMap(declaredBlocks);
    }

    public void executeCombat() {
        if (gameOver || getCurrentPhase() != Phase.COMBAT || declaredAttackers.isEmpty()) {
            return;
        }
        for (Permanent attacker : declaredAttackers) {
            attacker.tap();
        }
        resolveCombat(new ArrayList<>(declaredAttackers), new LinkedHashMap<>(declaredBlocks));
        clearCombatSelections();
    }

    private void clearCombatSelections() {
        declaredAttackers.clear();
        declaredBlocks.clear();
    }

    public void resolveCombat(List<Permanent> attackers, Map<Permanent, Permanent> blocks) {
        if (gameOver) {
            return;
        }
        combatResolver.resolveCombat(attackers, blocks, state);
        notifyHpChanged(state.getPlayer1());
        notifyHpChanged(state.getPlayer2());
        checkGameOver();
    }

    private void checkGameOver() {
        if (gameOver) {
            return;
        }
        Player winner = getWinner();
        if (winner != null) {
            declareWinner(winner);
        }
    }

    void declareWinner(Player winner) {
        if (gameOver) {
            return;
        }
        gameOver = true;
        if (listener != null) {
            listener.onGameOver(winner);
        }
    }

    public boolean isGameOver() {
        return gameOver;
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

    private void notifyHpChanged(Player player) {
        if (listener != null) {
            listener.onHpChanged(player);
        }
    }

    public void notifyHandUpdated(Player player) {
        if (listener != null) {
            listener.onHandUpdated(player);
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