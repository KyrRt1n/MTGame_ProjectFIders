package ua.fiders.model.effects;

public sealed interface CardEffect permits
        DrawCardEffect,
        HealPlayerEffect,
        DamageEnemyEffect,
        BuffStatsEffect,
        DestroyTargetEffect,
        BiteEffect {
}