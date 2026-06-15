package ua.fiders.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ua.fiders.model.cards.Card;
import ua.fiders.model.cards.CreatureCard;
import ua.fiders.model.cards.LandCard;
import ua.fiders.model.cards.SpellCard;
import ua.fiders.model.enums.CardKeywords;
import ua.fiders.model.effects.*;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeckLoader {
    private final ObjectMapper objectMapper;

    public DeckLoader() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Завантажує колоду з JSON-файлу, що знаходиться в resources.
     */
    public List<Card> loadDeck(String resourcePath, String language) {
        List<Card> deck = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Файл колоди не знайдено за шляхом: " + resourcePath);
            }

            JsonNode rootNode = objectMapper.readTree(is);

            if (rootNode.isArray()) {
                for (JsonNode cardNode : rootNode) {
                    String type = cardNode.get("type").asText();
                    int count = cardNode.get("count").asInt();

                    for (int i = 0; i < count; i++) {
                        Card card = createCardFromJson(type, cardNode, language);
                        if (card != null) {
                            deck.add(card);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Помилка під час завантаження або парсингу колоди: " + e.getMessage());
        }

        return deck;
    }

    /**
     * Фабричний метод, що створює карти згідно з точними конструкторами моделей
     */
    private Card createCardFromJson(String type, JsonNode node, String language) {
        String name = node.get("name").has(language)
                ? node.get("name").get(language).asText()
                : node.get("name").get("uk").asText();

        String imgPath = node.get("imgPath").asText();

        String description = "";
        if (node.has("description")) {
            description = node.get("description").has(language)
                    ? node.get("description").get(language).asText()
                    : node.get("description").get("uk").asText();
        }

        Card card = null;

        switch (type) {
            case "Land":
                Set<CardKeywords> landKeywords = parseKeywords(node, "keywords");
                card = new LandCard(name, landKeywords, imgPath);
                break;

            case "Creature":
                int manaCost = node.get("manaCost").asInt();
                int attack = node.get("attack").asInt();
                int hp = node.get("hp").asInt();
                Set<CardKeywords> creatureKeywords = parseKeywords(node, "keywords");
                Set<CardKeywords> grantedKws = parseKeywords(node, "grantedKeywords");

                CreatureCard c = new CreatureCard(name, manaCost, creatureKeywords, imgPath, attack, hp);
                c.setGrantedKeywords(grantedKws);
                card = c;
                break;

            case "Sorcery":
                int spellManaCost = node.get("manaCost").asInt();
                // Завантажуємо список ефектів для заклинання
                List<CardEffect> effects = parseEffects(node);
                card = new SpellCard(name, spellManaCost, imgPath, effects);
                break;

            default:
                System.err.println("Попередження: Невідомий тип карти в JSON: " + type);
                return null;
        }

        if (card != null)
            card.setDescription(description);

        return card;
    }

    /**
     * Допоміжний метод для парсингу ключових слів (Enums)
     */
    private Set<CardKeywords> parseKeywords(JsonNode node, String fieldName) {
        Set<CardKeywords> keywordsSet = new HashSet<>();
        if (node.has(fieldName) && node.get(fieldName).isArray()) {
            for (JsonNode keywordNode : node.get(fieldName)) {
                try {
                    String kwStr = keywordNode.asText().toUpperCase();
                    keywordsSet.add(CardKeywords.valueOf(kwStr));
                } catch (IllegalArgumentException e) {
                    System.err.println("Попередження: Ківорд '" + keywordNode.asText() + "' не знайдено!");
                }
            }
        }
        return keywordsSet;
    }

    /**
     * Допоміжний метод для автоматичного парсингу списку ефектів заклинання
     */
    private List<CardEffect> parseEffects(JsonNode node) {
        List<CardEffect> effectsList = new ArrayList<>();
        if (node.has("effects") && node.get("effects").isArray()) {
            for (JsonNode effectNode : node.get("effects")) {

                String effectType = effectNode.get("type").asText();

                CardEffect effect = switch (effectType) {
                    case "DAMAGE_ENEMY" -> {
                        int amount = effectNode.has("amount") ? effectNode.get("amount").asInt() : 0;
                        yield new DamageEnemyEffect(amount);
                    }
                    case "HEAL_PLAYER" -> {
                        int amount = effectNode.has("amount") ? effectNode.get("amount").asInt() : 0;
                        yield new HealPlayerEffect(amount);
                    }
                    case "DRAW_CARD" -> {
                        int amount = effectNode.has("amount") ? effectNode.get("amount").asInt() : 1;
                        yield new DrawCardEffect(amount);
                    }
                    case "BUFF_STATS" -> {
                        int attack = effectNode.has("attackAmount") ? effectNode.get("attackAmount").asInt() : 0;
                        int hp = effectNode.has("hpAmount") ? effectNode.get("hpAmount").asInt() : 0;
                        boolean isPermanent = effectNode.has("isPermanent") && effectNode.get("isPermanent").asBoolean();

                        yield new BuffStatsEffect(attack, hp, isPermanent);
                    }
                    case "DESTROY_TARGET" -> {
                        CardKeywords requiredKeyword = null;
                        if (effectNode.has("requiredKeyword")) {
                            try {
                                requiredKeyword = CardKeywords.valueOf(effectNode.get("requiredKeyword").asText().toUpperCase());
                            } catch (IllegalArgumentException e) {
                                System.err.println("Попередження: Невідомий ківорд для DESTROY_TARGET: " + effectNode.get("requiredKeyword").asText());
                            }
                        }
                        yield new DestroyTargetEffect(requiredKeyword);
                    }
                    case "BITE_EFFECT" -> new BiteEffect();
                    case "DAMAGE_TARGET" -> {
                        int amount = effectNode.has("amount") ? effectNode.get("amount").asInt() : 0;
                        yield new DamageTargetEffect(amount);
                    }

                    default -> {
                        System.err.println("Попередження: Невідомий тип ефекту: " + effectType);
                        yield null;
                    }
                };

                if (effect != null)
                    effectsList.add(effect);
            }
        }
        return effectsList;
    }
}