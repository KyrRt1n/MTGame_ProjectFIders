package ua.fiders.ui.panels;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

// View ігрового поля. Відображає зони зі скролінгом.
public class BattlefieldPanel extends VBox {

    private final HBox opponentZone;
    private final HBox playerZone; // Зона-приймач для Drag & Drop

    public BattlefieldPanel() {
        setSpacing(20);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(10));

        opponentZone = createZone("#2c3e50");
        playerZone = createZone("#27ae60");

        // Огортаємо наші зони у "вікна" прокрутки
        ScrollPane opponentScroll = createScrollPane(opponentZone);
        ScrollPane playerScroll = createScrollPane(playerZone);

        getChildren().addAll(opponentScroll, playerScroll);
    }

    /**
     * Створює і налаштовує ScrollPane для зони
     */
    private ScrollPane createScrollPane(HBox zone) {
        ScrollPane scrollPane = new ScrollPane(zone);

        scrollPane.setFitToHeight(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        scrollPane.setStyle("-fx-background-color: transparent; " +
                "-fx-background: transparent; " +
                "-fx-padding: 0; " +
                "-fx-control-inner-background: transparent; " +
                "-fx-border-color: transparent;");

        scrollPane.setMaxWidth(800);

        return scrollPane;
    }

    private HBox createZone(String borderColor) {
        HBox zone = new HBox(15);
        zone.setAlignment(Pos.CENTER);
        zone.setMinHeight(220);
        zone.setMinWidth(750);

        zone.setStyle("-fx-background-color: rgba(0, 0, 0, 0.2); " +
                "-fx-border-color: " + borderColor + "; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 10; " +
                "-fx-border-style: dashed;");
        return zone;
    }

    public HBox getOpponentZone() { return opponentZone; }
    public HBox getPlayerZone() { return playerZone; }
}