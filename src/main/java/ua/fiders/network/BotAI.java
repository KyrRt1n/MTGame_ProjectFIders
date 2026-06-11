package ua.fiders.network;

import ua.fiders.logic.GameEngine;
import ua.fiders.model.GameState;
import ua.fiders.model.Permanent;
import ua.fiders.model.Player;
import ua.fiders.model.cards.Card;
import ua.fiders.model.cards.CreatureCard;
import ua.fiders.model.enums.CardKeywords;
import ua.fiders.model.enums.Phase;

import java.util.*;
import java.util.stream.Collectors;

/**
 * BotAI — штучний інтелект для гравця-комп'ютера.
 *
 * Стратегія (Greedy / Rule-Based):
 *  1. MAIN  — грає якомога більше карт за доступну ману
 *             (спершу Землі, потім Істоти з найвищою атакою, потім Закляття).
 *  2. COMBAT — атакує всіма незатисненими істотами.
 *              При блокуванні обирає найвигідніший блокер (вбити атакуючого
 *              з мінімальними втратами або захистити гравця).
 *
 * Виклик:
 *   BotAI bot = new BotAI(gameEngine, botPlayer);
 *   bot.takeTurn();   // виконує один повний хід бота
 */
public class BotAI {

    private final GameEngine engine;
    private final Player     bot;

    public BotAI(GameEngine engine, Player bot) {
        this.engine = engine;
        this.bot    = bot;
    }

    // ----------------------------------------------------------------
    //  Головний метод: один повний хід бота
    // ----------------------------------------------------------------

    /**
     * Виконує всі фази ходу бота послідовно.
     * Викликається ззовні, коли настає хід бота.
     */
    public void takeTurn() {
        GameState state = engine.getState();

        // Переконуємося, що активний гравець — справді бот
        if (state.getCurrentPlayer() != bot) {
            System.out.println("[BotAI] Не мій хід — пропускаю.");
            return;
        }

        // START: engine.start() вже відпрацював (untap + draw + refillMana)
        // через PhaseManager, тому просто переходимо до головної фази.
        advanceToPhase(Phase.MAIN);

        // MAIN PHASE: граємо карти
        playCardsPhase(state);

        // COMBAT PHASE: оголошуємо атакуючих
        advanceToPhase(Phase.COMBAT);
        resolveCombatPhase(state);

        // SECOND MAIN: можна зіграти ще карти
        advanceToPhase(Phase.SECOND_MAIN);
        playCardsPhase(state);

        // END: передаємо хід
        advanceToPhase(Phase.END);
        engine.nextPhase(); // переходить до START суперника
    }

    // ----------------------------------------------------------------
    //  Головна фаза — розіграш карт
    // ----------------------------------------------------------------

    private void playCardsPhase(GameState state) {
        // 1. Землі (не витрачають ману)
        playLands();

        // 2. Істоти: сортуємо за силою (attack + hp) — найсильніші спершу
        List<Card> hand = new ArrayList<>(bot.getHand());
        hand.stream()
            .filter(c -> c instanceof CreatureCard)
            .sorted(Comparator.comparingInt(this::creaturePriority).reversed())
            .forEach(c -> engine.playCard(c));

        // 3. Закляття: якщо ще залишилася мана
        new ArrayList<>(bot.getHand()).stream()
            .filter(c -> !(c instanceof CreatureCard))
            .filter(c -> c.getManaCost() <= bot.getCurrentMana())
            .forEach(c -> engine.playCard(c));
    }

    private void playLands() {
        // Може зіграти лише одну землю за хід — engine.playCard() сам це перевірить
        bot.getHand().stream()
            .filter(c -> c.getManaCost() == 0 && !(c instanceof CreatureCard))
            .findFirst()
            .ifPresent(engine::playCard);
    }

    /** Пріоритет Істоти = attack + hp (чим більше — тим вигідніше грати першою). */
    private int creaturePriority(Card card) {
        if (card instanceof CreatureCard c) return c.getAttack() + c.getHp();
        return 0;
    }

