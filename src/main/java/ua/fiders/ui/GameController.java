package ua.fiders.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import ua.fiders.logic.*;
import ua.fiders.model.*;
import ua.fiders.model.cards.*;
import ua.fiders.model.enums.*;
import ua.fiders.network.NetworkSession;
import ua.fiders.ui.panels.*;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseButton;

import ua.fiders.data.DeckLoader;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GameController {
    private final BorderPane rootLayout;

    private OpponentHandPanel opponentHandPanel;
    private GraveyardPanel playerGraveyard;
    private GraveyardPanel opponentGraveyard;

    // UI Панелі
    private PlayerInfoPanel playerInfoPanel;
    private OpponentInfoPanel opponentInfoPanel;
    private HandPanel playerHandPanel;
    private BattlefieldPanel battlefieldPanel;
    private GameControlPanel controlPanel;

    private Player localPlayer;
    private Player remotePlayer;

    private GameEngine gameEngine;

    private final NetworkSession session;
    private final boolean isHost;
    private final long seed;

    private final Map<Permanent, CardView> boardViews = new HashMap<>();
    private Permanent selectedAttacker;
    private boolean attackConfirmed;

    private Button confirmAttackBtn;
    private Button fightBtn;

    private BattleLogPanel battleLogPanel;

    private Timeline turnTimer;
    private int timeLeft = 60;

    public GameController(NetworkSession session, boolean isHost, long seed) {
        this.session = session;
        this.isHost = isHost;
        this.seed = seed;

        rootLayout = new BorderPane();
        rootLayout.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 100%, #301515 0%, #050505 85%);");

        initGame();
        setupUI();
        setupGameListener();
        setupNetwork();

        gameEngine.start();
        updateControls();
        setupTimer();
    }

    private void initGame() {
        Player hostPlayer  = new Player("Host");
        Player guestPlayer = new Player("Guest");

        localPlayer  = isHost ? hostPlayer : guestPlayer;
        remotePlayer = isHost ? guestPlayer : hostPlayer;

        DeckLoader deckLoader = new DeckLoader();
        String deckPath = "/decks/Green.json";

        List<Card> hostDeck  = deckLoader.loadDeck(deckPath);
        List<Card> guestDeck = deckLoader.loadDeck(deckPath);

        Random rng = new Random(seed);
        Collections.shuffle(hostDeck, rng);
        Collections.shuffle(guestDeck, rng);

        hostPlayer.setDeck(hostDeck);
        guestPlayer.setDeck(guestDeck);

        dealStartingHand(hostPlayer, 7);
        dealStartingHand(guestPlayer, 7);

        gameEngine = new GameEngine(hostPlayer, guestPlayer);
    }

    private void dealStartingHand(Player player, int count) {
        for (int i = 0; i < count; i++) {
            Card drawn = player.drawnCard();
            if (drawn != null) {
                player.getHand().add(drawn);
            }
        }
    }

    // Мережа

    private void setupNetwork() {
        session.setMessageHandler(message ->
                Platform.runLater(() -> handleNetworkMessage(message)));

        session.setOnDisconnected(() -> Platform.runLater(() -> {
            setInteractionEnabled(false);
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("З'єднання втрачено");
            alert.setHeaderText("Суперник від'єднався");
            alert.show();
        }));
    }

    private void handleNetworkMessage(String message) {
        String[] parts = message.trim().split("\\s+");
        switch (parts[0]) {
            case "PLAY_CARD" -> {
                int handIndex = Integer.parseInt(parts[1]);
                Card card = gameEngine.getCurrentPlayer().getHand().get(handIndex);
                gameEngine.playCard(card);
            }
            case "NEXT_PHASE" -> {
                gameEngine.nextPhase();
                onPhaseChangedLocally();
            }
            case "ATTACKER" -> {
                Permanent p = battlefieldAt(Integer.parseInt(parts[1]));
                if (p != null) {
                    boolean attacking = gameEngine.toggleAttacker(p);
                    CardView view = boardViews.get(p);
                    if (view != null) view.setHighlight(attacking);
                }
            }
            case "BLOCK" -> {
                Permanent attacker = battlefieldAt(Integer.parseInt(parts[1]));
                Permanent blocker  = battlefieldAt(Integer.parseInt(parts[2]));
                if (attacker != null && blocker != null
                        && gameEngine.assignBlocker(attacker, blocker)) {
                    CardView view = boardViews.get(blocker);
                    if (view != null) view.setHighlight(true);
                }
            }
            case "ATTACK_DONE" -> {
                attackConfirmed = true;
                updateControls();
            }
            case "COMBAT" -> {
                executeCombatBothSides();
            }
            case "SEED" -> { }

            case "CHAT" -> {
                String chatMsg = message.substring(5);
                battleLogPanel.addLogMessage("Суперник: " + chatMsg);
            }
            default -> System.out.println("Невідоме повідомлення: " + message);
        }
    }

    private Permanent battlefieldAt(int index) {
        List<Permanent> battlefield = gameEngine.getState().getBattlefield();
        if (index < 0 || index >= battlefield.size()) {
            return null;
        }
        return battlefield.get(index);
    }

    private int battlefieldIndexOf(Permanent permanent) {
        return gameEngine.getState().getBattlefield().indexOf(permanent);
    }

    private boolean isMyTurn() {
        return gameEngine.getCurrentPlayer() == localPlayer;
    }

    // Слухач рушія

    private void setupGameListener() {
        gameEngine.setListener(new GameListener() {
            @Override
            public void onManaChanged(Player player) {
                if (player == localPlayer)
                    playerInfoPanel.updateMana(player.getCurrentMana());
            }

            @Override
            public void onTurnChanged(Player newActivePlayer) {
                controlPanel.updatePhaseText(getLocalizedPhaseName(gameEngine.getCurrentPhase())
                        + "\nХід: " + (newActivePlayer == localPlayer ? "ТВІЙ" : "суперника"));
                updateControls();
            }

            @Override
            public void onPermanentEnteredBattlefield(Permanent permanent) {
                addPermanentView(permanent);
            }

            @Override
            public void onHpChanged(Player player) {
                opponentInfoPanel.updateHp();
                playerInfoPanel.updateHp();
            }

            @Override
            public void onHandUpdated(Player player) {
                if (player == localPlayer) {
                    playerHandPanel.updateHand(player.getHand());
                }
            }

            @Override
            public void onGameOver(Player winner) {
                if (turnTimer != null) turnTimer.stop();
                setInteractionEnabled(false);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Гру завершено");
                alert.setHeaderText(winner == localPlayer ? "ПЕРЕМОГА!" : "ПОРАЗКА");
                alert.setContentText("Переможець: " + winner.getName());
                alert.show();
            }

            @Override
            public void onMessage(String msg) {
                battleLogPanel.addLogMessage(msg);
            }
        });
    }

    private void addPermanentView(Permanent permanent) {
        CardView boardCardView = new CardView(permanent.getBaseCard());
        boardCardView.setOnBoardMode();
        boardViews.put(permanent, boardCardView);

        boardCardView.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                handleBoardCardClick(permanent, boardCardView);
            }
        });

        battlefieldPanel.addCard(boardCardView, permanent.getController() == localPlayer);
    }

    // Бій

    private void handleBoardCardClick(Permanent permanent, CardView view) {
        if (gameEngine.getCurrentPhase() != Phase.COMBAT) {
            return;
        }

        if (isMyTurn() && !attackConfirmed) {
            if (permanent.getController() != localPlayer) {
                return;
            }
            boolean nowAttacking = gameEngine.toggleAttacker(permanent);
            view.setHighlight(nowAttacking);
            session.send("ATTACKER " + battlefieldIndexOf(permanent));
            return;
        }

        if (!isMyTurn() && attackConfirmed) {
            if (permanent.getController() != localPlayer) {
                selectedAttacker = gameEngine.getDeclaredAttackers().contains(permanent)
                        ? permanent : null;
                return;
            }
            if (selectedAttacker == null) {
                System.out.println("Спочатку клікни ворожого атакуючого");
                return;
            }
            if (gameEngine.assignBlocker(selectedAttacker, permanent)) {
                view.setHighlight(true);
                session.send("BLOCK " + battlefieldIndexOf(selectedAttacker)
                        + " " + battlefieldIndexOf(permanent));
            }
        }
    }

    private void confirmAttackClicked() {
        attackConfirmed = true;
        session.send("ATTACK_DONE");
        updateControls();
    }

    private void fightClicked() {
        session.send("COMBAT");
        executeCombatBothSides();
    }

    private void executeCombatBothSides() {
        gameEngine.executeCombat();
        attackConfirmed = false;
        selectedAttacker = null;
        syncBattlefield();
        updateControls();
    }

    private void syncBattlefield() {
        List<Permanent> alive = gameEngine.getState().getBattlefield();

        boardViews.entrySet().removeIf(entry -> {
            Permanent p = entry.getKey();
            CardView view = entry.getValue();

            if (!alive.contains(p)) {
                battlefieldPanel.removeCard(view);
                if (p.getController() == localPlayer) {
                    playerGraveyard.addCardToTop(view);
                } else {
                    opponentGraveyard.addCardToTop(view);
                }
                return true;
            }

            if (p.isTapped() && !view.isTapped()) view.tap();
            if (!p.isTapped() && view.isTapped()) view.untap();
            view.setHighlight(false);
            return false;
        });
    }

    // UI

    private void setupUI() {
        playerInfoPanel = new PlayerInfoPanel(localPlayer);
        opponentInfoPanel = new OpponentInfoPanel(remotePlayer);
        playerHandPanel = new HandPanel();
        battlefieldPanel = new BattlefieldPanel();
        controlPanel = new GameControlPanel();

        opponentHandPanel = new OpponentHandPanel(5);
        playerGraveyard = new GraveyardPanel("ВІДБІЙ");
        opponentGraveyard = new GraveyardPanel("ВІДБІЙ ВОРОГА");

        battleLogPanel = new BattleLogPanel();
        // Підключаємо відправку повідомлень до мережі
        battleLogPanel.setOnMessageSent(text -> {
            battleLogPanel.addLogMessage("Ти: " + text);
            session.send("CHAT " + text);
        });

        controlPanel.updatePhaseText(getLocalizedPhaseName(gameEngine.getCurrentPhase()));
        controlPanel.setNextPhaseAction(this::advancePhase);
        playerHandPanel.updateHand(localPlayer.getHand());

        confirmAttackBtn = new Button("ПІДТВЕРДИТИ АТАКУ");
        confirmAttackBtn.setStyle("-fx-background-color: #f1c40f; -fx-text-fill: black; -fx-font-weight: bold;");
        confirmAttackBtn.setPrefWidth(180);
        confirmAttackBtn.setOnAction(e -> confirmAttackClicked());

        fightBtn = new Button("БІЙ!");
        fightBtn.setStyle("-fx-background-color: #ff4757; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
        fightBtn.setPrefWidth(180);
        fightBtn.setOnAction(e -> fightClicked());

        controlPanel.getChildren().addAll(confirmAttackBtn, fightBtn);

        VBox graveyardsBox = new VBox(20);
        graveyardsBox.setAlignment(Pos.CENTER);
        graveyardsBox.getChildren().addAll(opponentGraveyard, playerGraveyard);

        HBox centerLayout = new HBox(20);
        centerLayout.setAlignment(Pos.CENTER);
        HBox.setHgrow(battlefieldPanel, Priority.ALWAYS);
        centerLayout.getChildren().addAll(battlefieldPanel, graveyardsBox);

        // Розставляємо на головному екрані
        rootLayout.setTop(opponentHandPanel);
        rootLayout.setCenter(centerLayout);
        rootLayout.setBottom(playerHandPanel);

        VBox leftPanel = new VBox(20);
        VBox.setVgrow(battleLogPanel, Priority.ALWAYS);
        leftPanel.getChildren().addAll(opponentInfoPanel, battleLogPanel, playerInfoPanel);
        BorderPane.setMargin(leftPanel, new Insets(20, 0, 20, 20));
        rootLayout.setLeft(leftPanel);

        BorderPane.setMargin(controlPanel, new Insets(20, 20, 20, 20));
        rootLayout.setRight(controlPanel);

        setupDragAndDrop();
    }

    public BorderPane getRootLayout() { return rootLayout; }

    // Логіка приймання карти на ігрове поле (Drop Target)
    private void setupDragAndDrop() {
        HBox playerZone = battlefieldPanel.getPlayerZone();

        playerZone.setOnDragOver(event -> {
            if (event.getGestureSource() instanceof CardView) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        playerZone.setOnDragDropped(event -> {
            boolean success = false;

            if (event.getGestureSource() instanceof CardView dragCardView) {
                Card playedCard = dragCardView.getCard();

                if (isMyTurn()) {
                    int handIndex = localPlayer.getHand().indexOf(playedCard);

                    if (handIndex >= 0 && gameEngine.playCard(playedCard)) {
                        session.send("PLAY_CARD " + handIndex);
                        playerHandPanel.getChildren().remove(dragCardView);

                        if (playedCard.getType() == Type.Sorcery) {
                            dragCardView.setOnBoardMode();
                            playerGraveyard.addCardToTop(dragCardView);
                        }

                        success = true;
                    }
                }
            }

            event.setDropCompleted(success);
            event.consume();
        });
    }

    /**
     * Логіка перемикання ігрових фаз
     */
    private void advancePhase() {
        if (!isMyTurn()) {
            return;
        }
        gameEngine.nextPhase();
        session.send("NEXT_PHASE");
        onPhaseChangedLocally();
    }

    private void onPhaseChangedLocally() {
        attackConfirmed = false;
        selectedAttacker = null;
        syncBattlefield();
        updateControls();

        controlPanel.updatePhaseText(getLocalizedPhaseName(gameEngine.getCurrentPhase())
                + "\nХід: " + (isMyTurn() ? "ТВІЙ" : "суперника"));

        timeLeft = 60;
        controlPanel.updateTimerText(timeLeft);
        if (turnTimer != null) {
            turnTimer.playFromStart();
        }
    }

    private void updateControls() {
        boolean combat = gameEngine.getCurrentPhase() == Phase.COMBAT;

        confirmAttackBtn.setDisable(!(combat && isMyTurn() && !attackConfirmed));
        fightBtn.setDisable(!(combat && !isMyTurn() && attackConfirmed));
    }

    private String getLocalizedPhaseName(Phase phase) {
        return switch (phase) {
            case START -> "ПОЧАТОК ХОДУ";
            case MAIN -> "ГОЛОВНА ФАЗА";
            case COMBAT -> "ФАЗА БОЮ";
            case SECOND_MAIN -> "ДРУГА ГОЛОВНА";
            case END -> "КІНЕЦЬ ХОДУ";
        };
    }

    // Для мультиплеєра знадобиться
    private void setInteractionEnabled(boolean enabled) {
        controlPanel.setDisable(!enabled);
        playerHandPanel.setDisable(!enabled);
        battlefieldPanel.getPlayerZone().setDisable(!enabled);
    }

    private void setupTimer() {
        turnTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeLeft--;
            controlPanel.updateTimerText(timeLeft);

            if (timeLeft <= 0) {
                if (isMyTurn()) {
                    System.out.println("Час вийшов! Автоматичний пропуск фази.");
                    advancePhase();
                } else {
                    timeLeft = 0;
                    controlPanel.updateTimerText(0);
                }
            }
        }));
        turnTimer.setCycleCount(Animation.INDEFINITE);
        turnTimer.play();
    }
}
