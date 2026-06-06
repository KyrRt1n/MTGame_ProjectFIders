package ua.fiders.model;

import ua.fiders.model.cards.*;
import ua.fiders.model.enums.CardKeywords;

public class Permanent {

    private final Card baseCard;
    private Player controller;
    private boolean isTapped;

    private int damageTaken;
    private int tempDamageBuff, tempHpBuff;

    public Permanent(Card baseCard, Player controller) {
        this.baseCard = baseCard;
        this.controller = controller;
        this.isTapped = true;
        this.damageTaken = 0;
        this.tempDamageBuff = 0;
        this.tempHpBuff = 0;
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
}
