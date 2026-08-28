# Schach-Lernapp

PC-App zum Schach lernen (Puzzle-Trainer als Alternative zu chess.com), gebaut mit
Java, JavaFX und Stockfish als lokale Analyse-Engine.

Aktueller Stand (siehe [meilensteine.md](meilensteine.md)): **M1–M5 und M7–M11 umgesetzt**.
M6 (eigene Lektionen zu Eröffnungen/Endspielen als JSON/DB) ist **noch nicht implementiert** –
das Endgame-Untermenü aus M9 nutzt dafür thematisch gefilterte Puzzles aus der bestehenden
Lichess-Datenbank, der Eröffnungstrainer aus M11 die ECO-Eröffnungsdaten von
[lichess-org/chess-openings](https://github.com/lichess-org/chess-openings) statt eigens
verfasster Lektionen.

## Features

- **Startmenü-Navigation**: eigenes Hauptmenü beim Start (Neues Spiel / Neues Puzzle / Eröffnung /
  Endgame / Partie importieren / Programm beenden); die Spielansicht hat unten rechts einen
  "Zurück"-Button ins Hauptmenü
- **Schachbrett** mit Drag&Drop, Zugvalidierung über [chesslib](https://github.com/bhlangonijr/chesslib),
  Rand-Beschriftung (a–h/1–8), automatischer Umdrehung bei Puzzles/Eröffnungen (eigene Seite unten)
- **Eval-Balken**: Live-Stellungsbewertung durch einen lokalen Stockfish-Subprozess
- **Lern-Modus**: freies Spiel mit Zug-für-Zug-Feedback (gut/ungenau/Fehler/Blunder) und
  Engine-Zugvorschlag in SAN
- **Puzzle-Trainer**: Puzzles aus einer selbst importierten Lichess-Puzzle-Datenbank lösen,
  eigenes Elo-ähnliches Rating, das sich an die Puzzle-Schwierigkeit anpasst
- **Endgame-Training**: eigene Auswahl-Ansicht mit thematisch gefilterten
  Matt-Puzzles (Bauernendspiel + eigene Promotion-Variante, Turm-, Läufer-, Springer-,
  Damenendspiel, Läufer vs. Springer, Allgemein) aus derselben Puzzle-Datenbank; nach dem
  ersten Puzzle bleibt der Auto-Advance im gewählten Thema
- **Eröffnungstrainer**: eine ECO-Eröffnung (Familie → Variante, ~150 Familien) auswählen, als
  Rolle "spielen als" oder "dagegen" plus Farbe; der Trainer spielt die Buchzüge der Gegenseite
  automatisch und prüft jeden eigenen Zug gegen die Buchlinie. Nach Buchende oder einer Abweichung
  übernehmen die normalen Eval-/Blunder-Rückmeldungen. Optionaler **Hinweis-Pfeil** auf dem Brett
  (Schalter direkt beim Trainer-Panel, Zustand wird mitgespeichert) zeigt den nächsten Buchzug.
  Nach durchgespielter Variante führt ein "Weiter"-Button direkt zur nächsten Variante derselben
  Eröffnung. Die ECO-Daten (Quelle: lichess-org/chess-openings) sind mitgeliefert und werden beim
  ersten Start automatisch in die Datenbank importiert – kein manueller Schritt nötig
- **Partie-Import & -Analyse (chess.com)**: eigene Partien eines chess.com-Users für einen
  Monat importieren (neueste zuerst), danach Zug-für-Zug-Analyse mit Stockfish (Fortschrittsbalken,
  läuft im Hintergrund), Eval-Graph + Zugliste zur Partie, Navigation per Klick oder Pfeiltasten
  (←/→, bei gedrückter Taste beschleunigt, unabhängig vom gerade fokussierten Bedienelement),
  Gut/Ungenau/Fehler/Blunder-Auswertung für die eigenen Züge
- **Dark Theme**: durchgängiges dunkles Farbschema (an Claude/Anthropic angelehnt), Brett/
  Menü/Eval-Leiste/Review-Zeile farblich unterschieden; Schachfiguren im "cburnett"-Design
  (Colin M. L. Burnett, siehe [Tech-Stack](#tech-stack) für die Lizenz)
- **Fortschritt** (Puzzle-Rating, Lern-Modus-Statistik, Hinweis-Pfeil-Schalter) wird automatisch
  unter `~/.schachlernapp/progress.json` gespeichert und beim nächsten Start geladen
- Robuste Fehlerbehandlung: fehlender Stockfish oder fehlende/korrupte Puzzle-/Eröffnungs-Datenbank
  führen zu einer Fehlermeldung im UI statt zum Absturz – die restlichen Funktionen
  bleiben nutzbar

## Voraussetzungen

- **JDK 21** oder neuer ([adoptium.net](https://adoptium.net/))
- **Stockfish** lokal installiert (siehe unten) — wird nicht mitgeliefert
- Maven ist **nicht** separat nötig: das Projekt bringt einen Maven Wrapper (`./mvnw`) mit,
  der bei Bedarf automatisch die passende Maven-Version herunterlädt
- Internetzugang **nur** für den Partie-Import (öffentliche chess.com-API, kein API-Key
  nötig) — alle anderen Features (Brett, Puzzle-Trainer, Lern-Modus, Endgame-Training,
  Eröffnungstrainer) funktionieren komplett offline; die ECO-Eröffnungsdaten liegen als
  `data/openings/*.tsv` im Repo

## Setup

### 1. Stockfish installieren

Stockfish muss separat installiert werden. Auf Arch/CachyOS z. B.:

```bash
sudo pacman -S stockfish
```

Alternativ die Binary direkt von [stockfishchess.org/download](https://stockfishchess.org/download/)
herunterladen und ausführbar machen (`chmod +x stockfish`).

Die App sucht die Binary standardmäßig unter dem Namen `stockfish` im `PATH`. Ist Stockfish
nicht auffindbar, öffnet sich das Fenster trotzdem — Eval-Balken, Blunder-Erkennung, Lern-Modus,
Puzzle-Feature und Eröffnungstrainer melden dann einen klaren Fehlerdialog statt abzustürzen;
das Brett bleibt uneingeschränkt nutzbar. Pfad überschreibbar via `-Dstockfish.path=...` (siehe
[Konfiguration](#konfiguration)).

### 2. Projekt bauen

```bash
./mvnw compile
```

Lädt beim ersten Aufruf automatisch JavaFX, chesslib (via JitPack), sqlite-jdbc und Gson herunter.

### 3. Puzzle-Datenbank befüllen (optional, aber empfohlen)

Das Puzzle-Feature braucht eine lokale SQLite-Datei mit Puzzles. Die volle
[Lichess-Puzzle-CSV](https://database.lichess.org/#puzzles) herunterladen (~1 GB, ~6 Mio. Zeilen)
und damit importieren:

```bash
./mvnw -q dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp "target/classes:$(cat cp.txt)" \
  org.schachlernapp.puzzle.PuzzleCsvImporter <pfad-zur-csv> puzzles.db \
  --min-rating 1000 --max-rating 1800 --limit 20000
```

`--min-rating`/`--max-rating`/`--limit` sind optional und filtern schon beim Einlesen (die volle
Datei muss nicht komplett verarbeitet werden). Ohne importierte `puzzles.db` startet die App
trotzdem — das Puzzle-Panel zeigt dann "Kein passendes Puzzle gefunden".

### 4. Eröffnungsdaten

Kein Schritt nötig: die ~3.800 ECO-Linien liegen als `data/openings/*.tsv` im Repo und werden
beim ersten Start automatisch in die Tabelle `openings` (in `puzzles.db`) importiert
(PGN→UCI-Konvertierung, einmalig ~0,5 s). Bei den Folgestarts wird der Import übersprungen.

## Build & Run

**Entwicklung** (startet über den javafx-maven-plugin direkt aus dem Quellcode):

```bash
./mvnw javafx:run
```

**Ausführbares Jar bauen** (Fat-Jar mit allen Abhängigkeiten):

```bash
./mvnw package
java -jar target/schachlernapp-0.1.0-SNAPSHOT.jar
```

Das Jar wird für die Build-Plattform gebaut (native JavaFX-Bibliotheken sind plattformspezifisch)
— für Windows/Mac müsste auf der jeweiligen Plattform separat gebaut werden.

## Konfiguration

Alle Pfade folgen demselben Muster: System-Property > Umgebungsvariable > Default.

| Zweck | System-Property | Umgebungsvariable | Default |
|---|---|---|---|
| Stockfish-Binary | `-Dstockfish.path=...` | `STOCKFISH_PATH` | `stockfish` (muss im `PATH` liegen) |
| Puzzle-Datenbank | `-Dpuzzles.db.path=...` | `PUZZLES_DB_PATH` | `puzzles.db` (Arbeitsverzeichnis) |
| Eröffnungs-Datenbank | `-Dopenings.db.path=...` | `OPENINGS_DB_PATH` | `puzzles.db` (dieselbe Datei, Tabelle `openings`) |
| ECO-Seed-Verzeichnis | `-Dopenings.tsv.dir=...` | `OPENINGS_TSV_DIR` | `data/openings` (Arbeitsverzeichnis) |
| Fortschrittsdatei | `-Dprogress.path=...` | `PROGRESS_PATH` | `~/.schachlernapp/progress.json` |

Beispiel:

```bash
./mvnw javafx:run -Dstockfish.path=/opt/stockfish/stockfish -Dpuzzles.db.path=/pfad/zu/puzzles.db
```

## Projektstruktur

```
pom.xml
mvnw, mvnw.cmd, .mvn/wrapper/         Maven Wrapper
data/openings/*.tsv                   ECO-Eröffnungs-Seed-Daten (lichess-org/chess-openings)
src/main/resources/style.css          Struktur/Layout/Typografie (keine Farben)
src/main/resources/dark-theme.css     Farbschema (Dark Theme) - einzige Stelle für alle Farbwerte
src/main/resources/pieces/*.png       Schachfiguren, "cburnett"-Set (siehe Tech-Stack)
src/main/java/org/schachlernapp/
    Main.java                        JavaFX-Einstiegspunkt, Verdrahtung, Persistenz-Lifecycle,
                                     Startmenü-Navigation (switchTo(AppView))
    Launcher.java                    Separater Start für das Fat-Jar (java -jar)
    chess/ChessLibCheck.java         chesslib-Funktionstest (FEN laden/ausgeben)
    engine/                          Stockfish-Subprozess, UCI-Kommunikation, Live-Auswertung
    analysis/                        Vorher/Nachher-Eval-Vergleich, Blunder-/Lern-Modus-Feedback
    puzzle/                          CSV-Import, SQLite-DAO (inkl. Endgame-Theme-Filter),
                                     Puzzle-Session, Rating-System
    opening/                         ECO-Seed-Import (PGN→UCI), SQLite-DAO (Tabelle openings),
                                     Eröffnungstrainer-Service (Buchzug-Vergleich, Hint-Pfeil)
    review/                          chess.com-Import (HTTP + PGN-Parsing via chesslib),
                                     Partie-Analyse-Engine (Stockfish, Fortschritts-Callback)
    progress/                        Laden/Speichern des Fortschritts als JSON
    ui/board/                        Brett-Widget, Drag&Drop, Figuren-Rendering, Hinweis-Pfeil-Overlay
    ui/eval/, ui/learn/, ui/puzzle/, ui/opening/
                                     Eval-Balken, Lern-Modus-, Puzzle- und Eröffnungs-Panels
    ui/review/                       Import-Dialog, Eval-Graph, Zugliste, Review-Zeile
    ui/AppView.java                  Enum der Top-Level-Ansichten (Hauptmenü / Auswahl / Spiel)
    ui/MainMenuView.java             Startmenü (Aktions-Buttons + Programm beenden)
    ui/EndgameMenuView.java          Endgame-Themen-Auswahl
    ui/OpeningMenuView.java          Eröffnungs-Auswahl (Familie → Variante, Rolle, Farbe)
    ui/OptionsPanel.java             seit M10 abgelöst durch MainMenuView, ungenutzt
    ui/UiAlerts.java                 Wiederverwendbare Fehlerdialoge
```

## Tech-Stack

- Java 21
- Maven (+ Maven Wrapper)
- [JavaFX](https://openjfx.io/) – UI
- [chesslib](https://github.com/bhlangonijr/chesslib) – Brettlogik, FEN/PGN
- [Stockfish](https://stockfishchess.org/) – Engine-Analyse (lokaler UCI-Subprozess)
- [sqlite-jdbc](https://github.com/xerial/sqlite-jdbc) – Puzzle-Datenbank
- [Gson](https://github.com/google/gson) – Fortschritt als JSON, chess.com-API-Antworten parsen
- [chess.com Public API](https://www.chess.com/news/view/published-data-api) – Partie-Import
  (kein API-Key nötig, nur `java.net.http.HttpClient` aus dem JDK)
- [lichess-org/chess-openings](https://github.com/lichess-org/chess-openings) – ECO-Eröffnungs-TSVs
  (`a.tsv`–`e.tsv`), als Seed-Daten unter `data/openings/` gebündelt; lizenziert unter CC0
- ["cburnett"-Schachfiguren](https://github.com/lichess-org/lila/tree/master/public/piece/cburnett)
  von Colin M. L. Burnett – lizenziert unter GPLv2+ / CC-BY-SA 3.0 (Namensnennung erforderlich,
  bei CC-BY-SA zusätzlich Share-Alike); Original-SVGs via lichess-org/lila, hier als PNG gebündelt
  (JavaFX kann kein SVG laden), keine zusätzliche Maven-Abhängigkeit
- maven-shade-plugin – Packaging als ausführbares Fat-Jar
