package ua.fiders.ui;

// Інтерфейс-контракт (на майбутнє).
// Через нього GameEngine (логіка) відправлятиме команди в GameController (інтерфейс),
// щоб оновити HP, намалювати взяту карту або запустити анімацію смерті.
public interface GameListener {
    // void onCardDrawn(Player player, Card card);
    // void onDamageDealt(Card target, int damage);
}