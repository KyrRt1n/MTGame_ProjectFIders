package ua.fiders.ui.panels;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class DeckPanel extends StackPane {

    private final Label countLabel;

    public DeckPanel(int initialCount) {
        setPrefSize(100, 140);
        setMinSize(100, 140);
        setMaxSize(100, 140);

        setStyle("-fx-background-color: #2c3e50; " +
                "-fx-border-color: #3a3a3c; " +
                "-fx-border-width: 3; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 10, 0, 0, 0);");

        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);

        Label title = new Label("КОЛОДА");
        title.setTextFill(Color.web("#7f8c8d"));
        title.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        countLabel = new Label(String.valueOf(initialCount));
        countLabel.setTextFill(Color.WHITE);
        countLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        box.getChildren().addAll(title, countLabel);
        getChildren().add(box);
    }

    public void updateCount(int count) {
        countLabel.setText(String.valueOf(count));
    }
}