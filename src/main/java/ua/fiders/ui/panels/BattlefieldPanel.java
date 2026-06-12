package ua.fiders.ui.panels;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ua.fiders.model.enums.Type;

public class BattlefieldPanel extends VBox {

    private final HBox opponentZone;
    private final HBox playerZone;

    public BattlefieldPanel() {
        setSpacing(10);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(5));

        opponentZone = createZone("#2c3e50");
        playerZone = createZone("#27ae60");

        ScrollPane opponentScroll = createScrollPane(opponentZone);
        ScrollPane playerScroll = createScrollPane(playerZone);

        getChildren().addAll(opponentScroll, playerScroll);
    }

    public void addCard(CardView view, boolean isPlayer) {
        HBox zone = isPlayer ? playerZone : opponentZone;

        if (view.getCard().getType() == Type.Land) {
            String landName = view.getCard().getName();

            for (Node node : zone.getChildren()) {
                if (node instanceof VBox landStack) {
                    if (!landStack.getChildren().isEmpty()) {
                        CardView firstCard = (CardView) landStack.getChildren().get(0);
                        if (firstCard.getCard().getName().equals(landName)) {
                            landStack.getChildren().add(view);
                            return;
                        }
                    }
                }
            }

            VBox newLandStack = new VBox(-170);
            newLandStack.setAlignment(Pos.TOP_CENTER);
            newLandStack.getChildren().add(view);
            zone.getChildren().add(newLandStack);

        } else {
            zone.getChildren().add(view);
        }
    }

    public void removeCard(CardView view) {
        removeFromZone(playerZone, view);
        removeFromZone(opponentZone, view);
    }

    private void removeFromZone(HBox zone, CardView viewToFind) {
        if (zone.getChildren().contains(viewToFind)) {
            zone.getChildren().remove(viewToFind);
            return;
        }

        for (Node node : zone.getChildren()) {
            if (node instanceof VBox landStack) {
                if (landStack.getChildren().contains(viewToFind)) {
                    landStack.getChildren().remove(viewToFind);
                    if (landStack.getChildren().isEmpty()) {
                        zone.getChildren().remove(landStack);
                    }
                    return;
                }
            }
        }
    }

    private ScrollPane createScrollPane(HBox zone) {
        ScrollPane scrollPane = new ScrollPane(zone);
        scrollPane.setFitToHeight(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0; -fx-border-color: transparent;");
        scrollPane.setMaxWidth(800);
        return scrollPane;
    }

    private HBox createZone(String borderColor) {
        HBox zone = new HBox(15);
        zone.setAlignment(Pos.CENTER);
        zone.setMinHeight(205);
        zone.setMinWidth(750);

        zone.setStyle("-fx-background-color: rgba(0, 0, 0, 0.2); " +
                "-fx-border-color: " + borderColor + "; " +
                "-fx-border-width: 2; " +
                "-fx-border-insets: 1; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10; " +
                "-fx-border-style: dashed;");
        return zone;
    }

    public HBox getOpponentZone() { return opponentZone; }
    public HBox getPlayerZone() { return playerZone; }
}