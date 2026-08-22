package org.schachlernapp;

/**
 * Getrennter Einstiegspunkt fürs Fat-Jar-Packaging (M7). {@code java -jar}
 * erkennt eine Klasse, die direkt {@code javafx.application.Application}
 * erweitert, als "JavaFX-App ohne Modulpfad" und verweigert den Start
 * ("Error: JavaFX runtime components are missing") - unabhängig davon, ob die
 * JavaFX-Klassen tatsächlich im Jar liegen. Ein Launcher, der
 * {@code Application} selbst nicht erweitert, umgeht diese Erkennung.
 * Funktioniert genauso mit {@code ./mvnw javafx:run} wie mit {@code java -jar}.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Main.main(args);
    }
}
