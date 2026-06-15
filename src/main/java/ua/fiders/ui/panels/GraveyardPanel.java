package ua.fiders.ui.panels;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

// View кладовища (відбою)
public class GraveyardPanel extends StackPane {

    public GraveyardPanel(String title) {
        setPrefSize(140, 200);
        setMinSize(140, 200);
        setMaxSize(140, 200);

        // Стиль порожнього кладовища (пунктирна рамка)
        setStyle("-fx-background-color: rgba(0, 0, 0, 0.4); " +
                "-fx-border-color: #7f8c8d; " +
                "-fx-border-width: 2; " +
                "-fx-border-style: dashed; " +
                "-fx-border-radius: 12; " +
                "-fx-background-radius: 12;");

        Label titleLabel = new Label(title);
        titleLabel.setTextFill(Color.GRAY);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        StackPane.setAlignment(titleLabel, Pos.CENTER);

        getChildren().add(titleLabel);
    }

    /**
     * Додає карту у відбій (показує її зверху)
     */
    public void addCardToTop(CardView cardView) {
        // Скидаємо зміщення та повороти, які залишилися після анімації польоту/атаки
        cardView.setTranslateX(0);
        cardView.setTranslateY(0);
        cardView.setRotate(0);
        cardView.setHighlight(null);
        cardView.setOpacity(0.6); // Робимо "мертву" карту трохи тьмянішою
        cardView.setOnMouseClicked(null);
        cardView.setOnDragDetected(null);

        // Очищаємо кладовище
        // і кладемо нову карту
        getChildren().clear();
        getChildren().add(cardView);

        // Змінюємо стиль кладовища на суцільний, бо воно більше не порожнє
        setStyle("-fx-background-color: transparent;");
    }
}