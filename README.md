# Schach-Lernapp

PC-App zum Schach lernen (Puzzle-Trainer als Alternative zu chess.com), gebaut mit
Java, JavaFX und Stockfish als lokale Analyse-Engine.

Aktueller Stand: **Meilenstein 1 – Grundgerüst** (siehe [meilensteine.md](meilensteine.md)).
Öffnet ein leeres JavaFX-Fenster und prüft beim Start im Hintergrund, ob
[chesslib](https://github.com/bhlangonijr/chesslib) und eine lokale Stockfish-Installation
funktionieren.

## Voraussetzungen

- **JDK 21** oder neuer ([java.net](https://adoptium.net/))
- **Stockfish** lokal installiert (siehe unten) — wird nicht mitgeliefert
- Maven ist **nicht** separat nötig: das Projekt bringt einen Maven Wrapper (`./mvnw`) mit,
  der bei Bedarf automatisch die passende Maven-Version herunterlädt

## Setup

### 1. Stockfish installieren

Stockfish muss separat installiert werden. Auf Arch/CachyOS z. B.:

```bash
sudo pacman -S stockfish
```

Alternativ die Binary direkt von [stockfishchess.org/download](https://stockfishchess.org/download/)
herunterladen und ausführbar machen (`chmod +x stockfish`).

Die App sucht die Binary standardmäßig unter dem Namen `stockfish` im `PATH`. Liegt sie woanders,
kann der Pfad konfiguriert werden über (Priorität von oben nach unten):

1. System-Property beim Start: `-Dstockfish.path=/pfad/zu/stockfish`
2. Umgebungsvariable: `STOCKFISH_PATH=/pfad/zu/stockfish`
3. Fallback: `stockfish` (muss im `PATH` liegen)

Die Konstanten dazu liegen in
[`StockfishEngine`](src/main/java/org/schachlernapp/engine/StockfishEngine.java).

### 2. Projekt bauen

```bash
./mvnw compile
```

Lädt beim ersten Aufruf automatisch JavaFX und chesslib (via JitPack) herunter.

## Build & Run

```bash
./mvnw javafx:run
```

Mit eigenem Stockfish-Pfad:

```bash
./mvnw javafx:run -Dstockfish.path=/opt/stockfish/stockfish
```

Erwartetes Verhalten:

- Es öffnet sich ein leeres JavaFX-Fenster ("Schach-Lernapp").
- In der Konsole erscheint eine Startdiagnose, die bestätigt, dass
  - chesslib eine Standard-Startposition aus einem FEN-String laden und ausgeben kann,
  - Stockfish als Subprozess gestartet werden kann und der UCI-Handshake
    (`uci` → `uciok`, `isready` → `readyok`, `position startpos` + `go` → `bestmove`) funktioniert.

Ist Stockfish nicht installiert oder nicht im `PATH`, öffnet sich das Fenster trotzdem —
die Diagnose meldet dann einen klaren Fehler statt die App abstürzen zu lassen.

## Projektstruktur

```
pom.xml
mvnw, mvnw.cmd, .mvn/wrapper/     Maven Wrapper
src/main/java/org/schachlernapp/
    Main.java                    JavaFX-Einstiegspunkt + Startdiagnose
    chess/ChessLibCheck.java     chesslib-Funktionstest (FEN laden/ausgeben)
    engine/StockfishEngine.java  Stockfish-Subprozess & UCI-Kommunikation
src/main/resources/              JavaFX-Ressourcen (CSS, Bilder, ...)
```

## Tech-Stack

- Java 21
- Maven (+ Maven Wrapper)
- [JavaFX](https://openjfx.io/) – UI
- [chesslib](https://github.com/bhlangonijr/chesslib) – Brettlogik, FEN/PGN
- [Stockfish](https://stockfishchess.org/) – Engine-Analyse (lokaler UCI-Subprozess)