    // ----------------------------------------------------------------
    //  Бойова фаза
    // ----------------------------------------------------------------

    private void resolveCombatPhase(GameState state) {
        List<Permanent> botCreatures = getBotCreatures(state);

        if (botCreatures.isEmpty()) {
            System.out.println("[BotAI] Немає істот для атаки.");
            return;
        }

        // Всі незатиснені істоти атакують
        List<Permanent> attackers = botCreatures.stream()
                .filter(p -> !p.isTapped())
                .collect(Collectors.toList());

        if (attackers.isEmpty()) {
            System.out.println("[BotAI] Всі істоти вже затиснені.");
            return;
        }

        // Блоки: гравець-людина вибирає блокерів сам (у звичайній грі).
        // Але якщо бот атакує — блоків немає з боку бота.
        // Якщо ж людина атакує, бот як захисник обирає блокерів:
        //   цей метод викликається ззовні через chooseBotBlocks().
        Map<Permanent, Permanent> blocks = Collections.emptyMap();

        System.out.println("[BotAI] Атакує " + attackers.size() + " істот(а).");
        engine.resolveCombat(attackers, blocks);
    }

    /**
     * Бот обирає блокерів для захисту від атакуючих людини.
     * Викликати ззовні, коли людина оголошує атакуючих.
     *
     * Стратегія:
     *  - Якщо атакуючий — Flying, відповідає лише Flying-блокер.
     *  - Обираємо блокера, який може вбити атакуючого без власної загибелі (ideal).
     *  - Якщо ідеального немає — ставимо найміцнішого, щоб захистити гравця.
     *
     * @param humanAttackers список атакуючих з боку людини
     * @return map attacker -> blocker
     */
    public Map<Permanent, Permanent> chooseBotBlocks(List<Permanent> humanAttackers,
                                                      GameState state) {
        Map<Permanent, Permanent> result = new LinkedHashMap<>();
        List<Permanent> availableBlockers = new ArrayList<>(getBotCreatures(state));

        for (Permanent attacker : humanAttackers) {
            // Flying — лише Flying-блокер
            boolean needsFlying = attacker.hasKeyword(CardKeywords.Flying);

            Optional<Permanent> bestBlocker = availableBlockers.stream()
                    .filter(b -> !needsFlying || b.hasKeyword(CardKeywords.Flying))
                    .filter(b -> canSurviveBlock(b, attacker))      // ideal: виживе
                    .max(Comparator.comparingInt(Permanent::getCurrentAttack)); // найсильніший що виживе

            if (bestBlocker.isEmpty()) {
                // Немає ідеального — ставимо "смертника": найслабшого, щоб не втрачати сильних
                bestBlocker = availableBlockers.stream()
                        .filter(b -> !needsFlying || b.hasKeyword(CardKeywords.Flying))
                        .min(Comparator.comparingInt(Permanent::getRemainingHp));
            }

            bestBlocker.ifPresent(blocker -> {
                result.put(attacker, blocker);
                availableBlockers.remove(blocker); // один блокер — одна ціль
                System.out.println("[BotAI] Блокую " + attacker.getBaseCard().getName()
                        + " своєю " + blocker.getBaseCard().getName());
            });
        }

        return result;
    }

    /**
     * Чи виживе blocker після атаки attacker?
     * (blocker.hp > attacker.attack)
     */
    private boolean canSurviveBlock(Permanent blocker, Permanent attacker) {
        return blocker.getRemainingHp() > attacker.getCurrentAttack();
    }

    // ----------------------------------------------------------------
    //  Допоміжні методи
    // ----------------------------------------------------------------

    /** Просуваємо фази гри до потрібної. */
    private void advanceToPhase(Phase target) {
        int safetyCounter = 0;
        while (engine.getCurrentPhase() != target && safetyCounter++ < 10) {
            engine.nextPhase();
        }
    }

    private List<Permanent> getBotCreatures(GameState state) {
        return state.getBattlefield().stream()
                .filter(p -> p.getController() == bot)
                .filter(p -> p.getBaseCard() instanceof CreatureCard)
                .collect(Collectors.toList());
    }
}
