package org.schachlernapp.review;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveConversionException;
import com.github.bhlangonijr.chesslib.move.MoveList;
import com.github.bhlangonijr.chesslib.pgn.GameLoader;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Holt Partien eines chess.com-Users für einen Monat über die öffentliche chess.com-API
 * (kein API-Key nötig) und wandelt sie in {@link ImportedGame}s um. Blockierend - Aufrufer
 * (z.B. {@code ImportGameDialog}) müssen dies wie {@code PuzzleRepository}-Zugriffe selbst in
 * einem Hintergrund-Thread ausführen.
 *
 * <p>chess.com liefert für das Monats-Archiv bei jedem unbekannten Username UND bei jedem
 * Monat ohne Partien denselben 404-Status. Um daraus eine sinnvolle Fehlermeldung zu machen,
 * wird bei einem 404 zusätzlich der Profil-Endpunkt geprüft (existiert der User überhaupt?).</p>
 */
public class GameImportService {

    private static final String ARCHIVE_URL = "https://api.chess.com/pub/player/%s/games/%04d/%02d";
    private static final String PROFILE_URL = "https://api.chess.com/pub/player/%s";

    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public GameImportService() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    public GameImportService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /** Lädt alle Partien des Users für den angegebenen Monat. Leere Liste, falls der Monat keine Partien enthält. */
    public List<ImportedGame> fetchGames(String username, YearMonth month) throws GameImportException {
        String normalizedUsername = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (normalizedUsername.isEmpty()) {
            throw new GameImportException(GameImportException.Reason.USER_NOT_FOUND,
                    "Bitte einen chess.com-Benutzernamen eingeben.");
        }

        String archiveUrl = String.format(ARCHIVE_URL, normalizedUsername, month.getYear(), month.getMonthValue());
        HttpResponse<String> response = send(archiveUrl);

        if (response.statusCode() == 404) {
            if (userExists(normalizedUsername)) {
                throw new GameImportException(GameImportException.Reason.NO_GAMES_FOR_MONTH,
                        "Keine Partien für \"" + username + "\" im gewählten Monat gefunden.");
            }
            throw new GameImportException(GameImportException.Reason.USER_NOT_FOUND,
                    "Der Benutzername \"" + username + "\" wurde bei chess.com nicht gefunden.");
        }
        if (response.statusCode() != 200) {
            throw new GameImportException(GameImportException.Reason.NETWORK_ERROR,
                    "chess.com antwortete mit Status " + response.statusCode() + ".");
        }

        ChessComGamesResponse parsed;
        try {
            parsed = gson.fromJson(response.body(), ChessComGamesResponse.class);
        } catch (JsonSyntaxException e) {
            throw new GameImportException(GameImportException.Reason.PARSE_ERROR,
                    "Antwort von chess.com konnte nicht gelesen werden.", e);
        }
        if (parsed == null || parsed.games == null) {
            return List.of();
        }

        // chess.com liefert das Monats-Archiv chronologisch aufsteigend (älteste zuerst) - für die
        // Partie-Auswahl im ReviewPanel-Dropdown ist "neueste zuerst" die erwartete Reihenfolge.
        List<ChessComGameDto> gamesNewestFirst = new ArrayList<>(parsed.games);
        gamesNewestFirst.sort((a, b) -> Long.compare(b.end_time, a.end_time));

        List<ImportedGame> result = new ArrayList<>();
        for (ChessComGameDto dto : gamesNewestFirst) {
            if (dto.pgn == null || dto.pgn.isBlank()) {
                continue; // z.B. noch laufende Partien haben teils kein PGN
            }
            result.add(toImportedGame(dto, normalizedUsername));
        }
        return result;
    }

    private boolean userExists(String normalizedUsername) throws GameImportException {
        HttpResponse<String> response = send(String.format(PROFILE_URL, normalizedUsername));
        return response.statusCode() == 200;
    }

    private HttpResponse<String> send(String url) throws GameImportException {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new GameImportException(GameImportException.Reason.NETWORK_ERROR,
                    "Verbindung zu chess.com fehlgeschlagen: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GameImportException(GameImportException.Reason.NETWORK_ERROR,
                    "Import wurde unterbrochen.", e);
        }
    }

    private ImportedGame toImportedGame(ChessComGameDto dto, String normalizedUsername) throws GameImportException {
        boolean userIsWhite = dto.white != null && normalizedUsername.equalsIgnoreCase(dto.white.username);
        Side userSide = userIsWhite ? Side.WHITE : Side.BLACK;
        ChessComPlayerDto userPlayer = userIsWhite ? dto.white : dto.black;
        ChessComPlayerDto opponentPlayer = userIsWhite ? dto.black : dto.white;
        String opponentUsername = opponentPlayer != null && opponentPlayer.username != null
                ? opponentPlayer.username : "?";

        GameOutcome outcome = toOutcome(userPlayer, opponentPlayer);
        LocalDate date = dto.end_time > 0
                ? Instant.ofEpochSecond(dto.end_time).atZone(ZoneId.systemDefault()).toLocalDate()
                : LocalDate.now();

        Game game = parsePgn(dto.pgn);
        MoveList halfMoves = game.getHalfMoves();
        List<String> sanMoves;
        try {
            sanMoves = Arrays.asList(halfMoves.toSanArray());
        } catch (MoveConversionException e) {
            throw new GameImportException(GameImportException.Reason.PARSE_ERROR,
                    "PGN konnte nicht in eine Zugliste umgewandelt werden.", e);
        }

        List<String> fens = new ArrayList<>(sanMoves.size() + 1);
        Board board = new Board();
        board.loadFromFen(halfMoves.getStartFen());
        fens.add(board.getFen());
        for (Move move : halfMoves) {
            board.doMove(move);
            fens.add(board.getFen());
        }

        return new ImportedGame(dto.url, date, opponentUsername, userSide, outcome, sanMoves, fens);
    }

    private static GameOutcome toOutcome(ChessComPlayerDto userPlayer, ChessComPlayerDto opponentPlayer) {
        String userResult = userPlayer != null ? userPlayer.result : null;
        String opponentResult = opponentPlayer != null ? opponentPlayer.result : null;
        if ("win".equals(userResult)) {
            return GameOutcome.WIN;
        }
        if ("win".equals(opponentResult)) {
            return GameOutcome.LOSS;
        }
        return GameOutcome.DRAW;
    }

    private Game parsePgn(String pgn) throws GameImportException {
        List<String> lines = new ArrayList<>(Arrays.asList(pgn.split("\\r?\\n")));
        Game game;
        try {
            game = GameLoader.loadNextGame(lines.iterator());
        } catch (Exception e) {
            throw new GameImportException(GameImportException.Reason.PARSE_ERROR,
                    "PGN konnte nicht geparst werden.", e);
        }
        if (game == null || game.getHalfMoves() == null) {
            throw new GameImportException(GameImportException.Reason.PARSE_ERROR,
                    "PGN enthielt keine gültige Partie.");
        }
        return game;
    }

    /** Wire-Format der chess.com-API - nur intern für {@link Gson#fromJson} genutzt. */
    private static final class ChessComGamesResponse {
        List<ChessComGameDto> games;
    }

    private static final class ChessComGameDto {
        String url;
        String pgn;
        long end_time;
        ChessComPlayerDto white;
        ChessComPlayerDto black;
    }

    private static final class ChessComPlayerDto {
        String username;
        String result;
    }
}
