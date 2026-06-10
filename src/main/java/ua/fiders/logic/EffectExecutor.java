package ua.fiders.logic;

import ua.fiders.model.*;
import ua.fiders.model.cards.*;
import ua.fiders.model.effects.*;

public class EffectExecutor {

    public void execute(CardEffect effect, Player caster, GameState state) {
        switch (effect) {
            case DrawCardEffect draw -> {
                System.out.println(caster.getName() + " бере " + draw.amount() + " карт!");
                for (int i = 0; i < draw.amount(); i++) {
                    Card drawn = caster.drawnCard();
                    if (drawn == null) break;
                    caster.getHand().add(drawn);
                }
            }
            case HealPlayerEffect heal -> {
                caster.setHp(caster.getHp() + heal.amount());
                System.out.println(caster.getName() + " лікується на " + heal.amount() + ". ХП: " + caster.getHp());
            }
            case DamageEnemyEffect damage -> {
                Player enemy = state.getOpponent(caster);
                enemy.setHp(enemy.getHp() - damage.amount());
                System.out.println("Ворог отримує " + damage.amount() + " урону! ХП ворога: " + enemy.getHp());
            }
        }
    }
}