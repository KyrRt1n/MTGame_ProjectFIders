package ua.fiders.logic;

import ua.fiders.model.GameState;
import ua.fiders.model.Permanent;
import ua.fiders.model.Player;
import ua.fiders.model.cards.CreatureCard;
import ua.fiders.model.enums.CardKeywords;

import java.util.List;
import java.util.Map;

public class CombatResolver {

    public void resolveCombat(List<Permanent> attackers,
                              Map<Permanent, Permanent> blocks,
                              GameState state) {

        Player attackingPlayer = state.getCurrentPlayer();
        Player defendingPlayer = (attackingPlayer == state.getPlayer1())
                ? state.getPlayer2()
                : state.getPlayer1();

        for (Permanent attacker : attackers) {
            if (!isCreature(attacker)) {
                continue;
            }

            Permanent blocker = blocks.get(attacker);

            if (blocker != null && !canBlock(blocker, attacker)) {
                blocker = null;
            }

            if (blocker == null) {
                resolveUnblocked(attacker, defendingPlayer);
            } else {
                resolveBlocked(attacker, blocker, defendingPlayer);
            }
        }

        removeDeadCreatures(state);
    }

    public boolean canBlock(Permanent blocker, Permanent attacker) {
        if (!attacker.hasKeyword(CardKeywords.FLYING)) {
            return true;
        }
        return blocker.hasKeyword(CardKeywords.FLYING)
                || blocker.hasKeyword(CardKeywords.REACH);
    }

    private void resolveUnblocked(Permanent attacker, Player defendingPlayer) {
        int power = attacker.getCurrentAttack();
        dealDamageToPlayer(defendingPlayer, power);
        applyLifelink(attacker, power);
    }

    private void resolveBlocked(Permanent attacker, Permanent blocker, Player defendingPlayer) {
        int attackerPower = attacker.getCurrentAttack();
        int blockerPower  = blocker.getCurrentAttack();

        int blockerHpBefore = blocker.getRemainingHp();

        blocker.takeDamage(attackerPower);

        if (attacker.hasKeyword(CardKeywords.TRAMPLE) && attackerPower > blockerHpBefore) {
            int excess = attackerPower - blockerHpBefore;
            dealDamageToPlayer(defendingPlayer, excess);
        }

        attacker.takeDamage(blockerPower);

        applyLifelink(attacker, attackerPower);
        applyLifelink(blocker, blockerPower);
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