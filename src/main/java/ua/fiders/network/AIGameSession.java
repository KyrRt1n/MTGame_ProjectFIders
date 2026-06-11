package ua.fiders.network;

import ua.fiders.data.CardRepository;
import ua.fiders.logic.GameEngine;
import ua.fiders.model.GameState;
import ua.fiders.model.Permanent;
import ua.fiders.model.Player;
import ua.fiders.model.cards.Card;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AIGameSession — сесія гри "Гравець проти Комп'ютера".
 *
 * Відповідає за:
 *  - Ініціалізацію гравця й бота з реальними колодами з CardRepository.
 *  - Запуск GameEngine.
 *  - Делегацію ходу боту через BotAI після кожного ходу людини.
 *
 * Схема використання (з UI або консолі):
 *
 *   AIGameSession session = new AIGameSession("Player");
 *   session.start();
 *
 *   // Хід гравця (кожна дія через session.getEngine())
 *   session.getEngine().playCard(someCard);
 *   session.getEngine().nextPhase(); // ...
 *
 *   // Після закінчення ходу гравця — бот сам ходить
 *   if (session.isBotTurn()) {
 *       session.executeBotTurn();
 *   }
 */
public class AIGameSession {

    private final Player     humanPlayer;
    private final Player     botPlayer;
    private final GameEngine engine;
    private final BotAI      botAI;

    public AIGameSession(String humanName) {
        this.humanPlayer = new Player(humanName);
        this.botPlayer   = new Player("AI Bot");

        // Завантажуємо колоди з репозиторію JSON
        CardRepository repo = CardRepository.getInstance();
        List<Card> humanDeck = repo.buildStarterDeck();
        List<Card> botDeck   = repo.buildStarterDeck();

        humanPlayer.setDeck(humanDeck);
        botPlayer.setDeck(botDeck);

        // Роздаємо стартові руки (7 карт кожному)
        dealStartingHand(humanPlayer, 7);
        dealStartingHand(botPlayer,   7);

        this.engine = new GameEngine(humanPlayer, botPlayer);
        this.botAI  = new BotAI(engine, botPlayer);
    }

    // ----- Public API -----

    /** Запускає гру (START фаза першого ходу). */
    public void start() {
        engine.start();
        System.out.println("[AIGameSession] Гра розпочата. Хід: "
                + engine.getCurrentPlayer().getName());
    }

    /**
     * Виконує повний хід бота.
     * Викликати, коли гравець натискає "Завершити хід" і хід переходить до бота.
     */
    public void executeBotTurn() {
        if (!isBotTurn()) {
            System.out.println("[AIGameSession] Зараз не хід бота!");
            return;
        }
        System.out.println("[AIGameSession] Хід AI бота...");
        botAI.takeTurn();
        System.out.println("[AIGameSession] Бот завершив хід.");
    }

    /**
     * Бот блокує атакуючих гравця.
     * Викликати під час COMBAT-фази ходу людини, передавши список атакуючих.
     *
     * @param humanAttackers атакуючі Permanent гравця-людини
     * @return карта: атакуючий -> блокер бота
     */
    public Map<Permanent, Permanent> getBotBlocks(List<Permanent> humanAttackers) {
        if (humanAttackers == null || humanAttackers.isEmpty()) {
            return Collections.emptyMap();
        }
        return botAI.chooseBotBlocks(humanAttackers, engine.getState());
    }

    /** @return true, якщо зараз хід бота */
    public boolean isBotTurn() {
        return engine.getCurrentPlayer() == botPlayer;
    }

    /** @return true, якщо гра закінчена */
    public boolean isGameOver() {
        return engine.isGameOver();
    }

    /** @return переможець або null, якщо гра ще не закінчена */
    public Player getWinner() {
        return engine.getWinner();
    }

    public GameEngine getEngine()  { return engine;      }
    public Player     getHuman()   { return humanPlayer; }
    public Player     getBot()     { return botPlayer;   }
    public GameState  getState()   { return engine.getState(); }

    // ----- Private helpers -----

    /** Роздає count карт зі стопки колоди в руку гравця. */
    private void dealStartingHand(Player player, int count) {
        for (int i = 0; i < count; i++) {
            Card drawn = player.drawnCard();
            if (drawn != null) {
                player.getHand().add(drawn);
            }
        }
    }
}
