package ua.fiders.ui.panels;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class GameControlPanel extends VBox {

    private final Label phaseLabel;
    private final Label timerLabel;
    private final Button nextPhaseBtn;
    private final Button mulliganBtn;

    public GameControlPanel() {
        setSpacing(15);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(20));
        setPrefWidth(220);

        setStyle("-fx-background-color: #1c1c1f; " +
                "-fx-border-color: #3a3a3c; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10;");

        Label titleLabel = new Label("ПОТОЧНА ФАЗА:");
        titleLabel.setTextFill(Color.LIGHTGRAY);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        phaseLabel = new Label("");
        phaseLabel.setTextFill(Color.web("#f1c40f"));
        phaseLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        phaseLabel.setWrapText(true);
        phaseLabel.setAlignment(Pos.CENTER);

        timerLabel = new Label("Час: 120");
        timerLabel.setTextFill(Color.web("#ff4757"));
        timerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        nextPhaseBtn = new Button("НАСТУПНА ФАЗА");
        nextPhaseBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 20 10 20; -fx-background-radius: 5;");
        nextPhaseBtn.setMaxWidth(Double.MAX_VALUE);

        mulliganBtn = new Button("ПЕРЕЗДАЧА (-1)");
        mulliganBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 20 10 20; -fx-background-radius: 5;");
        mulliganBtn.setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(titleLabel, phaseLabel, timerLabel, nextPhaseBtn, mulliganBtn);
    }

    public void updatePhaseText(String text) {
        phaseLabel.setText(text);
    }

    public void updateTimerText(int seconds) {
        timerLabel.setText("Час: " + seconds);
    }

    public void setNextPhaseAction(Runnable action) {
        nextPhaseBtn.setOnAction(e -> action.run());
    }

    public void setMulliganAction(Runnable action) {
        mulliganBtn.setOnAction(e -> action.run());
    }

    public void setMulliganVisible(boolean visible) {
        mulliganBtn.setVisible(visible);
        mulliganBtn.setManaged(visible); // Щоб кнопка не займала місце, коли прихована
    }
}