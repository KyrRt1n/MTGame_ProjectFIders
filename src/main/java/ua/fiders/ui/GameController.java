package ua.fiders.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import ua.fiders.logic.*;
import ua.fiders.model.*;
import ua.fiders.model.cards.*;
import ua.fiders.model.enums.*;
import ua.fiders.ui.panels.*;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Bounds;
import javafx.scene.input.MouseButton;
import javafx.util.Duration;

import ua.fiders.data.DeckLoader;

import java.util.Collections;
import java.util.List;

// Головний Контролер інтерфейсу.
// Збирає всі панелі разом і забезпечує їхню взаємодію.
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


    private Player player1;
    private Player opponent;

    private GameEngine gameEngine;

    public GameController() {
        rootLayout = new BorderPane();
        rootLayout.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 100%, #301515 0%, #050505 85%);");

        initGame(); // Ініціалізація рушія та колод напряму
        setupUI();
        setupGameListener();

        gameEngine.start();
    }

    private void initGame() {
        player1 = new Player("Player");
        opponent = new Player("Opponent");

        // Завантажуємо колоди через DeckLoader
        DeckLoader deckLoader = new DeckLoader();

        // ВКАЖИ ТУТ ПРАВИЛЬНИЙ ШЛЯХ ДО ТВОГО JSON ФАЙЛУ КОЛОДИ У RESOURCES!
        String deckPath = "/decks/Green.json";

        List<Card> humanDeck = deckLoader.loadDeck(deckPath);
        List<Card> opponentDeck = deckLoader.loadDeck(deckPath);

        // Обов'язково перемішуємо колоди, щоб вони не були однаковими кожну гру
        Collections.shuffle(humanDeck);
        Collections.shuffle(opponentDeck);

        player1.setDeck(humanDeck);
        opponent.setDeck(opponentDeck);

        // Роздаємо стартові руки (7 карт)
        dealStartingHand(player1, 7);
        dealStartingHand(opponent, 7);

        gameEngine = new GameEngine(player1, opponent);
    }

    private void dealStartingHand(Player player, int count) {
        for (int i = 0; i < count; i++) {
            Card drawn = player.drawnCard();
            if (drawn != null) {
                player.getHand().add(drawn);
            }
        }
    }

    private void setupGameListener() {
        gameEngine.setListener(new GameListener() {
            @Override
            public void onManaChanged(Player player) {
                if (player == player1)
                    playerInfoPanel.updateMana(player.getCurrentMana());
            }

            @Override
            public void onTurnChanged(Player newActivePlayer) {
                controlPanel.updatePhaseText(getLocalizedPhaseName(gameEngine.getCurrentPhase()) + "\nХід: " + newActivePlayer.getName());
            }

            @Override
            public void onPermanentEnteredBattlefield(Permanent permanent) {
                CardView boardCardView = new CardView(permanent.getBaseCard());
                boardCardView.setOnBoardMode();

                boardCardView.setOnMouseClicked(mouseEvent -> {
                    if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                        discardCard(boardCardView);
                    } else {
                        if (boardCardView.isTapped()) {
                            boardCardView.untap();
                            boardCardView.setHighlight(false);
                        } else {
                            boardCardView.tap();
                            boardCardView.setHighlight(true);
                        }
                    }
                });

                if (permanent.getController() == player1) {
                    battlefieldPanel.getPlayerZone().getChildren().add(boardCardView);
                } else {
                    battlefieldPanel.getOpponentZone().getChildren().add(boardCardView);
                }
            }

            @Override
            public void onHpChanged(Player player){
                opponentInfoPanel.updateHp();
                playerInfoPanel.updateHp();
            }

            @Override
            public void onHandUpdated(Player player) {
                if (player == player1) {
                    playerHandPanel.updateHand(player.getHand());
                }
            }
        });

    }

    private void setupUI() {
        playerInfoPanel = new PlayerInfoPanel(player1);
        opponentInfoPanel = new OpponentInfoPanel(opponent);
        playerHandPanel = new HandPanel();
        battlefieldPanel = new BattlefieldPanel();
        controlPanel = new GameControlPanel();

        opponentHandPanel = new OpponentHandPanel(5);
        playerGraveyard = new GraveyardPanel("ВІДБІЙ");
        opponentGraveyard = new GraveyardPanel("ВІДБІЙ ВОРОГА");

        controlPanel.updatePhaseText(getLocalizedPhaseName(gameEngine.getCurrentPhase()));
        controlPanel.setNextPhaseAction(this::advancePhase);
        playerHandPanel.updateHand(player1.getHand());

        // Спаковуємо стіл та кладовища разом
        VBox graveyardsBox = new VBox(20);
        graveyardsBox.setAlignment(Pos.CENTER);
        graveyardsBox.getChildren().addAll(opponentGraveyard, playerGraveyard);

        HBox centerLayout = new HBox(20);
        centerLayout.setAlignment(Pos.CENTER);
        HBox.setHgrow(battlefieldPanel, Priority.ALWAYS); // Стіл розтягується
        centerLayout.getChildren().addAll(battlefieldPanel, graveyardsBox);

        // Розставляємо на головному екрані
        rootLayout.setTop(opponentHandPanel);
        rootLayout.setCenter(centerLayout);
        rootLayout.setBottom(playerHandPanel);

        VBox leftPanel = new VBox(20);
        leftPanel.getChildren().addAll(opponentInfoPanel, playerInfoPanel);
        BorderPane.setMargin(leftPanel, new Insets(20, 0, 20, 20));
        rootLayout.setLeft(leftPanel);

        BorderPane.setMargin(controlPanel, new Insets(20, 20, 20, 0));
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

                if (gameEngine.playCard(playedCard)) {
                    playerHandPanel.getChildren().remove(dragCardView);

                    if (playedCard.getType() == Type.Sorcery) {
                        dragCardView.setOnBoardMode();
                        playerGraveyard.addCardToTop(dragCardView);
                    }

                    System.out.println("Успішно зіграно: " + playedCard.getName());
                    success = true;
                } else {
                    System.out.println("Неможливо зіграти " + playedCard.getName());
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
        gameEngine.nextPhase();

        String localizedPhaseName = getLocalizedPhaseName(gameEngine.getCurrentPhase());
        controlPanel.updatePhaseText(localizedPhaseName);

        System.out.println("Гру переведено у фазу: " + localizedPhaseName);
    }

    /**
     * Анімує переміщення карти зі столу у відбій гравця
     */
    private void discardCard(CardView cardView) {
        // Отримуємо абсолютні координати карти та кладовища на екрані
        Bounds cardBounds = cardView.localToScene(cardView.getBoundsInLocal());
        Bounds gyBounds = playerGraveyard.localToScene(playerGraveyard.getBoundsInLocal());

        double moveX = gyBounds.getCenterX() - cardBounds.getCenterX();
        double moveY = gyBounds.getCenterY() - cardBounds.getCenterY();

        // Анімація польоту
        TranslateTransition tt = new TranslateTransition(Duration.millis(400), cardView);
        tt.setByX(moveX);
        tt.setByY(moveY);

        // Анімація обертання (ефект того, що карта відлітає)
        RotateTransition rt = new RotateTransition(Duration.millis(400), cardView);
        rt.setByAngle(360);

        // Запускаємо їх паралельно
        ParallelTransition pt = new ParallelTransition(tt, rt);
        pt.setOnFinished(e -> {
            // Коли карта долетіла, фізично переміщуємо її в ієрархії JavaFX
            battlefieldPanel.getPlayerZone().getChildren().remove(cardView);
            playerGraveyard.addCardToTop(cardView);
        });

        pt.play();
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
}