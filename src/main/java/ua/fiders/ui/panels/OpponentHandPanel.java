package ua.fiders.ui.panels;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

// View руки опонента (сорочки карт)
public class OpponentHandPanel extends HBox {

    public OpponentHandPanel(int cardCount) {
        super(-30);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(5));
        setMinHeight(150);

        for (int i = 0; i < cardCount; i++) {
            getChildren().add(createCardBack());
        }
    }

    private StackPane createCardBack() {
        StackPane back = new StackPane();
        // Робимо карти трохи меншими, щоб вони не забирали багато місця
        back.setPrefSize(100, 140);

        back.setStyle("-fx-background-color: #2c3e50; " +
                "-fx-border-color: #34495e; " +
                "-fx-border-width: 3; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8;");

        Label logo = new Label("ENEMY\nCARD");
        logo.setTextFill(Color.web("#7f8c8d"));
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        logo.setAlignment(Pos.CENTER);

        back.getChildren().add(logo);
        return back;
    }

    public void updateHandSize(int cardCount) {
        getChildren().clear();
        for (int i = 0; i < cardCount; i++) {
            getChildren().add(createCardBack());
        }
    }
}