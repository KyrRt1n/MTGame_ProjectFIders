package ua.fiders.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import ua.fiders.logic.*;
import ua.fiders.model.*;
import ua.fiders.model.cards.*;
import ua.fiders.model.effects.*;
import ua.fiders.model.enums.*;
import ua.fiders.ui.panels.BattlefieldPanel;
import ua.fiders.ui.panels.HandPanel;
import ua.fiders.ui.panels.PlayerInfoPanel;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import ua.fiders.ui.panels.CardView;
import ua.fiders.ui.panels.GameControlPanel;
import ua.fiders.ui.panels.OpponentHandPanel;
import ua.fiders.ui.panels.GraveyardPanel;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Bounds;
import javafx.scene.input.MouseButton;
import javafx.util.Duration;
import java.util.List;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;

import java.util.HashSet;
import java.util.Set;

// Головний Контролер інтерфейсу.
// Збирає всі панелі разом і забезпечує їхню взаємодію (наприклад, Drop карти на стіл).
public class GameController {
    private final BorderPane rootLayout;

    private OpponentHandPanel opponentHandPanel;
    private GraveyardPanel playerGraveyard;
    private GraveyardPanel opponentGraveyard;

    // UI Панелі
    private PlayerInfoPanel playerInfoPanel;
    private HandPanel playerHandPanel;
    private BattlefieldPanel battlefieldPanel;
    private GameControlPanel controlPanel;

    // Логіка фаз
    private final String[] phases = {"ПОЧАТОК ХОДУ", "ГОЛОВНА ФАЗА", "ФАЗА БОЮ", "ДРУГА ГОЛОВНА", "КІНЕЦЬ ХОДУ"};
    private int currentPhaseIndex = 0;

    private Player player1;
    private Player opponent;

    private GameEngine gameEngine;

    public GameController() {
        rootLayout = new BorderPane();
        rootLayout.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 100%, #301515 0%, #050505 85%);");

        initMockData();
        setupUI();
        setupGameListener();
        startBackgroundMusic();

        gameEngine.start();
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
                controlPanel.updatePhaseText(gameEngine.getCurrentPhase().name() + "\nХід: " + newActivePlayer.getName());
            }

            @Override
            public void onPermanentEnteredBattlefield(Permanent permanent) {
                // TODO: візуальне переміщення в Drag and Drop
            }
        });

    }

    private void setupUI() {
        playerInfoPanel = new PlayerInfoPanel(player1);
        playerHandPanel = new HandPanel();
        battlefieldPanel = new BattlefieldPanel();
        controlPanel = new GameControlPanel();

        opponentHandPanel = new OpponentHandPanel(5);
        playerGraveyard = new GraveyardPanel("ВІДБІЙ");
        opponentGraveyard = new GraveyardPanel("ВІДБІЙ ВОРОГА");

        controlPanel.updatePhaseText(phases[currentPhaseIndex]);
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

        BorderPane.setMargin(playerInfoPanel, new Insets(20, 0, 20, 20));
        rootLayout.setLeft(playerInfoPanel);

        BorderPane.setMargin(controlPanel, new Insets(20, 20, 20, 0));
        rootLayout.setRight(controlPanel);

        setupDragAndDrop();
    }

    private void initMockData() {
        player1 = new Player("Player");
        opponent = new Player("AI Bot");

        player1.setMaxMana(5);
        player1.setCurrentMana(5);

        class LandCard extends Card {
            public LandCard(String name) { super(name, Type.Land, 0, new HashSet<>(), "abc"); }
        }

        Set<CardKeywords> flyingKeyword = new HashSet<>();
        flyingKeyword.add(CardKeywords.Flying);

        Set<CardKeywords> strongKeywords = new HashSet<>();
        strongKeywords.add(CardKeywords.Lifelink);
        strongKeywords.add(CardKeywords.Trample);

        Card dragon = new CreatureCard("Black Dragon", 5, flyingKeyword, "abc", 5, 5);
        Card paladin = new CreatureCard("Holy Paladin", 4, strongKeywords, "abc", 4, 4);
        Card forest = new LandCard("Forest");
        Card fireball = new SpellCard("Fireball", 2, "imgPath", List.of(new DamageEnemyEffect(5)));

        player1.getHand().addAll(List.of(forest, paladin, dragon, fireball));

        gameEngine = new GameEngine(player1, opponent);
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
                    playerZone.getChildren().add(dragCardView);
                    dragCardView.setOnBoardMode();

                    dragCardView.setOnMouseClicked(mouseEvent -> {
                        if (mouseEvent.getButton() == MouseButton.SECONDARY)
                            discardCard(dragCardView);
                        else {
                            if (dragCardView.isTapped()) {
                                dragCardView.untap();
                                dragCardView.setHighlight(false);
                            } else {
                                dragCardView.tap();
                                dragCardView.setHighlight(true);
                            }
                        }
                    });

                    System.out.println("Успішно зіграно: " + playedCard.getName());
                    success = true;

                } else
                    System.out.println("Неможливо зіграти " + playedCard.getName());
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

        String currentPhaseName = gameEngine.getCurrentPhase().name();
        controlPanel.updatePhaseText(currentPhaseName);

        System.out.println("Гру переведено у фазу: " + currentPhaseName);
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

    private void startBackgroundMusic() {
        try {
            URL resource = getClass().getResource("/music/bg_music.mp3");
            if (resource == null) {
                System.out.println("Файл музики не знайдено!");
                return;
            }

            Media sound = new Media(resource.toString());
            MediaPlayer mediaPlayer = new MediaPlayer(sound);

            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.setVolume(0.4);

            mediaPlayer.play();
        } catch (Exception e) {
            System.out.println("Помилка відтворення музики: " + e.getMessage());
        }
    }
}