package org.schachlernapp.opening;

import java.util.List;

/**
 * Eine ECO-Eröffnung/-Variante (M11), so wie sie in der Tabelle {@code openings} liegt
 * (Quelle: github.com/lichess-org/chess-openings).
 *
 * @param eco      ECO-Code, z.B. {@code "C42"}
 * @param name     voller Name inkl. Variante, z.B. {@code "Petrov's Defense: Classical Attack"}
 * @param uciMoves die Buchzüge der Linie in UCI-Notation (aus dem PGN der Quelle konvertiert),
 *                 z.B. {@code ["e2e4", "e7e5", "g1f3", "g8f6"]}
 */
public record Opening(String eco, String name, List<String> uciMoves) {

    public Opening {
        uciMoves = List.copyOf(uciMoves);
    }

    /**
     * Der Familienname (Teil vor dem ersten {@code ": "}), z.B. {@code "Petrov's Defense"}.
     * Für die zweistufige Auswahl im Menü (Eröffnung -&gt; Variante).
     */
    public String family() {
        int colon = name.indexOf(':');
        return colon < 0 ? name : name.substring(0, colon).trim();
    }

    /**
     * Der Variantenname (Teil nach dem ersten {@code ":"}), oder {@code "Hauptlinie"}, wenn der
     * Name keine Variante nennt.
     */
    public String variation() {
        int colon = name.indexOf(':');
        return colon < 0 ? "Hauptlinie" : name.substring(colon + 1).trim();
    }
}
