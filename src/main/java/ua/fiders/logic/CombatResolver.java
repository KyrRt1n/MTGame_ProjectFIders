package ua.fiders.logic;

import ua.fiders.model.GameState;
import ua.fiders.model.Permanent;
import ua.fiders.model.Player;
import ua.fiders.model.cards.CreatureCard;
import ua.fiders.model.enums.CardKeywords;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CombatResolver {

    public void resolveCombat(List<Permanent> attackers,
                              Map<Permanent, Permanent> blocks,
                              GameState state) {
        Map<Permanent, List<Permanent>> multi = new LinkedHashMap<>();
        for (Map.Entry<Permanent, Permanent> entry : blocks.entrySet()) {
            List<Permanent> single = new ArrayList<>();
            single.add(entry.getValue());
            multi.put(entry.getKey(), single);
        }
        resolveCombatMulti(attackers, multi, state);
    }

    public void resolveCombatMulti(List<Permanent> attackers,
                                   Map<Permanent, List<Permanent>> blocks,
                                   GameState state) {

        Player attackingPlayer = state.getCurrentPlayer();
        Player defendingPlayer = (attackingPlayer == state.getPlayer1())
                ? state.getPlayer2()
                : state.getPlayer1();

        for (Permanent attacker : attackers) {
            if (!isCreature(attacker)) {
                continue;
            }

            List<Permanent> assigned = blocks.getOrDefault(attacker, List.of());
            List<Permanent> legalBlockers = new ArrayList<>();
            for (Permanent blocker : assigned) {
                if (canBlock(blocker, attacker, state)) {
                    legalBlockers.add(blocker);
                }
            }

            if (legalBlockers.isEmpty()) {
                resolveUnblocked(attacker, defendingPlayer);
            } else {
                resolveBlocked(attacker, legalBlockers, defendingPlayer, state);
            }
        }

        removeDeadCreatures(state);
    }

    public boolean canBlock(Permanent blocker, Permanent attacker, GameState state) {
        if (!attacker.hasEffectiveKeyword(CardKeywords.FLYING, state))
            return true;

        return blocker.hasEffectiveKeyword(CardKeywords.FLYING, state)
                || blocker.hasEffectiveKeyword(CardKeywords.REACH, state);
    }

    private void resolveUnblocked(Permanent attacker, Player defendingPlayer) {
        int power = attacker.getCurrentAttack();
        dealDamageToPlayer(defendingPlayer, power);
        applyLifelink(attacker, power);
    }

    private void resolveBlocked(Permanent attacker,
                                List<Permanent> blockers,
                                Player defendingPlayer,
                                GameState state) {
        int totalPower = attacker.getCurrentAttack();
        int unassigned = totalPower;

        for (Permanent blocker : blockers) {
            if (unassigned <= 0) break;
            int lethal = Math.min(unassigned, blocker.getRemainingHp());
            blocker.takeDamage(lethal);
            unassigned -= lethal;
        }

        if (attacker.hasEffectiveKeyword(CardKeywords.TRAMPLE, state) && unassigned > 0) {
            dealDamageToPlayer(defendingPlayer, unassigned);
        }

        int blockerTotalPower = 0;
        for (Permanent blocker : blockers) {
            blockerTotalPower += blocker.getCurrentAttack();
        }
        attacker.takeDamage(blockerTotalPower);

        applyLifelink(attacker, totalPower);
        for (Permanent blocker : blockers) {
            applyLifelink(blocker, blocker.getCurrentAttack());
        }
    }

    private void dealDamageToPlayer(Player player, int amount) {
        player.setHp(player.getHp() - amount);
    }

    private void applyLifelink(Permanent creature, int damageDealt) {
        if (creature.hasKeyword(CardKeywords.LIFELINK) && damageDealt > 0) {
            Player owner = creature.getController();
            owner.setHp(owner.getHp() + damageDealt);
        }
    }

    public void removeDeadCreatures(GameState state) {
        state.getBattlefield().removeIf(p -> {
            if (isCreature(p) && p.isDead()) {
                p.getController().getGraveyard().add(p.getBaseCard());
                return true;
            }
            return false;
        });
    }

    private boolean isCreature(Permanent permanent) {
        return permanent.getBaseCard() instanceof CreatureCard;
    }
}