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

// View стану гравця. Відображає дані з моделі Player.
public class PlayerInfoPanel extends VBox {
    private final Player player;
    private final Label hpLabel;
    private final Label manaLabel;

    public PlayerInfoPanel(Player player) {
        this.player = player;

        setPadding(new Insets(15));
        setSpacing(10);
        setAlignment(Pos.CENTER_LEFT);
        setPrefWidth(200);
        setStyle("-fx-background-color: #1c1c1f; -fx-border-color: #3a3a3c; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label nameLabel = new Label(player.getName());
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        HBox hpBox = new HBox(10);
        hpBox.setAlignment(Pos.CENTER_LEFT);
        Label hpIcon = new Label("❤");
        hpIcon.setTextFill(Color.web("#ff4757"));
        hpIcon.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        hpLabel = new Label(String.valueOf(player.getHp()));
        hpLabel.setTextFill(Color.web("#ff4757"));
        hpLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        hpBox.getChildren().addAll(hpIcon, hpLabel);

        HBox manaBox = new HBox(10);
        manaBox.setAlignment(Pos.CENTER_LEFT);
        Label manaIcon = new Label("💧");
        manaIcon.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        manaLabel = new Label("5");
        manaLabel.setTextFill(Color.web("#3498db"));
        manaLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        manaBox.getChildren().addAll(manaIcon, manaLabel);

        getChildren().addAll(nameLabel, hpBox, manaBox);
    }

    public void updateHp() { hpLabel.setText(String.valueOf(player.getHp())); }
    public void updateMana(int currentMana) { manaLabel.setText(String.valueOf(currentMana)); }
}