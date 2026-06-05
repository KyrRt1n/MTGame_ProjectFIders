package ua.fiders.model;

import ua.fiders.model.cards.Card;

import java.util.ArrayList;
import java.util.List;

public class Player {

    private String name;
    private int hp = 20;
    private List<Card> hand = new ArrayList<>();
    private List<Card> deck = new ArrayList<>();
    private int maxMana = 0;
    private int currMana = 0;

    public Player(String name) {
        this.name = name;
    }

    private void spendMana(int manaCost){
        this.currMana -= manaCost;
    }

    private void refillMana(){
        this.currMana = maxMana;
    }

    public Card drawnCard() {
        if(deck.isEmpty())
            return null;

        return deck.removeLast();
    }


    public int getMaxMana() {
        return maxMana;
    }

    public void setMaxMana(int maxMana) {
        this.maxMana = maxMana;
    }

    public int getCurrMana() {
        return currMana;
    }

    public void setCurrMana(int currMana) {
        this.currMana = currMana;
    }

    public List<Card> getDeck() {
        return deck;
    }

    public void setDeck(List<Card> deck) {
        this.deck = deck;
    }

    public List<Card> getHand() {
        return hand;
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
