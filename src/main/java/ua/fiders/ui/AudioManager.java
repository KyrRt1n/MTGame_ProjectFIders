package ua.fiders.ui;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;

public class AudioManager {
    private static AudioManager instance;
    private MediaPlayer bgMediaPlayer;

    private AudioManager() {}

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    public void playBackgroundMusic() {
        try {
            URL resource = getClass().getResource("/music/bg_music.mp3");
            if (resource == null) {
                System.out.println("Файл музики не знайдено!");
                return;
            }

            Media sound = new Media(resource.toString());
            bgMediaPlayer = new MediaPlayer(sound);

            bgMediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            bgMediaPlayer.setVolume(0.4);

            bgMediaPlayer.play();
            System.out.println("[AudioManager] Фонова музика запущена.");
        } catch (Exception e) {
            System.out.println("Помилка відтворення музики: " + e.getMessage());
        }
    }

    public MediaPlayer getBgMediaPlayer() {
        return bgMediaPlayer;
    }
}