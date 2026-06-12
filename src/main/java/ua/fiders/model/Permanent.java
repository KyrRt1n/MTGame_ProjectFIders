package ua.fiders.model;

import ua.fiders.model.cards.*;
import ua.fiders.model.enums.CardKeywords;

public class Permanent {

    private final Card baseCard;
    private Player controller;
    private boolean isTapped;

    private int damageTaken;
    private int permanentDamageBuff = 0, permanentHpBuff = 0;
    private int eotDamageBuff = 0, eotHpBuff = 0; //end of turn

    public Permanent(Card baseCard, Player controller) {
        this.baseCard = baseCard;
        this.controller = controller;
        this.isTapped = true;
        this.damageTaken = 0;
        this.permanentDamageBuff = 0;
        this.permanentHpBuff = 0;
        this.eotDamageBuff = 0;
        this.eotHpBuff = 0;
    }

    public void tap() {
        isTapped = true;
    }

    public void untap() {
        isTapped = false;
    }

    public boolean isTapped() {
        return isTapped;
    }

    public boolean hasKeyword(CardKeywords keyword){
        return baseCard.getKeywords().contains(keyword);
    }

    public Card getBaseCard() {
        return baseCard;
    }

    public Player getController() {
        return controller;
    }

    public int getCurrentAttack() {
        if (baseCard instanceof CreatureCard card)
            return card.getAttack() + permanentDamageBuff + eotDamageBuff;
        return 0;
    }

    public int getMaxHp() {
        if (baseCard instanceof CreatureCard card)
            return card.getHp() + permanentHpBuff + eotHpBuff;
        return 0;
    }

    public void addBuff(int attack, int hp, boolean isPermanent) {
        if (isPermanent) {
            permanentDamageBuff += attack;
            permanentHpBuff += hp;
        } else {
            eotDamageBuff += attack;
            eotHpBuff += hp;
        }
    }

    public void clearEndOfTurnEffects() {
        eotDamageBuff = 0;
        eotHpBuff = 0;
    }

    public int getRemainingHp(){
        return getMaxHp() - damageTaken;
    }

    public void takeDamage(int amount) {
        damageTaken += amount;
    }

    public int getDamageTaken() {
        return damageTaken;
    }

    public boolean isDead() {
        return damageTaken >= getMaxHp();
    }

    public void clearDamage() {
        damageTaken = 0;
    }
}
