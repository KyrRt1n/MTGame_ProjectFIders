package ua.fiders.model.cards;

import java.util.Set;

public class CreatureCard extends Card{
    private int attack;
    private int hp;

    public CreatureCard(String name, int manaCost, Set<CardKeywords> keywords, int attack, int hp) {
        super(name, Type.Creature, manaCost, keywords);
        this.attack = attack;
        this.hp = hp;
    }

    public int getAttack() {
        return attack;
    }

    public void setAttack(int attack) {
        this.attack = attack;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }
}
