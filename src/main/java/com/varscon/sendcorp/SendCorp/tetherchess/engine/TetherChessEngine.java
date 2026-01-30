package com.varscon.sendcorp.SendCorp.tetherchess.engine;

import com.varscon.sendcorp.SendCorp.tetherchess.models.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TETHER CHESS ENGINE (Tal's Forest)
 *
 * The main orchestrator for the Tether Chess variant.
 * Implements the complete game logic including the Four Unique Rules:
 *
 * 1. RANK ENTANGLEMENT (Geometric Entanglement)
 *    Every piece on a horizontal rank shares its movement potential
 *    with all friendly pieces on that same rank.
 *
 * 2. INSTANT PAWN PROMOTION (Pawn-Knight Apex)
 *    A Pawn sharing a rank with a Knight can utilize the Knight's L-jump.
 *    If that jump lands the Pawn on the 8th rank, it promotes immediately.
 *
 * 3. NATIVE LETHALITY
 *    A piece can only deliver Check or Checkmate using its own native
 *    movement rules. A teleporting Rook landing next to a King does not
 *    check the King unless the King is on the Rook's file or rank.
 *
 * 4. NO RECURSIVE JUMPING
 *    Cannot chain borrowed moves. Each turn allows one transporter move.
 *    The borrowed movement cannot itself be used to borrow again.
 *
 * The "Michael Tal" opening: Using back-rank Knights to teleport the
 * Queen or Rooks over the pawn wall on Move 1.
 */
public class TetherChessEngine {

    private final Board board;
    private GameState gameState;
    private final List<String> gameLog;

    public enum GameState {
        ONGOING,
        WHITE_WINS_CHECKMATE,
        BLACK_WINS_CHECKMATE,
        STALEMATE,
        DRAW_BY_REPETITION,
        DRAW_BY_FIFTY_MOVES,
        DRAW_BY_INSUFFICIENT_MATERIAL
    }

    public TetherChessEngine() {
        this.board = new Board();
        this.gameState = GameState.ONGOING;
        this.gameLog = new ArrayList<>();
    }

    /**
     * Initialize a new game with standard starting position.
     */
    public void newGame() {
        board.setupStartingPosition();
        gameState = GameState.ONGOING;
        gameLog.clear();
        gameLog.add("=== TETHER CHESS (Tal's Forest) ===");
        gameLog.add("Game started. White to move.");
        gameLog.add("");
        gameLog.add("FOUR UNIQUE RULES:");
        gameLog.add("1. Rank Entanglement - Pieces share movement with rank-mates");
        gameLog.add("2. Pawn-Knight Apex - Pawn + Knight jump to 8th = instant promotion");
        gameLog.add("3. Native Lethality - Only native moves can deliver check");
        gameLog.add("4. No Recursive Jumping - One teleport per turn maximum");
        gameLog.add("");
    }

    /**
     * Get all legal moves for the current position.
     */
    public List<Move> getLegalMoves() {
        if (gameState != GameState.ONGOING) {
            return Collections.emptyList();
        }
        return board.generateAllLegalMoves();
    }

    /**
     * Get all legal moves for a specific piece.
     */
    public List<Move> getLegalMovesForPiece(Position position) {
        if (gameState != GameState.ONGOING) {
            return Collections.emptyList();
        }

        Piece piece = board.getPieceAt(position);
        if (piece == null || piece.isWhite() != board.isWhiteToMove()) {
            return Collections.emptyList();
        }

        return board.generateLegalMovesForPiece(piece);
    }

    /**
     * Get all transporter (borrowed movement) moves available.
     * These are the moves that use Geometric Entanglement.
     */
    public List<Move> getTransporterMoves() {
        return getLegalMoves().stream()
                .filter(Move::isTransporterMove)
                .collect(Collectors.toList());
    }

    /**
     * Get Pawn-Knight Apex moves (pawns that can promote via Knight jump).
     */
    public List<Move> getPawnKnightApexMoves() {
        return getLegalMoves().stream()
                .filter(Move::isPawnKnightApex)
                .collect(Collectors.toList());
    }

    /**
     * Execute a move given by algebraic notation (e.g., "e2-e4", "Nb1~Qd3").
     */
    public MoveResult makeMove(String notation) {
        if (gameState != GameState.ONGOING) {
            return new MoveResult(false, "Game is over: " + gameState);
        }

        // Parse the move
        Move move = parseMove(notation);
        if (move == null) {
            return new MoveResult(false, "Invalid move notation: " + notation);
        }

        return executeMove(move);
    }

    /**
     * Execute a move given by from and to positions.
     */
    public MoveResult makeMove(Position from, Position to) {
        return makeMove(from, to, null);
    }

    /**
     * Execute a move with optional promotion type.
     */
    public MoveResult makeMove(Position from, Position to, PieceType promotionType) {
        if (gameState != GameState.ONGOING) {
            return new MoveResult(false, "Game is over: " + gameState);
        }

        Piece piece = board.getPieceAt(from);
        if (piece == null) {
            return new MoveResult(false, "No piece at " + from);
        }

        if (piece.isWhite() != board.isWhiteToMove()) {
            return new MoveResult(false, "Not your turn");
        }

        // Find matching legal move
        List<Move> legalMoves = board.generateLegalMovesForPiece(piece);
        Move matchingMove = null;

        for (Move move : legalMoves) {
            if (move.getTo().equals(to)) {
                if (move.isPromotion()) {
                    if (promotionType != null && move.getPromotionType() == promotionType) {
                        matchingMove = move;
                        break;
                    } else if (promotionType == null && move.getPromotionType() == PieceType.QUEEN) {
                        matchingMove = move;
                        break;
                    }
                } else {
                    matchingMove = move;
                    break;
                }
            }
        }

        if (matchingMove == null) {
            return new MoveResult(false, "Illegal move: " + from + " to " + to);
        }

        return executeMove(matchingMove);
    }

    /**
     * Execute a move and update game state.
     */
    private MoveResult executeMove(Move move) {
        // Log the move
        String moveStr = move.toString();
        String annotation = "";

        // Check if this gives check (using Native Lethality)
        board.makeMove(move);

        boolean givesCheck = board.isInCheck();
        boolean isCheckmate = board.isCheckmate();
        boolean isStalemate = board.isStalemate();

        if (isCheckmate) {
            annotation = "#";
            gameState = board.isWhiteToMove() ? GameState.BLACK_WINS_CHECKMATE : GameState.WHITE_WINS_CHECKMATE;
        } else if (isStalemate) {
            gameState = GameState.STALEMATE;
        } else if (givesCheck) {
            annotation = "+";
        }

        // Build result message
        StringBuilder message = new StringBuilder();
        message.append(moveStr).append(annotation);

        if (move.isTransporterMove()) {
            message.append(" (Transporter: borrowed ").append(move.getBorrowedFromType()).append(" movement)");
        }

        if (move.isPawnKnightApex()) {
            message.append(" [PAWN-KNIGHT APEX! Instant promotion!]");
        }

        if (isCheckmate) {
            message.append(" CHECKMATE!");
        } else if (isStalemate) {
            message.append(" STALEMATE!");
        } else if (givesCheck) {
            message.append(" Check!");
        }

        gameLog.add(message.toString());

        return new MoveResult(true, message.toString(), move, givesCheck, isCheckmate);
    }

    /**
     * Parse move notation to find matching move.
     */
    private Move parseMove(String notation) {
        notation = notation.trim();

        // Handle castling
        if (notation.equals("O-O") || notation.equals("0-0")) {
            Position kingPos = board.findKing(board.isWhiteToMove());
            return findMoveInList(kingPos, kingPos.offset(2, 0));
        }
        if (notation.equals("O-O-O") || notation.equals("0-0-0")) {
            Position kingPos = board.findKing(board.isWhiteToMove());
            return findMoveInList(kingPos, kingPos.offset(-2, 0));
        }

        // Parse standard notation like "e2-e4" or "e2e4"
        notation = notation.replace("-", "").replace("x", "");

        if (notation.length() >= 4) {
            try {
                String fromStr = notation.substring(0, 2);
                String toStr = notation.substring(2, 4);
                Position from = Position.fromAlgebraic(fromStr);
                Position to = Position.fromAlgebraic(toStr);
                return findMoveInList(from, to);
            } catch (Exception e) {
                // Fall through to return null
            }
        }

        return null;
    }

    private Move findMoveInList(Position from, Position to) {
        Piece piece = board.getPieceAt(from);
        if (piece == null) return null;

        for (Move move : board.generateLegalMovesForPiece(piece)) {
            if (move.getTo().equals(to)) {
                return move;
            }
        }
        return null;
    }

    // ========================================================================
    // Strategic Analysis (for the "Quiet Respect" principle)
    // ========================================================================

    /**
     * Analyze if a piece at a given position can deliver check via Native Lethality
     * from any of its possible destination squares.
     */
    public List<Move> getCheckingMoves() {
        List<Move> checkingMoves = new ArrayList<>();
        Position enemyKing = board.findKing(!board.isWhiteToMove());

        for (Move move : getLegalMoves()) {
            // Check if landing at move.to allows native check on enemy king
            if (board.validateLethality(move.getPiece(), move.getTo(), enemyKing)) {
                // Verify the move is actually legal (doesn't leave own king in check)
                checkingMoves.add(move);
            }
        }

        return checkingMoves;
    }

    /**
     * Demonstrate the "Michael Tal" opening possibilities.
     * Shows how back-rank Knights can teleport pieces over the pawn wall.
     */
    public List<Move> getMichaelTalOpeningMoves() {
        if (board.getMoveHistory().size() > 0) {
            return Collections.emptyList(); // Only valid on move 1
        }

        List<Move> talMoves = new ArrayList<>();

        for (Move move : getLegalMoves()) {
            if (!move.isTransporterMove()) continue;

            // Check if borrowing from Knight on back rank
            Piece borrowedFrom = move.getBorrowedFromPiece();
            if (borrowedFrom == null) continue;

            int backRank = board.isWhiteToMove() ? 0 : 7;
            if (borrowedFrom.getType() == PieceType.KNIGHT &&
                    borrowedFrom.getPosition().getRank() == backRank) {

                // Check if the moving piece is also on back rank (Q or R)
                PieceType movingType = move.getPiece().getType();
                if ((movingType == PieceType.QUEEN || movingType == PieceType.ROOK) &&
                        move.getFrom().getRank() == backRank) {
                    talMoves.add(move);
                }
            }
        }

        return talMoves;
    }

    // ========================================================================
    // Getters and Utility Methods
    // ========================================================================

    public Board getBoard() {
        return board;
    }

    public GameState getGameState() {
        return gameState;
    }

    public boolean isWhiteToMove() {
        return board.isWhiteToMove();
    }

    public List<String> getGameLog() {
        return Collections.unmodifiableList(gameLog);
    }

    public String getBoardDisplay() {
        return board.toString();
    }

    /**
     * Get rank-mates for a piece (for UI highlighting).
     */
    public List<Position> getRankMatesPositions(Position piecePosition) {
        Piece piece = board.getPieceAt(piecePosition);
        if (piece == null) return Collections.emptyList();

        return board.identifyRankMates(piece).stream()
                .map(Piece::getPosition)
                .collect(Collectors.toList());
    }

    /**
     * Result of a move attempt.
     */
    public static class MoveResult {
        private final boolean success;
        private final String message;
        private final Move move;
        private final boolean givesCheck;
        private final boolean isCheckmate;

        public MoveResult(boolean success, String message) {
            this(success, message, null, false, false);
        }

        public MoveResult(boolean success, String message, Move move, boolean givesCheck, boolean isCheckmate) {
            this.success = success;
            this.message = message;
            this.move = move;
            this.givesCheck = givesCheck;
            this.isCheckmate = isCheckmate;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Move getMove() { return move; }
        public boolean givesCheck() { return givesCheck; }
        public boolean isCheckmate() { return isCheckmate; }
    }
}
