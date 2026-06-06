package ua.fiders.ui.panels;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import ua.fiders.model.cards.Card;

import java.util.List;

// View руки гравця. Генерує події перетягування
public class HandPanel extends HBox {

    public HandPanel() {
        super(-40); // Ефект "віяла" (нахлест карт)
        setAlignment(Pos.CENTER);
        setPadding(new Insets(20));
        setMinHeight(250);
    }

    public void updateHand(List<Card> cards) {
        getChildren().clear();

        for (Card card : cards) {
            CardView cardView = new CardView(card);

            // Реєстрація карти як джерела Drag-and-Drop
            cardView.setOnDragDetected(event -> {
                Dragboard db = cardView.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(card.getName());
                db.setContent(content);

                db.setDragView(cardView.snapshot(null, null)); // Тінь карти за курсором
                cardView.setOpacity(0.3); // Напівпрозорість оригіналу в руці
                event.consume();
            });

            cardView.setOnDragDone(event -> {
                cardView.setOpacity(1.0);
                event.consume();
            });

            getChildren().add(cardView);
        }
    }
}