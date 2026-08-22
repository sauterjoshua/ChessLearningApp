package org.schachlernapp.progress;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lädt/speichert {@link ProgressData} als JSON-Datei im User-Home. Beide
 * Operationen sind defensiv: {@link #load()} wirft nie (liefert bei fehlender
 * oder korrupter Datei einfach frische Default-Werte), {@link #save(ProgressData)}
 * fängt IO-Fehler ab statt die App beim Beenden abstürzen zu lassen.
 */
public class ProgressStore {

    /** System-Property zum Überschreiben des Datei-Pfads: -Dprogress.path=/pfad/zu/progress.json */
    public static final String PATH_PROPERTY = "progress.path";
    /** Alternative Umgebungsvariable: PROGRESS_PATH */
    public static final String PATH_ENV_VAR = "PROGRESS_PATH";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /** Ermittelt den Datei-Pfad aus System-Property, Umgebungsvariable oder Fallback (in dieser Reihenfolge). */
    public static Path resolveDefaultPath() {
        String fromProperty = System.getProperty(PATH_PROPERTY);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return Path.of(fromProperty);
        }
        String fromEnv = System.getenv(PATH_ENV_VAR);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return Path.of(fromEnv);
        }
        return Path.of(System.getProperty("user.home"), ".schachlernapp", "progress.json");
    }

    /** Lädt den gespeicherten Fortschritt. Fehlt die Datei oder ist sie korrupt, werden Default-Werte geliefert. */
    public ProgressData load() {
        Path path = resolveDefaultPath();
        if (!Files.isRegularFile(path)) {
            System.out.println("[progress] Keine gespeicherte Datei unter " + path + " - starte mit Default-Werten.");
            return new ProgressData();
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ProgressData data = gson.fromJson(reader, ProgressData.class);
            if (data == null) {
                throw new JsonParseException("Datei ist leer/enthält kein Objekt");
            }
            System.out.println("[progress] Fortschritt geladen von " + path);
            return data;
        } catch (IOException | JsonParseException e) {
            System.out.println("[progress] FEHLER beim Laden von " + path + " (" + e.getMessage()
                    + ") - starte mit Default-Werten.");
            return new ProgressData();
        }
    }

    /** Speichert den Fortschritt. Schlägt das IO fehl, wird nur geloggt - kein Crash beim Beenden. */
    public void save(ProgressData data) {
        Path path = resolveDefaultPath();
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                gson.toJson(data, writer);
            }
        } catch (IOException e) {
            System.out.println("[progress] FEHLER beim Speichern nach " + path + ": " + e.getMessage());
        }
    }
}
