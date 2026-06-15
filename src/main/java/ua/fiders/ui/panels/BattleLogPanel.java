package ua.fiders.ui.panels;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.function.Consumer;

public class BattleLogPanel extends VBox {

    private final ListView<String> logView;
    private final TextField inputField;

    public BattleLogPanel() {
        setSpacing(10);
        setPadding(new Insets(10));
        setPrefWidth(220);

        setStyle("-fx-background-color: #1c1c1f; " +
                "-fx-border-color: #3a3a3c; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10;");

        Label titleLabel = new Label("ЖУРНАЛ БОЮ");
        titleLabel.setTextFill(Color.LIGHTGRAY);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        logView = new ListView<>();
        logView.setStyle("-fx-background-color: #2c2c2e; " +
                "-fx-control-inner-background: #2c2c2e;");

        logView.setCellFactory(list -> new ListCell<String>() {
            private final Label label = new Label();

            {
                // перенос по словах
                label.setWrapText(true);
                label.setTextFill(Color.WHITE);

                // Важливо: біндимо саме prefWidth, а не maxWidth
                label.prefWidthProperty().bind(list.widthProperty().subtract(15));

                // Змушуємо Label розраховувати свою висоту на основі кількості рядків
                label.setMinHeight(Region.USE_PREF_SIZE);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    label.setText(item);
                    setGraphic(label);
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 4 0 4 0;");
                }
            }
        });

        VBox.setVgrow(logView, Priority.ALWAYS);

        inputField = new TextField();
        inputField.setPromptText("Написати в чат...");
        inputField.setStyle("-fx-background-color: #3a3a3c; " +
                "-fx-text-fill: white; " +
                "-fx-prompt-text-fill: gray; " +
                "-fx-background-radius: 5;");

        getChildren().addAll(titleLabel, logView, inputField);
    }

    /**
     * Додає нове повідомлення в лог і автоматично прокручує вниз.
     * Platform.runLater захищає від помилок, якщо повідомлення прийде з мережевого потоку.
     */
    public void addLogMessage(String message) {
        Platform.runLater(() -> {
            logView.getItems().add(message);
            logView.scrollTo(logView.getItems().size() - 1);
        });
    }

    /**
     * Встановлює дію, яка відбудеться, коли гравець натисне Enter у полі вводу
     */
    public void setOnMessageSent(Consumer<String> action) {
        inputField.setOnAction(e -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty()) {
                action.accept(text);
                inputField.clear();
            }
        });
    }
}