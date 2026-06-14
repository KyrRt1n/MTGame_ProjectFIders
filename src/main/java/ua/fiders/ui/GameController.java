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
import javafx.scene.layout.StackPane;
import javafx.geometry.Bounds;
import javafx.animation.TranslateTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;

import ua.fiders.data.DeckLoader;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GameController {

    private final StackPane glassPane;
    private final BorderPane rootLayout;

    private OpponentHandPanel opponentHandPanel;
    private GraveyardPanel playerGraveyard;
    private GraveyardPanel opponentGraveyard;

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
    private int timeLeft = 120;

    private Card pendingSpell = null;
    private int targetsNeeded = 0;
    private Permanent target1 = null;
    private Permanent target2 = null;
    private CardView pendingSpellView = null;

    public GameController(NetworkSession session, boolean isHost, long seed) {
        this.session = session;
        this.isHost = isHost;
        this.seed = seed;

        rootLayout = new BorderPane();
        rootLayout.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 100%, #301515 0%, #050505 85%);");

        glassPane = new StackPane();
        glassPane.getChildren().add(rootLayout);
        glassPane.setPickOnBounds(false);

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
        String greenDeckPath = "/decks/Green.json";
        String redDeckPath  = "/decks/Red.json";

        List<Card> hostDeck  = deckLoader.loadDeck(greenDeckPath);
        List<Card> guestDeck = deckLoader.loadDeck(redDeckPath);

        Random rng = new Random(seed);
        Collections.shuffle(hostDeck, rng);
        Collections.shuffle(guestDeck, rng);

        hostPlayer.setDeck(hostDeck);
        guestPlayer.setDeck(guestDeck);

        dealStartingHand(hostPlayer, 6);
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

                if (gameEngine.playCard(card)) {
                    opponentHandPanel.updateHandSize(gameEngine.getCurrentPlayer().getHand().size());
                    if (card.getType() == Type.Sorcery) showSpellAnimation(card, false);
                    syncBattlefield();
                }
            }
            case "PLAY_SPELL_TARGET" -> {
                int handIndex = Integer.parseInt(parts[1]);
                int t1Index = Integer.parseInt(parts[2]);
                int t2Index = Integer.parseInt(parts[3]);

                Card card = gameEngine.getCurrentPlayer().getHand().get(handIndex);
                Permanent t1 = battlefieldAt(t1Index);
                Permanent t2 = battlefieldAt(t2Index);

                if (gameEngine.playCard(card, t1, t2)) {
                    opponentHandPanel.updateHandSize(gameEngine.getCurrentPlayer().getHand().size());
                    showSpellAnimation(card, false);
                    syncBattlefield();
                }
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
                if (attacker != null && blocker != null && gameEngine.assignBlocker(attacker, blocker)) {
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
                String chatMsg = message.startsWith("CHAT ") ? message.substring(5) : "";
                if (!chatMsg.isBlank()) {
                    battleLogPanel.addLogMessage("Суперник: " + chatMsg);
                }
            }
            default -> System.out.println("Невідоме повідомлення: " + message);
        }
    }

    private Permanent battlefieldAt(int index) {
        if (index == -1) return null;
        List<Permanent> battlefield = gameEngine.getState().getBattlefield();
        if (index < 0 || index >= battlefield.size()) return null;
        return battlefield.get(index);
    }

    private int battlefieldIndexOf(Permanent permanent) {
        if (permanent == null) return -1;
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
                if (player == localPlayer) {
                    playerInfoPanel.updateMana(player.getCurrentMana());
                } else if (player == remotePlayer) {
                    opponentInfoPanel.updateMana(player.getCurrentMana());
                }
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
                } else {
                    opponentHandPanel.updateHandSize(player.getHand().size());
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
            } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                cancelTargeting(); // Правий клік скасовує прицілювання
            }
        });

        battlefieldPanel.addCard(boardCardView, permanent.getController() == localPlayer);
    }

    // бій та прицілювання

    private void handleBoardCardClick(Permanent permanent, CardView view) {

        // режим прицілювання заклинання
        if (pendingSpell != null) {
            handleTargetSelection(permanent, view);
            return;
        }

        // фаза бою
        if (gameEngine.getCurrentPhase() == Phase.COMBAT) {
            handleCombatClick(permanent, view);
        }
    }

    private void handleTargetSelection(Permanent permanent, CardView view) {
        if (target1 == null) {
            target1 = permanent;
            view.setStyle("-fx-border-color: #3498db; -fx-border-width: 4; -fx-border-radius: 12;"); // Підсвітка цілі
            if (targetsNeeded == 1) applyPendingSpell();
            else battleLogPanel.addLogMessage("Оберіть ціль №2...");
        } else if (target2 == null && permanent != target1) {
            target2 = permanent;
            view.setStyle("-fx-border-color: #3498db; -fx-border-width: 4; -fx-border-radius: 12;");
            if (targetsNeeded == 2) applyPendingSpell();
        }
    }

    private void applyPendingSpell() {
        int handIndex = localPlayer.getHand().indexOf(pendingSpell);
        int t1Index = battlefieldIndexOf(target1);
        int t2Index = battlefieldIndexOf(target2);

        if (gameEngine.playCard(pendingSpell, target1, target2)) {
            session.send("PLAY_SPELL_TARGET " + handIndex + " " + t1Index + " " + t2Index);
            playerHandPanel.getChildren().remove(pendingSpellView);
            showSpellAnimation(pendingSpell, true);
        } else {
            battleLogPanel.addLogMessage("Не вдалося зіграти заклинання.");
        }
        cancelTargeting();
    }

    private void cancelTargeting() {
        if (pendingSpell != null) {
            battleLogPanel.addLogMessage("Прицілювання скасовано.");
            if (pendingSpellView != null) pendingSpellView.setOpacity(1.0);
        }
        pendingSpell = null;
        target1 = null;
        target2 = null;
        targetsNeeded = 0;
        pendingSpellView = null;
        syncBattlefield();
    }

    private void handleCombatClick(Permanent permanent, CardView view) {
        if (isMyTurn() && !attackConfirmed) {
            if (permanent.getController() != localPlayer) return;
            boolean nowAttacking = gameEngine.toggleAttacker(permanent);
            view.setHighlight(nowAttacking);
            session.send("ATTACKER " + battlefieldIndexOf(permanent));
            return;
        }

        if (!isMyTurn() && attackConfirmed) {
            if (permanent.getController() != localPlayer) {
                selectedAttacker = gameEngine.getDeclaredAttackers().contains(permanent) ? permanent : null;
                return;
            }
            if (selectedAttacker == null) {
                battleLogPanel.addLogMessage("Спочатку клікни ворожого атакуючого!");
                return;
            }
            if (gameEngine.assignBlocker(selectedAttacker, permanent)) {
                view.setHighlight(true);
                session.send("BLOCK " + battlefieldIndexOf(selectedAttacker) + " " + battlefieldIndexOf(permanent));
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

        controlPanel.updatePhaseText(getLocalizedPhaseName(gameEngine.getCurrentPhase())
                + "\nХід: " + (isMyTurn() ? "ТВІЙ" : "суперника"));
    }

    private void syncBattlefield() {
        List<Permanent> alive = gameEngine.getState().getBattlefield();

        boardViews.entrySet().removeIf(entry -> {
            Permanent p = entry.getKey();
            CardView view = entry.getValue();

            if (!alive.contains(p)) {
                battlefieldPanel.removeCard(view);
                if (p.getController() == localPlayer) playerGraveyard.addCardToTop(view);
                else opponentGraveyard.addCardToTop(view);
                return true;
            }

            if (p.isTapped() && !view.isTapped()) view.tap();
            if (!p.isTapped() && view.isTapped()) view.untap();
            view.setHighlight(false);

            view.updateStats(p.getCurrentAttack(), p.getRemainingHp(), p.getMaxHp());

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

        opponentHandPanel = new OpponentHandPanel(remotePlayer.getHand().size());
        playerGraveyard = new GraveyardPanel("ВІДБІЙ");
        opponentGraveyard = new GraveyardPanel("ВІДБІЙ ВОРОГА");

        battleLogPanel = new BattleLogPanel();
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

    public StackPane getRootLayout() { return glassPane; }

    // Логіка приймання карти на ігрове поле + ТАРГЕТИНГ
    private void setupDragAndDrop() {
        HBox playerZone = battlefieldPanel.getPlayerZone();

        // Додаємо можливість відмінити драг енд дроп через правий клік по столу
        playerZone.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) cancelTargeting();
        });

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

                if (isMyTurn() && localPlayer.getCurrentMana() >= playedCard.getManaCost()) {

                    int needed = gameEngine.requiredTargets(playedCard);

                    // Якщо заклинання потребує цілі
                    if (needed > 0) {
                        pendingSpell = playedCard;
                        targetsNeeded = needed;
                        pendingSpellView = dragCardView;

                        // Повертаємо карту візуально в руку, поки не виберемо ціль
                        dragCardView.setOpacity(0.5);
                        battleLogPanel.addLogMessage("Оберіть " + needed + " ціль(і) для " + playedCard.getName());
                        success = false;
                    } else {
                        // Звичайна карта без цілей
                        int handIndex = localPlayer.getHand().indexOf(playedCard);
                        if (handIndex >= 0 && gameEngine.playCard(playedCard)) {
                            session.send("PLAY_CARD " + handIndex);
                            playerHandPanel.getChildren().remove(dragCardView);

                            if (playedCard.getType() == Type.Sorcery) {
                                showSpellAnimation(playedCard, true);
                            }
                            success = true;
                        }
                    }
                } else {
                    battleLogPanel.addLogMessage("Недостатньо мани!");
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    // анімація заклять
    private void showSpellAnimation(Card card, boolean isLocal) {
        CardView spellView = new CardView(card);
        spellView.setOnBoardMode();

        spellView.setScaleX(1.5);
        spellView.setScaleY(1.5);

        glassPane.getChildren().add(spellView);

        PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
        pause.setOnFinished(e -> {
            GraveyardPanel targetGraveyard = isLocal ? playerGraveyard : opponentGraveyard;

            Bounds cardBounds = spellView.localToScene(spellView.getBoundsInLocal());
            Bounds gyBounds = targetGraveyard.localToScene(targetGraveyard.getBoundsInLocal());

            double moveX = gyBounds.getCenterX() - cardBounds.getCenterX();
            double moveY = gyBounds.getCenterY() - cardBounds.getCenterY();

            TranslateTransition tt = new TranslateTransition(Duration.millis(500), spellView);
            tt.setByX(moveX);
            tt.setByY(moveY);

            ScaleTransition st = new ScaleTransition(Duration.millis(500), spellView);
            st.setToX(1.0);
            st.setToY(1.0);

            RotateTransition rt = new RotateTransition(Duration.millis(500), spellView);
            rt.setByAngle(360);

            ParallelTransition pt = new ParallelTransition(tt, st, rt);
            pt.setOnFinished(ev -> {
                glassPane.getChildren().remove(spellView);
                targetGraveyard.addCardToTop(spellView);
            });
            pt.play();
        });
        pause.play();
    }

    private void advancePhase() {
        if (!isMyTurn()) return;
        cancelTargeting();
        gameEngine.nextPhase();
        session.send("NEXT_PHASE");
        onPhaseChangedLocally();
    }

    private void onPhaseChangedLocally() {
        attackConfirmed = false;
        selectedAttacker = null;
        cancelTargeting();
        syncBattlefield();
        updateControls();

        controlPanel.updatePhaseText(getLocalizedPhaseName(gameEngine.getCurrentPhase())
                + "\nХід: " + (isMyTurn() ? "ТВІЙ" : "суперника"));

        timeLeft = 120;
        controlPanel.updateTimerText(timeLeft);
        if (turnTimer != null) turnTimer.playFromStart();
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
                    battleLogPanel.addLogMessage("Час вийшов! Автоматичний пропуск фази.");
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