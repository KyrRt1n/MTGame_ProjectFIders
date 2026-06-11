package ua.fiders.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ua.fiders.model.cards.Card;
import ua.fiders.model.cards.CreatureCard;
import ua.fiders.model.cards.LandCard;
import ua.fiders.model.cards.SpellCard;
import ua.fiders.model.effects.CardEffect;
import ua.fiders.model.effects.DamageEnemyEffect;
import ua.fiders.model.effects.DrawCardEffect;
import ua.fiders.model.effects.HealPlayerEffect;
import ua.fiders.model.enums.CardKeywords;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Репозиторій карток.
 * Завантажує всі карти з /resources/cards.json один раз (singleton-кеш).
 *
 * Використання:
 *   List<Card> all  = CardRepository.getInstance().getAllCards();
 *   List<Card> deck = CardRepository.getInstance().buildStarterDeck();
 */
public class CardRepository {

    private static CardRepository instance;

    // Кешований список усіх карток
    private final List<Card> allCards;

    // ----- Singleton -----
    private CardRepository() {
        this.allCards = loadFromJson();
    }

    public static CardRepository getInstance() {
        if (instance == null) {
            instance = new CardRepository();
        }
        return instance;
    }

    // ----- Public API -----

    /** Повертає незмінну копію всього репозиторію. */
    public List<Card> getAllCards() {
        return Collections.unmodifiableList(allCards);
    }

    /**
     * Повертає перемішану стартову колоду з 20 карт:
     * 8 Земель + 12 звичайних карт (Істоти + Закляття).
     * Гарантовано не менше 4 Земель, щоб гравець міг виставляти інші карти.
     */
    public List<Card> buildStarterDeck() {
        List<Card> lands    = allCards.stream()
                .filter(c -> c.getType().name().equals("Land"))
                .collect(Collectors.toList());

        List<Card> nonLands = allCards.stream()
                .filter(c -> !c.getType().name().equals("Land"))
                .collect(Collectors.toList());

        Collections.shuffle(lands);
        Collections.shuffle(nonLands);

        List<Card> deck = new ArrayList<>();

        // Беремо 8 земель (з повторенням, якщо в JSON їх менше)
        for (int i = 0; i < 8; i++) {
            deck.add(cloneCard(lands.get(i % lands.size())));
        }

        // Беремо 12 не-земель (з повторенням)
        for (int i = 0; i < 12; i++) {
            deck.add(cloneCard(nonLands.get(i % nonLands.size())));
        }

        Collections.shuffle(deck);
        return deck;
    }

    // ----- Private helpers -----

    private List<Card> loadFromJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();

            InputStream is = CardRepository.class
                    .getResourceAsStream("/cards.json");

            if (is == null) {
                System.err.println("[CardRepository] cards.json не знайдено в ресурсах!");
                return Collections.emptyList();
            }

            List<CardDto> dtos = mapper.readValue(is, new TypeReference<>() {});
            List<Card> result  = new ArrayList<>();

            for (CardDto dto : dtos) {
                Card card = convertDto(dto);
                if (card != null) {
                    result.add(card);
                }
            }

            System.out.println("[CardRepository] Завантажено " + result.size() + " карт.");
            return result;

        } catch (Exception e) {
            System.err.println("[CardRepository] Помилка читання JSON: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private Card convertDto(CardDto dto) {
        Set<CardKeywords> keywords = parseKeywords(dto.keywords);

        return switch (dto.type.toUpperCase()) {
            case "CREATURE" -> new CreatureCard(
                    dto.name, dto.manaCost, keywords, dto.imgPath,
                    dto.attack, dto.hp);

            case "LAND"     -> new LandCard(
                    dto.name, keywords, dto.imgPath);

            case "SPELL"    -> {
                List<CardEffect> effects = parseEffects(dto.effects);
                yield new SpellCard(dto.name, dto.manaCost, dto.imgPath, effects);
            }

            default -> {
                System.err.println("[CardRepository] Невідомий тип карти: " + dto.type);
                yield null;
            }
        };
    }

    private Set<CardKeywords> parseKeywords(List<String> raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptySet();
        Set<CardKeywords> result = new HashSet<>();
        for (String kw : raw) {
            try {
                result.add(CardKeywords.valueOf(kw));
            } catch (IllegalArgumentException e) {
                System.err.println("[CardRepository] Невідомий keyword: " + kw);
            }
        }
        return result;
    }

    private List<CardEffect> parseEffects(List<CardDto.EffectDto> raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptyList();
        List<CardEffect> result = new ArrayList<>();
        for (CardDto.EffectDto eff : raw) {
            CardEffect effect = switch (eff.effectType.toUpperCase()) {
                case "DAMAGE_ENEMY" -> new DamageEnemyEffect(eff.amount);
                case "HEAL_PLAYER"  -> new HealPlayerEffect(eff.amount);
                case "DRAW_CARD"    -> new DrawCardEffect(eff.amount);
                default -> {
                    System.err.println("[CardRepository] Невідомий effectType: " + eff.effectType);
                    yield null;
                }
            };
            if (effect != null) result.add(effect);
        }
        return result;
    }

    /**
     * "Клонує" картку: для CREATURE і SPELL це важливо, щоб кожна копія
     * у колоді була окремим об'єктом (інакше два гравці можуть
     * ділити один і той самий об'єкт).
     */
    private Card cloneCard(Card original) {
        return switch (original) {
            case CreatureCard c -> new CreatureCard(
                    c.getName(), c.getManaCost(),
                    new HashSet<>(c.getKeywords()),
                    c.getName().toLowerCase().replace(" ", "_"),
                    c.getAttack(), c.getHp());

            case LandCard l -> new LandCard(
                    l.getName(),
                    new HashSet<>(l.getKeywords()),
                    l.getName().toLowerCase());

            case SpellCard s -> new SpellCard(
                    s.getName(), s.getManaCost(),
                    s.getName().toLowerCase().replace(" ", "_"),
                    s.getEffects());

            default -> original; // не очікується
        };
    }
}
