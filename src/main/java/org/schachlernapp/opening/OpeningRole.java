package org.schachlernapp.opening;

/**
 * Rolle des Users im Eröffnungstrainer (M11).
 *
 * <ul>
 *   <li>{@link #PLAY_AS} - der User führt die gewählte Eröffnung selbst aus; der Trainer
 *       spielt die Gegenzüge der ECO-Buchlinie automatisch.</li>
 *   <li>{@link #PLAY_AGAINST} - der User steht auf der Gegenseite und muss die Buch-Antworten
 *       gegen die Eröffnung finden; der Trainer spielt die Eröffnungszüge automatisch.</li>
 * </ul>
 *
 * <p>Mechanisch identisch (der Trainer spielt jeweils die Züge der Seite, die der User
 * nicht kontrolliert) - die Rolle bestimmt nur Beschriftung und die sinnvolle Vorauswahl
 * der Farbe.</p>
 */
public enum OpeningRole {
    PLAY_AS("Eröffnung spielen"),
    PLAY_AGAINST("Dagegen spielen");

    private final String label;

    OpeningRole(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
