package ua.fiders.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import ua.fiders.model.Player;
import ua.fiders.model.cards.Card;
import ua.fiders.model.enums.CardKeywords;
import ua.fiders.model.cards.CreatureCard;
import ua.fiders.model.enums.Type;
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

    public GameController() {
        rootLayout = new BorderPane();
        rootLayout.setStyle("-fx-background-color: #151517;");

        initMockData();
        setupUI();
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

        class LandCard extends Card {
            public LandCard(String name) { super(name, Type.Land, 0, new HashSet<>()); }
        }

        Set<CardKeywords> flyingKeyword = new HashSet<>();
        flyingKeyword.add(CardKeywords.Flying);

        Set<CardKeywords> strongKeywords = new HashSet<>();
        strongKeywords.add(CardKeywords.Lifelink);
        strongKeywords.add(CardKeywords.Trample);

        Card dragon = new CreatureCard("Black Dragon", 5, flyingKeyword, 5, 5);
        Card goblin = new CreatureCard("Goblin Scout", 1, null, 2, 1);
        Card paladin = new CreatureCard("Holy Paladin", 4, strongKeywords, 4, 4);
        Card forest = new LandCard("Forest");
        Card mountain = new LandCard("Mountain");

        player1.getHand().addAll(List.of(forest, mountain, goblin, paladin, dragon));
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
                // Візуальне переміщення
                playerHandPanel.getChildren().remove(dragCardView);
                playerZone.getChildren().add(dragCardView);

                dragCardView.setOnBoardMode(); // Вимкнення Hover ефектів

                // Логіка кліку по карті, яка вже лежить на столі
                dragCardView.setOnMouseClicked(mouseEvent -> {
                    // Якщо клік правою кнопкою миші - відправляємо карту у відбій
                    if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                        discardCard(dragCardView);
                    } else {
                        // Якщо лівою - повертаємо (Tap/Untap)
                        if (dragCardView.isTapped()) {
                            dragCardView.untap();
                            dragCardView.setHighlight(false);
                        } else {
                            dragCardView.tap();
                            dragCardView.setHighlight(true);
                        }
                    }
                });

                // Синхронізація з моделлю
                Card playedCard = dragCardView.getCard();
                player1.getHand().remove(playedCard);
                System.out.println("Викладено на стіл: " + playedCard.getName());

                success = true;
            }

            event.setDropCompleted(success);
            event.consume();
        });
    }

    /**
     * Логіка перемикання ігрових фаз
     */
    private void advancePhase() {
        // Переходимо до наступної фази, або повертаємось на початок
        currentPhaseIndex = (currentPhaseIndex + 1) % phases.length;
        String nextPhase = phases[currentPhaseIndex];

        controlPanel.updatePhaseText(nextPhase);

        // gameEngine.nextPhase();

        System.out.println("Гру переведено у фазу: " + nextPhase);
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
}