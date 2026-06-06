package ua.fiders.model.enums;

public enum Phase {
    START,
    MAIN,
    COMBAT,
    SECOND_MAIN,
    END;

    public Phase next() {
        Phase[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public boolean isLast() {
        return this == END;
    }
}
