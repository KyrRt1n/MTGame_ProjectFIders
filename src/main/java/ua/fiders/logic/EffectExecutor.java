package ua.fiders.logic;

import ua.fiders.model.*;
import ua.fiders.model.cards.*;
import ua.fiders.model.effects.*;
import java.util.function.Consumer;

public class EffectExecutor {

    private final Consumer<String> logger;

    public EffectExecutor(Consumer<String> logger) {
        this.logger = logger;
    }

    public void execute(CardEffect effect, Player caster, GameState state){
        execute(effect, caster, state, null, null);
    }

    public void execute(CardEffect effect, Player caster, GameState state, Permanent target1, Permanent target2) {
        switch (effect) {
            case DrawCardEffect draw -> {
                logger.accept(caster.getName() + " бере " + draw.amount() + " карт!");
                for (int i = 0; i < draw.amount(); i++) {
                    Card drawn = caster.drawnCard();
                    if (drawn != null)
                        caster.getHand().add(drawn);
                }
            }
            case HealPlayerEffect heal -> {
                caster.setHp(caster.getHp() + heal.amount());
                logger.accept(caster.getName() + " лікується на " + heal.amount() + ". ХП: " + caster.getHp());
            }
            case DamageEnemyEffect damage -> {
                Player enemy = state.getOpponent(caster);
                enemy.setHp(enemy.getHp() - damage.amount());
                logger.accept("Ворог отримує " + damage.amount() + " урону! ХП ворога: " + enemy.getHp());
            }
            case BuffStatsEffect buff -> {
                if (target1 != null) {
                    target1.addBuff(buff.attackAmount(), buff.hpAmount(), buff.isPermanent());
                    logger.accept("Істота " + target1.getBaseCard().getName() + " отримує баф!");
                }
            }
            case DestroyTargetEffect destroy -> {
                if (target1 != null) {
                    if (destroy.requiredKeyword() == null || target1.hasKeyword(destroy.requiredKeyword())) {
                        target1.takeDamage(999);
                        logger.accept(target1.getBaseCard().getName() + " знищено!");
                    } else {
                        logger.accept("Немає потрібного ківорду. Спелл скасовано.");
                    }
                }
            }
            case BiteEffect bite -> {
                if (target1 != null && target2 != null) {
                    int damage = target1.getCurrentAttack();
                    target2.takeDamage(damage);
                    logger.accept(target1.getBaseCard().getName() + " кусає " + target2.getBaseCard().getName() + " на " + damage + " урону!");
                }
            }
            case DamageTargetEffect damageTarget -> {
                if (target1 != null) {
                    target1.takeDamage(damageTarget.amount());
                    System.out.println(target1.getBaseCard().getName() + " отримує " + damageTarget.amount() + " урону!");
                }
            }
        }
    }
}