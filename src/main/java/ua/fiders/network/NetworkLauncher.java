package ua.fiders.network;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;
import java.util.Random;

public class NetworkLauncher {

    public static final int PORT = 8887;

    public interface GameStarter {
        void start(NetworkSession session, boolean isHost, long seed);
    }

    public static void launch(GameStarter starter) {
        ChoiceDialog<String> modeDialog = new ChoiceDialog<>("Створити гру", "Створити гру", "Приєднатися");
        modeDialog.setTitle("Мережева гра");
        modeDialog.setHeaderText("Оберіть режим");
        Optional<String> mode = modeDialog.showAndWait();
        if (mode.isEmpty()) {
            return;
        }

        if (mode.get().equals("Створити гру")) {
            hostGame(starter);
        } else {
            joinGame(starter);
        }
    }

    private static void hostGame(GameStarter starter) {
        NetworkSession session = NetworkSession.host(PORT);

        Alert waiting = new Alert(Alert.AlertType.INFORMATION);
        waiting.setTitle("Очікування");
        waiting.setHeaderText("Очікуємо суперника на порту " + PORT + "...");
        waiting.getButtonTypes().setAll(ButtonType.CANCEL);

        final boolean[] connected = {false};

        session.setOnConnected(() -> Platform.runLater(() -> {
            connected[0] = true;
            waiting.close();
            long seed = new Random().nextLong();
            session.send("SEED " + seed);
            starter.start(session, true, seed);
        }));

        waiting.showAndWait();

        if (!connected[0]) {
            session.close();
        }
    }

    private static void joinGame(GameStarter starter) {
        TextInputDialog ipDialog = new TextInputDialog("localhost");
        ipDialog.setTitle("Приєднання");
        ipDialog.setHeaderText("Введіть IP хоста");
        Optional<String> ip = ipDialog.showAndWait();
        if (ip.isEmpty()) {
            return;
        }

        NetworkSession session = NetworkSession.join(ip.get().trim(), PORT);

        session.setMessageHandler(message -> {
            if (message.startsWith("SEED ")) {
                long seed = Long.parseLong(message.substring(5).trim());
                Platform.runLater(() -> starter.start(session, false, seed));
            }
        });
    }
}