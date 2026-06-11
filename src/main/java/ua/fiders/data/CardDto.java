package ua.fiders.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Data Transfer Object для десеріалізації карт з JSON.
 * Jackson заповнює поля автоматично за назвою ключів у cards.json.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardDto {

    // "CREATURE" | "LAND" | "SPELL"
    public String type;
    public String name;
    public int manaCost;
    public String imgPath;

    // Тільки для CREATURE
    public int attack;
    public int hp;

    // Список рядків: "Flying", "Lifelink", "Trample"
    public List<String> keywords;

    // Тільки для SPELL
    public List<EffectDto> effects;

    /** Вкладений DTO для ефектів закляттів */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EffectDto {
        // "DAMAGE_ENEMY" | "HEAL_PLAYER" | "DRAW_CARD"
        public String effectType;
        public int amount;
    }
}
