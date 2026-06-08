package ua.fiders.logic;

import ua.fiders.model.*;
import ua.fiders.model.effects.*;

public class EffectExecutor {

    public void execute(CardEffect effect, Player caster, GameState state) {
        switch (effect) {
            case DrawCardEffect draw -> {
                System.out.println(caster.getName() + " бере " + draw.amount() + " карт!");
                for (int i = 0; i < draw.amount(); i++) {
                    // TODO: drawCard(); caster.getHand().add(caster.drawnCard());
                }
            }
            case HealPlayerEffect heal -> {
                caster.setHp(caster.getHp() + heal.amount());
                System.out.println(caster.getName() + " лікується на " + heal.amount() + ". ХП: " + caster.getHp());
            }
            case DamageEnemyEffect damage -> {
                Player enemy = (caster == state.getPlayer1()) ? state.getPlayer2() : state.getPlayer1();
                enemy.setHp(enemy.getHp() - damage.amount());
                System.out.println("Ворог отримує " + damage.amount() + " урону! ХП ворога: " + enemy.getHp());
            }
        }
    }
}