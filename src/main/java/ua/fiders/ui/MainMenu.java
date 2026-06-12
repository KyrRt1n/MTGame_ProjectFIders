package ua.fiders.ui;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class MainMenu extends StackPane {

    private final VBox mainBox;
    private final VBox settingsBox;
    private final Slider volumeSlider;

    private boolean isUkr = true;

    private final Button playBtn;
    private final Button settingsBtn;
    private final Button langBtn;
    private final Button backBtn;
    private final CheckBox fullscreenCheck;
    private final Label volLabel;

    public MainMenu(Runnable onPlay, Stage stage) {
        setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 100%, #301515 0%, #050505 85%);");

        mainBox = new VBox(20);
        settingsBox = new VBox(20);
        mainBox.setAlignment(Pos.CENTER);

        Label title = new Label("JAVA THE GATHERING");
        title.setTextFill(Color.web("#f1c40f"));
        title.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        title.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(241, 196, 15, 0.4), 10, 0.0, 0, 0);");

        playBtn = createStyledButton("Грати (Хост/Приєднатись)");
        playBtn.setOnAction(e -> onPlay.run());

        settingsBtn = createStyledButton("Налаштування");
        settingsBtn.setOnAction(e -> {
            mainBox.setVisible(false);
            settingsBox.setVisible(true);
        });

        langBtn = createStyledButton("🌐 Мова: УКР");
        langBtn.setOnAction(e -> toggleLanguage());

        mainBox.getChildren().addAll(title, playBtn, settingsBtn, langBtn);

        settingsBox.setAlignment(Pos.CENTER);
        settingsBox.setVisible(false);
        settingsBox.setMaxSize(400, 300);
        settingsBox.setStyle("-fx-background-color: #1c1c1f; -fx-border-color: #3a3a3c; " +
                "-fx-border-width: 3; -fx-border-radius: 15; -fx-background-radius: 15; -fx-padding: 30;");

        Label settingsTitle = new Label("НАЛАШТУВАННЯ");
        settingsTitle.setTextFill(Color.WHITE);
        settingsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        VBox volBox = new VBox(5);
        volBox.setAlignment(Pos.CENTER);
        volLabel = new Label("Гучність:");
        volLabel.setTextFill(Color.LIGHTGRAY);
        volLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        volumeSlider = new Slider(0, 100, 40);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.setShowTickLabels(true);
        volBox.getChildren().addAll(volLabel, volumeSlider);

        fullscreenCheck = new CheckBox("Повноекранний режим");
        fullscreenCheck.setTextFill(Color.LIGHTGRAY);
        fullscreenCheck.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        fullscreenCheck.setSelected(stage.isFullScreen());

        fullscreenCheck.setOnAction(e -> stage.setFullScreen(fullscreenCheck.isSelected()));

        backBtn = createStyledButton("Назад");
        backBtn.setOnAction(e -> {
            settingsBox.setVisible(false);
            mainBox.setVisible(true);
        });

        settingsBox.getChildren().addAll(settingsTitle, volBox, fullscreenCheck, backBtn);

        getChildren().addAll(mainBox, settingsBox);
    }

    /**
     * Логіка швидкого перемикання мов (Укр/Англ)
     */
    private void toggleLanguage() {
        isUkr = !isUkr;
        if (isUkr) {
            playBtn.setText("Грати (Хост/Приєднатись)");
            settingsBtn.setText("Налаштування");
            langBtn.setText("🌐 Мова: УКР");
            backBtn.setText("Назад");
            fullscreenCheck.setText("Повноекранний режим");
            volLabel.setText("Гучність:");
        } else {
            playBtn.setText("Play (Host/Join)");
            settingsBtn.setText("Settings");
            langBtn.setText("🌐 Language: ENG");
            backBtn.setText("Back");
            fullscreenCheck.setText("Fullscreen mode");
            volLabel.setText("Volume:");
        }
    }

    /**
     * Фабричний метод для створення однакових стилізованих кнопок
     */
    private Button createStyledButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(280);

        String defaultStyle = "-fx-background-color: #2c2c2e; -fx-text-fill: white; " +
                "-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 12; " +
                "-fx-border-color: #3a3a3c; -fx-border-width: 2; " +
                "-fx-border-radius: 8; -fx-background-radius: 8;";

        String hoverStyle = "-fx-background-color: #3498db; -fx-text-fill: white; " +
                "-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 12; " +
                "-fx-border-color: #2980b9; -fx-border-width: 2; " +
                "-fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;";

        btn.setStyle(defaultStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(defaultStyle));

        return btn;
    }

    public Slider getVolumeSlider() {
        return volumeSlider;
    }
}