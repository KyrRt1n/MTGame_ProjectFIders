package ua.fiders.ui.panels;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

// View ігрового поля. Лише відображає зони, не містить логіки гри.
public class BattlefieldPanel extends VBox {

    private final HBox opponentZone;
    private final HBox playerZone; // Зона-приймач для Drag & Drop

    public BattlefieldPanel() {
        setSpacing(20);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(20));

        opponentZone = createZone("#2c3e50");
        playerZone = createZone("#27ae60");

        getChildren().addAll(opponentZone, playerZone);
    }

    private HBox createZone(String borderColor) {
        HBox zone = new HBox(15);
        zone.setAlignment(Pos.CENTER);
        zone.setMinHeight(220);

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