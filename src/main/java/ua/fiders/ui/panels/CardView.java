package ua.fiders.ui.panels;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import ua.fiders.model.cards.Card;
import ua.fiders.model.cards.CardKeywords;
import ua.fiders.model.cards.CreatureCard;
import javafx.animation.RotateTransition;
import javafx.util.Duration;

// Основний візуальний компонент карти (View).
// Інкапсулює стилі та ефекти наведення (Hover).
public class CardView extends StackPane {
    private final Card card;
    private boolean isTapped = false;

    private static final String DEFAULT_STYLE =
            "-fx-background-color: #1c1c1f; -fx-border-color: #3a3a3c; " +
                    "-fx-border-width: 3; -fx-border-radius: 12; -fx-background-radius: 12;";

    private static final String HOVER_STYLE =
            "-fx-background-color: #242428; -fx-border-color: #f1c40f; " +
                    "-fx-border-width: 3; -fx-border-radius: 12; -fx-background-radius: 12; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(241, 196, 15, 0.6), 15, 0.4, 0, 0);";

    public CardView(Card card) {
        this.card = card;

        setPrefSize(140, 200);
        setMinSize(140, 200);
        setMaxSize(140, 200);
        setStyle(DEFAULT_STYLE);

        VBox contentLayout = new VBox(8);
        contentLayout.setPadding(new Insets(10));
        contentLayout.setAlignment(Pos.TOP_CENTER);

        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(card.getName());
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(90);

        Label manaLabel = new Label(String.valueOf(card.getManaCost()));
        manaLabel.setTextFill(Color.web("#3498db"));
        manaLabel.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        manaLabel.setStyle("-fx-background-color: #2c2c2e; -fx-padding: 2 6 2 6; -fx-background-radius: 20;");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        topBar.getChildren().addAll(nameLabel, topSpacer, manaLabel);

        Label typeLabel = new Label(card.getType().toString());
        typeLabel.setTextFill(Color.LIGHTGRAY);
        typeLabel.setFont(Font.font("Arial", 10));
        typeLabel.setStyle("-fx-background-color: #2c2c2e; -fx-padding: 2 8 2 8; -fx-background-radius: 5;");

        FlowPane keywordsPane = new FlowPane(4, 4);
        keywordsPane.setAlignment(Pos.CENTER);

        for (CardKeywords keyword : card.getKeywords()) {
            Label kwLabel = new Label(keyword.name());
            kwLabel.setFont(Font.font("Arial", FontWeight.BOLD, 9));
            kwLabel.setTextFill(Color.web("#f1c40f"));
            kwLabel.setStyle("-fx-background-color: #252528; -fx-padding: 2 5 2 5; -fx-border-color: #f1c40f; -fx-border-radius: 3; -fx-background-radius: 3;");
            keywordsPane.getChildren().add(kwLabel);
        }

        Region middleSpacer = new Region();
        VBox.setVgrow(middleSpacer, Priority.ALWAYS);

        HBox bottomBar = new HBox();
        bottomBar.setAlignment(Pos.BOTTOM_RIGHT);

        if (card instanceof CreatureCard creatureCard) {
            String statsText = creatureCard.getAttack() + " / " + creatureCard.getHp();
            Label statsLabel = new Label(statsText);
            statsLabel.setTextFill(Color.web("#ff4757"));
            statsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            statsLabel.setStyle("-fx-background-color: #2c2c2e; -fx-padding: 3 8 3 8; -fx-background-radius: 5; -fx-border-color: #ff4757; -fx-border-radius: 5;");
            bottomBar.getChildren().add(statsLabel);
        }

        contentLayout.getChildren().addAll(topBar, typeLabel, keywordsPane, middleSpacer, bottomBar);
        getChildren().add(contentLayout);
        setupHoverEffects();
    }

    private void setupHoverEffects() {
        setOnMouseEntered(event -> {
            setStyle(HOVER_STYLE);
            setScaleX(1.1);
            setScaleY(1.1);
            setViewOrder(-1.0); // Виводить карту на передній план
        });

        setOnMouseExited(event -> {
            setStyle(DEFAULT_STYLE);
            setScaleX(1.0);
            setScaleY(1.0);
            setViewOrder(0.0);
        });
    }

    public Card getCard() { return card; }

    // Змінює стан карти, коли вона викладається на стіл
    public void setOnBoardMode() {
        setOnMouseEntered(null);
        setOnMouseExited(null);
        setScaleX(1.0);
        setScaleY(1.0);
        setStyle(DEFAULT_STYLE);
        setOnDragDetected(null);
    }

    public boolean isTapped() {
        return isTapped;
    }

    /**
     * Плавний поворот карти на 90 градусів (імітація виснаження/атаки)
     */
    public void tap() {
        if (isTapped) return;
        RotateTransition rt = new RotateTransition(Duration.millis(200), this);
        rt.setToAngle(90);
        rt.play();
        isTapped = true;
    }

    /**
     * Повернення карти у вертикальне положення
     */
    public void untap() {
        if (!isTapped) return;
        RotateTransition rt = new RotateTransition(Duration.millis(200), this);
        rt.setToAngle(0);
        rt.play();
        isTapped = false;
    }

    /**
     * Візуальне виділення карти (наприклад, коли вона обрана для атаки)
     */
    public void setHighlight(boolean active) {
        if (active) {
            // Додаємо червоне світіння та рамку
            setStyle("-fx-background-color: #1c1c1f; " +
                    "-fx-border-color: #ff4757; " +
                    "-fx-border-width: 3; " +
                    "-fx-border-radius: 12; " +
                    "-fx-background-radius: 12; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(255, 71, 87, 0.8), 15, 0.5, 0, 0);");
        } else {
            setStyle(DEFAULT_STYLE); // Повертаємо стандартний вигляд
        }
    }
}