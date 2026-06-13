package ua.fiders.ui.panels;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import ua.fiders.model.cards.Card;
import ua.fiders.model.enums.CardKeywords;
import ua.fiders.model.cards.CreatureCard;
import javafx.animation.RotateTransition;
import javafx.util.Duration;

import java.io.InputStream;

// Основний візуальний компонент карти (View).
// Інкапсулює стилі, картинки, опис та ефекти наведення (Hover).
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

        VBox contentLayout = new VBox(4);
        contentLayout.setPadding(new Insets(8));
        contentLayout.setAlignment(Pos.TOP_CENTER);

        // Ім'я + Мана
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(card.getName());
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(90);

        Label manaLabel = new Label(String.valueOf(card.getManaCost()));
        manaLabel.setTextFill(Color.web("#3498db"));
        manaLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        manaLabel.setStyle("-fx-background-color: #2c2c2e; -fx-padding: 2 5 2 5; -fx-background-radius: 20;");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        topBar.getChildren().addAll(nameLabel, topSpacer, manaLabel);

        // Картинка
        ImageView imageView = new ImageView();
        try {
            InputStream imgStream = getClass().getResourceAsStream(card.getImgPath());
            if (imgStream != null) {
                imageView.setImage(new Image(imgStream));
                imageView.setFitWidth(118);
                imageView.setFitHeight(65);
                imageView.setPreserveRatio(false);
            }
        } catch (Exception e) {
            System.out.println("Не знайдено арт для карти: " + card.getName());
        }

        // ТИП ТА КЛЮЧОВІ СЛОВА
        Label typeLabel = new Label(card.getType().toString());
        typeLabel.setTextFill(Color.LIGHTGRAY);
        typeLabel.setFont(Font.font("Arial", 9));
        typeLabel.setStyle("-fx-background-color: #2c2c2e; -fx-padding: 1 6 1 6; -fx-background-radius: 3;");

        FlowPane keywordsPane = new FlowPane(2, 2);
        keywordsPane.setAlignment(Pos.CENTER);
        for (CardKeywords keyword : card.getKeywords()) {
            Label kwLabel = new Label(keyword.name());
            kwLabel.setFont(Font.font("Arial", FontWeight.BOLD, 8));
            kwLabel.setTextFill(Color.web("#f1c40f"));
            kwLabel.setStyle("-fx-background-color: #252528; -fx-padding: 1 4 1 4; -fx-border-color: #f1c40f; -fx-border-radius: 3; -fx-background-radius: 3;");
            keywordsPane.getChildren().add(kwLabel);
        }

        // ОПИС КАРТИ
        Label descLabel = new Label(card.getDescription());
        descLabel.setTextFill(Color.web("#aaaaaa"));
        descLabel.setFont(Font.font("Arial", FontPosture.ITALIC, 8.5));
        descLabel.setWrapText(true);
        descLabel.setTextAlignment(TextAlignment.CENTER);
        descLabel.setAlignment(Pos.CENTER);
        descLabel.setMinHeight(25); // Щоб текст не стрибав

        Region middleSpacer = new Region();
        VBox.setVgrow(middleSpacer, Priority.ALWAYS);

        // НИЖНІЙ БЛОК (Статистика)
        HBox bottomBar = new HBox();
        bottomBar.setAlignment(Pos.BOTTOM_RIGHT);

        if (card instanceof CreatureCard creatureCard) {
            String statsText = creatureCard.getAttack() + " / " + creatureCard.getHp();
            Label statsLabel = new Label(statsText);
            statsLabel.setTextFill(Color.web("#ff4757"));
            statsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
            statsLabel.setStyle("-fx-background-color: #2c2c2e; -fx-padding: 2 6 2 6; -fx-background-radius: 5; -fx-border-color: #ff4757; -fx-border-radius: 5;");
            bottomBar.getChildren().add(statsLabel);
        }

        contentLayout.getChildren().addAll(topBar, imageView, typeLabel, keywordsPane, descLabel, middleSpacer, bottomBar);
        getChildren().add(contentLayout);

        setupHoverEffects();
        setupTooltip();
    }

    private void setupTooltip() {
        // Спливаюча  підказка, щоб зручно читати дрібний текст
        String tooltipText = card.getName().toUpperCase() + " (Мана: " + card.getManaCost() + ")\n";
        if (card.getDescription() != null && !card.getDescription().isBlank()) {
            tooltipText += "\n" + card.getDescription();
        }

        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(250);
        tooltip.setShowDelay(Duration.millis(400)); // Через 0.4 сек після наведення
        tooltip.setStyle("-fx-font-size: 14px; -fx-background-color: rgba(20, 20, 25, 0.95); -fx-text-fill: #f1c40f; -fx-border-color: #3a3a3c; -fx-border-width: 2;");

        Tooltip.install(this, tooltip);
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