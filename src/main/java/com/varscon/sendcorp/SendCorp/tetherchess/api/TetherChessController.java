package com.varscon.sendcorp.SendCorp.tetherchess.api;

import com.varscon.sendcorp.SendCorp.tetherchess.api.dto.*;
import com.varscon.sendcorp.SendCorp.tetherchess.engine.TetherChessEngine;
import com.varscon.sendcorp.SendCorp.tetherchess.models.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * REST API Controller for Tether Chess (Tal's Forest).
 *
 * Provides endpoints for:
 * - Creating new games
 * - Making moves (including transporter moves)
 * - Getting game state and legal moves
 * - Analyzing special moves (Pawn-Knight Apex, Michael Tal opening)
 */
@RestController
@RequestMapping("/api/v1/tetherchess")
@CrossOrigin(origins = "*")
public class TetherChessController {

    // In-memory game storage (use proper persistence in production)
    private final Map<String, TetherChessEngine> games = new ConcurrentHashMap<>();

    /**
     * Get the rules of Tether Chess.
     */
    @GetMapping("/rules")
    public ResponseEntity<Map<String, Object>> getRules() {
        Map<String, Object> rules = new LinkedHashMap<>();
        rules.put("name", "Tether Chess (Tal's Forest)");
        rules.put("creator", "The Principle of Geometric Entanglement");

        Map<String, String> fourRules = new LinkedHashMap<>();
        fourRules.put("1_rankEntanglement",
                "Every piece on a horizontal rank shares its movement potential with all friendly pieces on that same rank. " +
                        "Piece A can move to any square that Piece B (on the same rank) could legally reach, starting from Piece A's current position.");
        fourRules.put("2_pawnKnightApex",
                "A Pawn sharing a rank with a Knight can utilize the Knight's L-jump. " +
                        "If that jump lands the Pawn on the 8th rank, it promotes IMMEDIATELY.");
        fourRules.put("3_nativeLethality",
                "A piece can only deliver Check or Checkmate using its own NATIVE movement rules. " +
                        "A teleporting Rook landing next to a King does NOT check unless the King is on the Rook's file or rank.");
        fourRules.put("4_noRecursiveJumping",
                "Cannot chain borrowed moves. Each turn allows one transporter move maximum. " +
                        "The borrowed movement cannot itself be used to borrow again.");

        rules.put("fourUniqueRules", fourRules);

        Map<String, String> strategy = new LinkedHashMap<>();
        strategy.put("pathIntegrity", "If the borrowed move is a slide (Bishop/Rook/Queen), the path must be clear from the moving piece's origin.");
        strategy.put("quietRespect", "Despite high-octane teleportation, the game remains grounded by Native Lethality. " +
                "You can fly across the board, but you must land where your own soul (native move) can strike.");
        strategy.put("michaelTalOpening", "Use back-rank Knights to teleport Queen or Rooks over the pawn wall on Move 1.");

        rules.put("strategy", strategy);

        return ResponseEntity.ok(rules);
    }

    /**
     * Create a new game.
     */
    @PostMapping("/games")
    public ResponseEntity<Map<String, Object>> createGame() {
        String gameId = UUID.randomUUID().toString().substring(0, 8);
        TetherChessEngine engine = new TetherChessEngine();
        engine.newGame();
        games.put(gameId, engine);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("gameId", gameId);
        response.put("message", "New Tether Chess game created");
        response.put("gameState", GameStateResponse.fromEngine(engine));

        return ResponseEntity.ok(response);
    }

    /**
     * Get game state.
     */
    @GetMapping("/games/{gameId}")
    public ResponseEntity<?> getGame(@PathVariable String gameId) {
        TetherChessEngine engine = games.get(gameId);
        if (engine == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(GameStateResponse.fromEngine(engine));
    }

    /**
     * Make a move.
     */
    @PostMapping("/games/{gameId}/moves")
    public ResponseEntity<?> makeMove(@PathVariable String gameId, @RequestBody MoveRequest request) {
        TetherChessEngine engine = games.get(gameId);
        if (engine == null) {
            return ResponseEntity.notFound().build();
        }

        TetherChessEngine.MoveResult result;

        if (request.getNotation() != null && !request.getNotation().isEmpty()) {
            // Use full notation
            result = engine.makeMove(request.getNotation());
        } else if (request.getFrom() != null && request.getTo() != null) {
            // Use from/to squares
            try {
                Position from = Position.fromAlgebraic(request.getFrom());
                Position to = Position.fromAlgebraic(request.getTo());
                PieceType promotion = null;

                if (request.getPromotion() != null) {
                    switch (request.getPromotion().toUpperCase()) {
                        case "Q": promotion = PieceType.QUEEN; break;
                        case "R": promotion = PieceType.ROOK; break;
                        case "B": promotion = PieceType.BISHOP; break;
                        case "N": promotion = PieceType.KNIGHT; break;
                    }
                }

                result = engine.makeMove(from, to, promotion);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(MoveResponse.error("Invalid position: " + e.getMessage(), engine));
            }
        } else {
            return ResponseEntity.badRequest().body(MoveResponse.error("Must provide 'notation' or 'from'/'to' fields", engine));
        }

        return ResponseEntity.ok(MoveResponse.fromResult(result, engine));
    }

    /**
     * Get legal moves for a specific piece.
     */
    @GetMapping("/games/{gameId}/pieces/{position}/moves")
    public ResponseEntity<?> getPieceMoves(@PathVariable String gameId, @PathVariable String position) {
        TetherChessEngine engine = games.get(gameId);
        if (engine == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Position pos = Position.fromAlgebraic(position);
            List<Move> moves = engine.getLegalMovesForPiece(pos);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("position", position);
            response.put("piece", engine.getBoard().getPieceAt(pos) != null ?
                    engine.getBoard().getPieceAt(pos).toString() : null);

            // Separate native and transporter moves
            List<String> nativeMoves = moves.stream()
                    .filter(m -> !m.isTransporterMove())
                    .map(Move::toString)
                    .collect(Collectors.toList());

            List<Map<String, Object>> transporterMoves = moves.stream()
                    .filter(Move::isTransporterMove)
                    .map(m -> {
                        Map<String, Object> tm = new LinkedHashMap<>();
                        tm.put("notation", m.toString());
                        tm.put("borrowedFrom", m.getBorrowedFromType().toString());
                        tm.put("isPawnKnightApex", m.isPawnKnightApex());
                        return tm;
                    })
                    .collect(Collectors.toList());

            response.put("nativeMoves", nativeMoves);
            response.put("transporterMoves", transporterMoves);

            // Show rank-mates (pieces sharing movement)
            List<String> rankMates = engine.getRankMatesPositions(pos).stream()
                    .map(Position::toString)
                    .collect(Collectors.toList());
            response.put("rankMates", rankMates);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid position: " + e.getMessage()));
        }
    }

    /**
     * Get the "Michael Tal" opening moves (Move 1 only).
     */
    @GetMapping("/games/{gameId}/analysis/michael-tal")
    public ResponseEntity<?> getMichaelTalMoves(@PathVariable String gameId) {
        TetherChessEngine engine = games.get(gameId);
        if (engine == null) {
            return ResponseEntity.notFound().build();
        }

        List<Move> talMoves = engine.getMichaelTalOpeningMoves();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("description", "The Michael Tal Opening: Use back-rank Knights to teleport Queen or Rooks over the pawn wall on Move 1");
        response.put("available", !talMoves.isEmpty());
        response.put("moves", talMoves.stream().map(m -> {
            Map<String, Object> moveInfo = new LinkedHashMap<>();
            moveInfo.put("notation", m.toString());
            moveInfo.put("piece", m.getPiece().getType().toString());
            moveInfo.put("from", m.getFrom().toString());
            moveInfo.put("to", m.getTo().toString());
            moveInfo.put("borrowsFromKnight", m.getBorrowedFromPiece().getPosition().toString());
            return moveInfo;
        }).collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    /**
     * Get all Pawn-Knight Apex moves available.
     */
    @GetMapping("/games/{gameId}/analysis/pawn-knight-apex")
    public ResponseEntity<?> getPawnKnightApexMoves(@PathVariable String gameId) {
        TetherChessEngine engine = games.get(gameId);
        if (engine == null) {
            return ResponseEntity.notFound().build();
        }

        List<Move> apexMoves = engine.getPawnKnightApexMoves();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("description", "Pawn-Knight Apex: Pawns that can use Knight's L-jump to reach 8th rank for INSTANT promotion");
        response.put("available", !apexMoves.isEmpty());
        response.put("moves", apexMoves.stream().map(m -> {
            Map<String, Object> moveInfo = new LinkedHashMap<>();
            moveInfo.put("notation", m.toString());
            moveInfo.put("pawnFrom", m.getFrom().toString());
            moveInfo.put("promotionSquare", m.getTo().toString());
            moveInfo.put("promotesTo", m.getPromotionType().toString());
            moveInfo.put("borrowsFromKnight", m.getBorrowedFromPiece().getPosition().toString());
            return moveInfo;
        }).collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    /**
     * Get moves that give check (using Native Lethality rule).
     */
    @GetMapping("/games/{gameId}/analysis/checking-moves")
    public ResponseEntity<?> getCheckingMoves(@PathVariable String gameId) {
        TetherChessEngine engine = games.get(gameId);
        if (engine == null) {
            return ResponseEntity.notFound().build();
        }

        List<Move> checkingMoves = engine.getCheckingMoves();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("description", "Moves that deliver check via NATIVE movement (Native Lethality rule)");
        response.put("count", checkingMoves.size());
        response.put("moves", checkingMoves.stream().map(m -> {
            Map<String, Object> moveInfo = new LinkedHashMap<>();
            moveInfo.put("notation", m.toString());
            moveInfo.put("piece", m.getPiece().getType().toString());
            moveInfo.put("isTransporter", m.isTransporterMove());
            if (m.isTransporterMove()) {
                moveInfo.put("note", "Uses transporter to reach position, but check is via NATIVE movement");
            }
            return moveInfo;
        }).collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a game.
     */
    @DeleteMapping("/games/{gameId}")
    public ResponseEntity<?> deleteGame(@PathVariable String gameId) {
        TetherChessEngine removed = games.remove(gameId);
        if (removed == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("message", "Game deleted", "gameId", gameId));
    }
}
