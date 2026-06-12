package ua.fiders.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import ua.fiders.network.NetworkLauncher;

public class MainApp extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        Image appIcon = new Image(getClass().getResourceAsStream("/icon.png"));
        primaryStage.getIcons().add(appIcon);
        primaryStage.setTitle("Java the Gathering");

        AudioManager.getInstance().playBackgroundMusic();

        MainMenu mainMenu = new MainMenu(this::startGame, primaryStage);
        Scene menuScene = new Scene(mainMenu, 1200, 800);

        String cssPath = getClass().getResource("/css/style.css").toExternalForm();
        menuScene.getStylesheets().add(cssPath);

        MediaPlayer player = AudioManager.getInstance().getBgMediaPlayer();
        if (player != null) {
            mainMenu.getVolumeSlider().setValue(player.getVolume() * 100);
            mainMenu.getVolumeSlider().valueProperty().addListener((obs, oldVal, newVal) -> {
                player.setVolume(newVal.doubleValue() / 100.0);
            });
        }

        primaryStage.setScene(menuScene);
        primaryStage.show();
    }

    /**
     * Цей метод викликається лише тоді, коли користувач тисне "Грати" в меню
     */
    private void startGame() {
        System.out.println("[MainApp] Ініціалізація мережевої гри...");

        NetworkLauncher.launch((session, isHost, seed) -> {
            GameController controller = new GameController(session, isHost, seed);
            Scene gameScene = new Scene(controller.getRootLayout(), 1200, 800);
            gameScene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            boolean isFullScreen = primaryStage.isFullScreen();
            primaryStage.setScene(gameScene);

            if (isFullScreen)
                primaryStage.setFullScreen(true);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}