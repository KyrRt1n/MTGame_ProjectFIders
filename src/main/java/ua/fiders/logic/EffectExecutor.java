package ua.fiders.logic;

import ua.fiders.model.*;
import ua.fiders.model.cards.*;
import ua.fiders.model.effects.*;

public class EffectExecutor {

    public void execute(CardEffect effect, Player caster, GameState state){
        execute(effect, caster, state, null, null);
    }

    public void execute(CardEffect effect, Player caster, GameState state, Permanent target1, Permanent target2) {
        switch (effect) {
            case DrawCardEffect draw -> {
                for (int i = 0; i < draw.amount(); i++) {
                    Card drawn = caster.drawnCard();
                    if (drawn != null)
                        caster.getHand().add(drawn);
                }
            }
            case HealPlayerEffect heal -> caster.setHp(caster.getHp() + heal.amount());
            case DamageEnemyEffect damage -> state.getOpponent(caster).setHp(state.getOpponent(caster).getHp() - damage.amount());

            case BuffStatsEffect buff -> {
                if (target1 != null) {
                    target1.addBuff(buff.attackAmount(), buff.hpAmount(), buff.isPermanent());
                    System.out.println("Кріча " + target1.getBaseCard().getName() + " отримало баф!");
                }
            }
            case DestroyTargetEffect destroy -> {
                if (target1 != null) {
                    if (destroy.requiredKeyword() == null || target1.hasKeyword(destroy.requiredKeyword())) {
                        target1.takeDamage(999);
                        System.out.println(target1.getBaseCard().getName() + " знищено!");
                    } else {
                        System.out.println("Нема потрібного ківорду. Спелл скасовано.");
                    }
                }
            }
            case BiteEffect bite -> {
                //target1 damages target2
                if (target1 != null && target2 != null) {
                    int damage = target1.getCurrentAttack();
                    target2.takeDamage(damage);
                    System.out.println(target1.getBaseCard().getName() + " кусає " + target2.getBaseCard().getName() + " на " + damage + " урону!");
                }
            }
        }
    }
}