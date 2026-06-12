package ua.fiders.model.effects;

public record BuffStatsEffect(int attackAmount, int hpAmount, boolean isPermanent) implements CardEffect {
}