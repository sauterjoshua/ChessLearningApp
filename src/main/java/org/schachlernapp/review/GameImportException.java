package org.schachlernapp.review;

/** Fehlschlag beim Import über {@link GameImportService} - {@link #reason()} steuert die Fehlermeldung im Dialog. */
public class GameImportException extends Exception {

    public enum Reason {
        USER_NOT_FOUND,
        NO_GAMES_FOR_MONTH,
        NETWORK_ERROR,
        PARSE_ERROR
    }

    private final Reason reason;

    public GameImportException(Reason reason, String message) {
        this(reason, message, null);
    }

    public GameImportException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
