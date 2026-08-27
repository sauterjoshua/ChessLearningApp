# Schach-Lernapp

PC-App zum Schach lernen (Puzzle-Trainer als Alternative zu chess.com), gebaut mit
Java, JavaFX und Stockfish als lokale Analyse-Engine.

Aktueller Stand (siehe [meilensteine.md](meilensteine.md)): **M1–M5 und M7–M9 umgesetzt**.
M6 (eigene Lektionen zu Eröffnungen/Endspielen als JSON/DB) ist **noch nicht implementiert** –
das Endgame-Untermenü aus M9 nutzt dafür thematisch gefilterte Puzzles aus der bestehenden
Lichess-Datenbank statt eigens verfasster Lektionen.

## Features

- **Schachbrett** mit Drag&Drop, Zugvalidierung über [chesslib](https://github.com/bhlangonijr/chesslib),
  Rand-Beschriftung (a–h/1–8), automatischer Umdrehung bei Puzzles (lösende Seite unten)
- **Eval-Balken**: Live-Stellungsbewertung durch einen lokalen Stockfish-Subprozess
- **Lern-Modus**: freies Spiel mit Zug-für-Zug-Feedback (gut/ungenau/Fehler/Blunder) und
  Engine-Zugvorschlag in SAN
- **Puzzle-Trainer**: Puzzles aus einer selbst importierten Lichess-Puzzle-Datenbank lösen,
  eigenes Elo-ähnliches Rating, das sich an die Puzzle-Schwierigkeit anpasst
- **Endgame-Training**: eigenes Untermenü im Options-Panel mit thematisch gefilterten
  Matt-Puzzles (Bauern-, Turm-, Läufer-, Springer-, Damenendspiel, Läufer vs. Springer,
  Bauernendspiel mit Promotion, Allgemein) aus derselben Puzzle-Datenbank
- **Partie-Import & -Analyse (chess.com)**: eigene Partien eines chess.com-Users für einen
  Monat importieren, danach Zug-für-Zug-Analyse mit Stockfish (Fortschrittsbalken, läuft im
  Hintergrund), Eval-Graph + Zugliste zur Partie, Navigation per Klick oder Pfeiltasten
  (←/→), Gut/Ungenau/Fehler/Blunder-Auswertung für die eigenen Züge
- **Fortschritt** (Puzzle-Rating, Lern-Modus-Statistik) wird automatisch unter
  `~/.schachlernapp/progress.json` gespeichert und beim nächsten Start geladen
- Robuste Fehlerbehandlung: fehlender Stockfish oder fehlende/korrupte Puzzle-Datenbank
  führen zu einer Fehlermeldung im UI statt zum Absturz – die restlichen Funktionen
  bleiben nutzbar

## Voraussetzungen

- **JDK 21** oder neuer ([adoptium.net](https://adoptium.net/))
- **Stockfish** lokal installiert (siehe unten) — wird nicht mitgeliefert
- Maven ist **nicht** separat nötig: das Projekt bringt einen Maven Wrapper (`./mvnw`) mit,
  der bei Bedarf automatisch die passende Maven-Version herunterlädt
- Internetzugang **nur** für den Partie-Import (öffentliche chess.com-API, kein API-Key
  nötig) — alle anderen Features (Brett, Puzzle-Trainer, Lern-Modus, Endgame-Training)
  funktionieren komplett offline

## Setup

### 1. Stockfish installieren

Stockfish muss separat installiert werden. Auf Arch/CachyOS z. B.:

```bash
sudo pacman -S stockfish
```

Alternativ die Binary direkt von [stockfishchess.org/download](https://stockfishchess.org/download/)
herunterladen und ausführbar machen (`chmod +x stockfish`).

Die App sucht die Binary standardmäßig unter dem Namen `stockfish` im `PATH`. Ist Stockfish
nicht auffindbar, öffnet sich das Fenster trotzdem — Eval-Balken, Blunder-Erkennung, Lern-Modus
und Puzzle-Feature melden dann einen klaren Fehlerdialog statt abzustürzen; das Brett bleibt
uneingeschränkt nutzbar. Pfad überschreibbar via `-Dstockfish.path=...` (siehe
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

Alle drei folgen demselben Muster: System-Property > Umgebungsvariable > Default.

| Zweck | System-Property | Umgebungsvariable | Default |
|---|---|---|---|
| Stockfish-Binary | `-Dstockfish.path=...` | `STOCKFISH_PATH` | `stockfish` (muss im `PATH` liegen) |
| Puzzle-Datenbank | `-Dpuzzles.db.path=...` | `PUZZLES_DB_PATH` | `puzzles.db` (Arbeitsverzeichnis) |
| Fortschrittsdatei | `-Dprogress.path=...` | `PROGRESS_PATH` | `~/.schachlernapp/progress.json` |

Beispiel:

```bash
./mvnw javafx:run -Dstockfish.path=/opt/stockfish/stockfish -Dpuzzles.db.path=/pfad/zu/puzzles.db
```

## Projektstruktur

```
pom.xml
mvnw, mvnw.cmd, .mvn/wrapper/         Maven Wrapper
src/main/resources/style.css          Zentrales Stylesheet (Brett-Farben, Panels, Figuren)
src/main/java/org/schachlernapp/
    Main.java                        JavaFX-Einstiegspunkt, Verdrahtung, Persistenz-Lifecycle
    Launcher.java                    Separater Start für das Fat-Jar (java -jar)
    chess/ChessLibCheck.java         chesslib-Funktionstest (FEN laden/ausgeben)
    engine/                          Stockfish-Subprozess, UCI-Kommunikation, Live-Auswertung
    analysis/                        Vorher/Nachher-Eval-Vergleich, Blunder-/Lern-Modus-Feedback
    puzzle/                          CSV-Import, SQLite-DAO (inkl. Endgame-Theme-Filter),
                                     Puzzle-Session, Rating-System
    review/                          chess.com-Import (HTTP + PGN-Parsing via chesslib),
                                     Partie-Analyse-Engine (Stockfish, Fortschritts-Callback)
    progress/                        Laden/Speichern des Fortschritts als JSON
    ui/board/                        Brett-Widget, Drag&Drop, Figuren-Rendering
    ui/eval/, ui/learn/, ui/puzzle/  Eval-Balken, Lern-Modus- und Puzzle-Feedback-Panels
    ui/review/                       Import-Dialog, Eval-Graph, Zugliste, Review-Zeile
    ui/OptionsPanel.java             Aktions-Buttons (Neues Spiel/Puzzle/Endgame-Untermenü/
                                     Partie importieren)
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
- maven-shade-plugin – Packaging als ausführbares Fat-Jar
