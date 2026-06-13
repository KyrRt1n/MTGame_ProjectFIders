package ua.fiders.logic;

import ua.fiders.model.GameState;
import ua.fiders.model.Permanent;
import ua.fiders.model.Player;
import ua.fiders.model.cards.Card;
import ua.fiders.model.cards.CreatureCard;
import ua.fiders.model.cards.SpellCard;
import ua.fiders.model.effects.CardEffect;
import ua.fiders.model.enums.CardKeywords;
import ua.fiders.model.enums.Phase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
    private final Set<Permanent> summoningSick = new HashSet<>();

    private final EffectExecutor effectExecutor;

    public GameEngine(Player player1, Player player2) {
        this.state = new GameState(player1, player2);
        this.phaseManager = new PhaseManager(this, state);
        this.combatResolver = new CombatResolver();
        this.landPlayedThisTurn = false;
        this.gameOver = false;
        this.effectExecutor = new EffectExecutor(this::logMessage);
    }

    public void start() {
        phaseManager.startGame();
        clearSummoningSicknessFor(state.getCurrentPlayer());
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
            clearSummoningSicknessFor(state.getCurrentPlayer());
            notifyTurnChanged();
        }
        notifyManaChanged(state.getCurrentPlayer());
    }

    public boolean playCard(Card card) {
        return playCard(card, null, null);
    }

    public boolean playCard(Card card, Permanent target1, Permanent target2) {
        if (gameOver) return false;

        Player active = state.getCurrentPlayer();
        if (!active.getHand().contains(card)) return false;

        return switch (card.getType()) {
            case Land     -> playLand(active, card);
            case Creature -> playCreature(active, card);
            case Sorcery  -> playSorcery(active, card, target1, target2);
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

    private boolean playSorcery(Player player, Card card, Permanent target1, Permanent target2) {
        if (player.getCurrentMana() < card.getManaCost()) return false;

        if (card instanceof SpellCard spell) {
            player.spendMana(card.getManaCost());
            player.getHand().remove(card);

            logMessage(player.getName() + " чаклує: " + spell.getName());

            for (CardEffect effect : spell.getEffects()) {
                effectExecutor.execute(effect, player, state, target1, target2);
            }
            player.getGraveyard().add(card);

            combatResolver.removeDeadCreatures(state);

            notifyManaChanged(player);
            notifyHpChanged(state.getOpponent(state.getCurrentPlayer()));
            checkGameOver();
            return true;
        }
        return false;
    }

    private void addPermanent(Card card, Player controller) {
        Permanent permanent = new Permanent(card, controller);
        permanent.untap();
        summoningSick.add(permanent);
        state.getBattlefield().add(permanent);
        if (listener != null) {
            listener.onPermanentEnteredBattlefield(permanent);
        }
    }

    private void clearSummoningSicknessFor(Player player) {
        summoningSick.removeIf(p -> p.getController() == player);
    }

    public boolean canDeclareAttacker(Permanent permanent) {
        return !gameOver
                && getCurrentPhase() == Phase.COMBAT
                && permanent.getBaseCard() instanceof CreatureCard
                && permanent.getController() == state.getCurrentPlayer()
                && !permanent.isTapped()
                && !summoningSick.contains(permanent)
                && state.getBattlefield().contains(permanent)
                && !permanent.hasEffectiveKeyword(CardKeywords.DEFENDER, state);
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
                && combatResolver.canBlock(blocker, attacker, state);
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

    public void logMessage(String msg) {
        System.out.println(msg);
        if (listener != null) {
            listener.onMessage(msg);
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