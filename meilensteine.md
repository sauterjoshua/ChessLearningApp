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

## M8: Spiel-Analyse (chess.com-Import)
- Eigene Partien eines chess.com-Users für Monat/Jahr importieren (öffentliche API, PGN-Parsing)
- Partie Halbzug für Halbzug mit Stockfish analysieren (Fortschrittsanzeige)
- Eval-Graph + Zugliste zur Partie, Navigation per Klick oder Pfeiltasten
- Gut/Ungenau/Fehler/Blunder-Auswertung für die eigenen Züge

## M9: Endgame-Untermenü
- Eigenes Untermenü im Options-Panel (Haupt-/Untermenü-Umschaltung, erweiterbar für z.B. Eröffnung)
- Thematisch gefilterte Matt-/Promotion-Puzzles aus der bestehenden Lichess-Datenbank
- Nach dem ersten Thema-Puzzle bleibt der Auto-Advance im selben Thema (statt allg. Puzzle)

## M10: Startmenü-Navigation
- Eigenständiges Startmenü statt dauerhaftem Options-Panel neben dem Brett
- `Main.switchTo(AppView)` schaltet zwischen Hauptmenü, Endgame-/Eröffnungs-Auswahl und Spielansicht
- "Programm beenden"-Button im Hauptmenü
- (`OptionsPanel` damit abgelöst, bleibt nur noch als toter Code im Repo)

## M11: Eröffnungstrainer
- ECO-Eröffnungen (Quelle: lichess-org/chess-openings, `data/openings/*.tsv`) beim ersten Start
  in die Tabelle `openings` importiert (PGN→UCI via chesslib), in derselben `puzzles.db`
- Auswahl: Eröffnung (Familie) → Variante; Rolle (spielen als / dagegen) + Farbe
- Trainer spielt die Buchzüge der Gegenseite automatisch, vergleicht jeden User-Zug mit der
  Buchlinie; nach Buchende/Abweichung übernehmen die M3/M5-Rückmeldungen
- Generischer Hinweis-Pfeil auf dem Brett (`BoardView.showHintArrow`), Schalter im Trainer-Panel,
  Zustand in `progress.json`
- "Weiter"-Button: nach durchgespielter Variante direkt zur nächsten Variante derselben Eröffnung

## Offen
- Bibliothekswahl (chesslib vs. Alternativen)
- JavaFX vs. Swing für UI
- M6 (eigene Lektionen zu Eröffnungen/Endspielen als JSON/DB) – noch nicht begonnen
