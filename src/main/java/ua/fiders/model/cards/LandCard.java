package ua.fiders.model.cards;

import java.util.Set;

public class LandCard extends Card{

    public LandCard(String name, Set<CardKeywords> keywords) {
        super(name, Type.Land, 0, keywords);
    }
}
