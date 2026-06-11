package ua.fiders.model.cards;

import ua.fiders.model.enums.CardKeywords;
import ua.fiders.model.enums.Type;

import java.util.HashSet;
import java.util.Set;

public abstract class Card {
    private String name;
    private int manaCost;
    private Type type;
    protected Set<CardKeywords> keywords;
    private String imgPath;

    public Card(String name, Type type, int manaCost, Set<CardKeywords> keywords, String imgPath) {
        this.name = name;
        this.type = type;
        this.manaCost = manaCost;
        this.keywords = (keywords != null) ? keywords : new HashSet<>();
        this.imgPath = imgPath;
    }

    public String getImgPath() {
        return imgPath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getManaCost() {
        return manaCost;
    }

    public void setManaCost(int manaCost) {
        this.manaCost = manaCost;
    }

    public Set<CardKeywords> getKeywords() {
        return keywords;
    }

    public Type getType() {
        return type;
    }
}