package ua.fiders.ui.panels;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import ua.fiders.model.Player;

public class OpponentInfoPanel extends VBox {
    private final Player player;
    private final Label hpLabel;

    public OpponentInfoPanel(Player player) {
        this.player = player;

        setPadding(new Insets(10));
        setSpacing(6);
        setAlignment(Pos.CENTER_LEFT);
        setPrefWidth(200);
        setStyle("-fx-background-color: #1c1c1f; -fx-border-color: #3a3a3c; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label nameLabel = new Label(player.getName());
        nameLabel.setTextFill(Color.web("#aaaaaa"));
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        HBox hpBox = new HBox(8);
        hpBox.setAlignment(Pos.CENTER_LEFT);

        Label hpIcon = new Label("❤");
        hpIcon.setTextFill(Color.web("#ff4757"));
        hpIcon.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        hpLabel = new Label(String.valueOf(player.getHp()));
        hpLabel.setTextFill(Color.web("#ff4757"));
        hpLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));

        hpBox.getChildren().addAll(hpIcon, hpLabel);
        getChildren().addAll(nameLabel, hpBox);
    }

    public void updateHp() { hpLabel.setText(String.valueOf(player.getHp())); }
}