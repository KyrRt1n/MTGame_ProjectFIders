package ua.fiders.model.cards;

import ua.fiders.model.effects.*;
import ua.fiders.model.enums.*;
import java.util.*;

public class SpellCard extends Card {
    private final List<CardEffect> effects;

    public SpellCard(String name, int manaCost, String imgPath, List<CardEffect> effects) {
        super(name, Type.Sorcery, manaCost, new HashSet<>(), imgPath);
        this.effects = List.copyOf(effects);
    }

    public List<CardEffect> getEffects() {
        return effects;
    }
}