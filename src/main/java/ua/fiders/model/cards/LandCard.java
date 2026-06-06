package ua.fiders.model.cards;

import ua.fiders.model.enums.CardKeywords;
import ua.fiders.model.enums.Type;

import java.util.Set;

public class LandCard extends Card{

    public LandCard(String name, Set<CardKeywords> keywords, String imgPath) {
        super(name, Type.Land, 0, keywords, imgPath);
    }
}
