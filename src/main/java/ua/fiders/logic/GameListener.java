package ua.fiders.logic;

import ua.fiders.model.Permanent;
import ua.fiders.model.Player;

public interface GameListener {
    void onManaChanged(Player player);

    void onTurnChanged(Player newActivePlayer);

    void onPermanentEnteredBattlefield(Permanent permanent);

    void onHpChanged(Player player);

    void onHandUpdated(Player player);

    default void onGameOver(Player winner) {
    }
}