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
    private int currentMana = 0;

    public Player(String name) {
        this.name = name;
    }

    public void spendMana(int manaCost){
        this.currentMana -= manaCost;
    }

    public void refillMana(){
        this.currentMana = maxMana;
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

    public int getCurrentMana() {
        return currentMana;
    }

    public void setCurrentMana(int currentMana) {
        this.currentMana = currentMana;
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
