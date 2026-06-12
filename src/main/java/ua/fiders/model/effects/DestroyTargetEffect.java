package ua.fiders.model.effects;
import ua.fiders.model.enums.CardKeywords;
// requiredKeyword == null - destroys any. if present - only with this
public record DestroyTargetEffect(CardKeywords requiredKeyword) implements CardEffect {

}