package ua.fiders.model;

import ua.fiders.model.cards.Card;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private String name;
    private int hp = 20;
    private List<Card> hand = new ArrayList<>();
    private List<Card> deck = new ArrayList<>();

    public Player(String name) {
        this.name = name;
    }

    public Card drawnCard() {
        if(deck.isEmpty())
            return null;

        return deck.removeLast();
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
