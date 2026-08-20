# Meilensteine – Schach-Lernapp (Java)

## M1: Grundgerüst
- Projekt-Setup (Maven/Gradle, JavaFX)
- Schach-Library einbinden (z.B. chesslib) für Brettlogik, FEN/PGN
- Stockfish als Subprozess starten, UCI-Kommunikation testen

## M2: Brett-UI
- Brett-Widget (JavaFX) mit Drag&Drop
- Zugvalidierung über Library
- Positionen laden/anzeigen (FEN)

## M3: Engine-Integration
- Stockfish-Wrapper: Zug bewerten, Eval abrufen
- Eval-Balken in UI
- Blunder-Erkennung (Eval-Differenz vor/nach Zug)

## M4: Puzzle-Feature (Kernstück)
- Lichess Puzzle-CSV importieren → SQLite
- Puzzle laden (FEN + Lösung)
- User-Zug mit Lösung vergleichen, Feedback anzeigen
- Eigenes Rating-System (steigt/sinkt)

## M5: Lern-Modus
- Zug spielen → Eval-Vergleich → Feedback/Erklärtext
- Zugvorschläge anzeigen

## M6: Content-Erweiterung
- Eigene Lektionen (Eröffnungen, Endspiele) als JSON/DB
- Lektionsauswahl in UI

## M7: Politur
- Persistenz (Fortschritt, Rating) speichern
- Fehlerbehandlung, Performance-Check
- Packaging (ausführbares JAR)

## Offen
- Bibliothekswahl (chesslib vs. Alternativen)
- JavaFX vs. Swing für UI
