package com.varscon.sendcorp.SendCorp.tetherchess.models;

import java.util.*;

/**
 * Tether Chess Board - implements the Geometric Entanglement system.
 *
 * FOUR UNIQUE RULES OF TETHER CHESS (Tal's Forest):
 * 1. RANK ENTANGLEMENT: Pieces on the same horizontal rank share movement potential
 * 2. INSTANT PAWN PROMOTION: Pawn using Knight's L-jump to 8th rank promotes immediately
 * 3. NATIVE LETHALITY: Only native movement can deliver check/checkmate
 * 4. NO RECURSIVE JUMPING: Cannot chain borrowed moves (one teleport per turn)
 *
 * THE DISCONNECTION:
 * When a piece moves from Rank A to Rank B, it IMMEDIATELY loses all borrowed
 * capabilities from Rank A allies. On the next turn, it gains new capabilities
 * from any allies on Rank B. The entanglement is position-based, not persistent.
 */
public class Board {
    private final Piece[][] squares;
    private boolean whiteToMove;
    private Position enPassantTarget;
    private boolean whiteCanCastleKingside;
    private boolean whiteCanCastleQueenside;
    private boolean blackCanCastleKingside;
    private boolean blackCanCastleQueenside;
    private final List<Move> moveHistory;

    public Board() {
        this.squares = new Piece[8][8];
        this.whiteToMove = true;
        this.moveHistory = new ArrayList<>();
        this.whiteCanCastleKingside = true;
        this.whiteCanCastleQueenside = true;
        this.blackCanCastleKingside = true;
        this.blackCanCastleQueenside = true;
    }

    /**
     * Initialize board to standard starting position.
     */
    public void setupStartingPosition() {
        // Clear board
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                squares[x][y] = null;
            }
        }

        // White pieces
        squares[0][0] = new Piece(PieceType.ROOK, true, new Position(0, 0));
        squares[1][0] = new Piece(PieceType.KNIGHT, true, new Position(1, 0));
        squares[2][0] = new Piece(PieceType.BISHOP, true, new Position(2, 0));
        squares[3][0] = new Piece(PieceType.QUEEN, true, new Position(3, 0));
        squares[4][0] = new Piece(PieceType.KING, true, new Position(4, 0));
        squares[5][0] = new Piece(PieceType.BISHOP, true, new Position(5, 0));
        squares[6][0] = new Piece(PieceType.KNIGHT, true, new Position(6, 0));
        squares[7][0] = new Piece(PieceType.ROOK, true, new Position(7, 0));

        for (int x = 0; x < 8; x++) {
            squares[x][1] = new Piece(PieceType.PAWN, true, new Position(x, 1));
        }

        // Black pieces
        squares[0][7] = new Piece(PieceType.ROOK, false, new Position(0, 7));
        squares[1][7] = new Piece(PieceType.KNIGHT, false, new Position(1, 7));
        squares[2][7] = new Piece(PieceType.BISHOP, false, new Position(2, 7));
        squares[3][7] = new Piece(PieceType.QUEEN, false, new Position(3, 7));
        squares[4][7] = new Piece(PieceType.KING, false, new Position(4, 7));
        squares[5][7] = new Piece(PieceType.BISHOP, false, new Position(5, 7));
        squares[6][7] = new Piece(PieceType.KNIGHT, false, new Position(6, 7));
        squares[7][7] = new Piece(PieceType.ROOK, false, new Position(7, 7));

        for (int x = 0; x < 8; x++) {
            squares[x][6] = new Piece(PieceType.PAWN, false, new Position(x, 6));
        }

        whiteToMove = true;
        whiteCanCastleKingside = true;
        whiteCanCastleQueenside = true;
        blackCanCastleKingside = true;
        blackCanCastleQueenside = true;
        enPassantTarget = null;
        moveHistory.clear();
    }

    public Piece getPieceAt(Position pos) {
        if (!pos.isValid()) return null;
        return squares[pos.getX()][pos.getY()];
    }

    public Piece getPieceAt(int x, int y) {
        if (x < 0 || x >= 8 || y < 0 || y >= 8) return null;
        return squares[x][y];
    }

    public void setPieceAt(Position pos, Piece piece) {
        squares[pos.getX()][pos.getY()] = piece;
        if (piece != null) {
            piece.setPosition(pos);
        }
    }

    public boolean isWhiteToMove() {
        return whiteToMove;
    }

    public Position getEnPassantTarget() {
        return enPassantTarget;
    }

    public List<Move> getMoveHistory() {
        return Collections.unmodifiableList(moveHistory);
    }

    // ========================================================================
    // TETHER CHESS CORE ALGORITHM: The Three Checks
    // ========================================================================

    /**
     * STEP 1: identifyRankMates(piece)
     * Scans the current y-axis (rank) for friendly units.
     * These are the pieces that share their movement potential.
     *
     * THE DISCONNECTION: This method uses the piece's CURRENT position.
     * When a piece moves from Rank 3 to Rank 4, on its next turn:
     * - It NO LONGER has access to Rank 3 allies' movement
     * - It NOW has access to any Rank 4 allies' movement
     * The entanglement is based on current position, not history.
     *
     * @param piece The piece to find rank-mates for
     * @return List of friendly pieces on the same rank (excluding the piece itself)
     */
    public List<Piece> identifyRankMates(Piece piece) {
        List<Piece> rankMates = new ArrayList<>();
        // THE DISCONNECTION: Always use current position for rank calculation
        int rank = piece.getPosition().getRank();

        for (int x = 0; x < 8; x++) {
            Piece other = squares[x][rank];
            if (other != null && other != piece && other.isWhite() == piece.isWhite()) {
                rankMates.add(other);
            }
        }

        return rankMates;
    }

    /**
     * STEP 2: calculateTransporterVector(piece, mates)
     * Generates a move-set based on the union of all mates' native moves
     * applied to the piece's origin position.
     *
     * KEY RULES ENFORCED:
     * - Path Integrity: Sliding piece moves must have clear paths from origin
     * - No Recursive Jumping: Each transporter move is marked, cannot chain
     *
     * @param piece The piece that wants to borrow movement
     * @param mates The rank-mates that can share movement
     * @return List of possible transporter moves
     */
    public List<Move> calculateTransporterVector(Piece piece, List<Piece> mates) {
        List<Move> transporterMoves = new ArrayList<>();
        Position origin = piece.getPosition();

        for (Piece mate : mates) {
            // Get the mate's native movement vectors
            List<int[]> vectors = mate.getNativeMovementVectors();

            for (int[] vector : vectors) {
                int dx = vector[0];
                int dy = vector[1];
                boolean isSliding = vector[2] == 1;

                if (isSliding) {
                    // Sliding moves: check path integrity from origin
                    for (int distance = 1; distance < 8; distance++) {
                        Position target = origin.offset(dx * distance, dy * distance);
                        if (!target.isValid()) break;

                        Piece targetPiece = getPieceAt(target);

                        // Path must be clear (path integrity rule)
                        if (targetPiece != null) {
                            // Can capture enemy piece
                            if (targetPiece.isWhite() != piece.isWhite()) {
                                // Special handling for pawn using borrowed movement
                                if (piece.getType() == PieceType.PAWN) {
                                    Move move = createPawnTransporterMove(piece, target, mate, targetPiece);
                                    if (move != null) transporterMoves.add(move);
                                } else {
                                    transporterMoves.add(Move.builder()
                                            .from(origin)
                                            .to(target)
                                            .piece(piece)
                                            .capturedPiece(targetPiece)
                                            .transporter(mate)
                                            .build());
                                }
                            }
                            break; // Path blocked
                        }

                        // Empty square - valid transporter destination
                        if (piece.getType() == PieceType.PAWN) {
                            Move move = createPawnTransporterMove(piece, target, mate, null);
                            if (move != null) transporterMoves.add(move);
                        } else {
                            transporterMoves.add(Move.builder()
                                    .from(origin)
                                    .to(target)
                                    .piece(piece)
                                    .transporter(mate)
                                    .build());
                        }
                    }
                } else {
                    // Non-sliding moves (Knight, King)
                    Position target = origin.offset(dx, dy);
                    if (!target.isValid()) continue;

                    Piece targetPiece = getPieceAt(target);

                    // Can't capture own pieces
                    if (targetPiece != null && targetPiece.isWhite() == piece.isWhite()) {
                        continue;
                    }

                    if (piece.getType() == PieceType.PAWN) {
                        Move move = createPawnTransporterMove(piece, target, mate, targetPiece);
                        if (move != null) transporterMoves.add(move);
                    } else {
                        Move.Builder builder = Move.builder()
                                .from(origin)
                                .to(target)
                                .piece(piece)
                                .transporter(mate);

                        if (targetPiece != null) {
                            builder.capturedPiece(targetPiece);
                        }

                        transporterMoves.add(builder.build());
                    }
                }
            }
        }

        return transporterMoves;
    }

    /**
     * Helper for pawn transporter moves - handles PAWN-KNIGHT APEX promotion.
     * If a pawn uses Knight's L-jump to reach 8th rank, instant promotion!
     */
    private Move createPawnTransporterMove(Piece pawn, Position target, Piece mate, Piece captured) {
        Move.Builder builder = Move.builder()
                .from(pawn.getPosition())
                .to(target)
                .piece(pawn)
                .transporter(mate);

        if (captured != null) {
            builder.capturedPiece(captured);
        }

        // PAWN-KNIGHT APEX: Instant promotion when Knight-jumping to 8th rank!
        if (target.isPromotionRank(pawn.isWhite())) {
            builder.promotion(PieceType.QUEEN); // Default to Queen, can be changed
        }

        return builder.build();
    }

    /**
     * STEP 3: validateLethality(move, enemyKingPosition)
     * After the move, checks if the active piece's NATIVE movement
     * intersects with the enemy King's position.
     *
     * NATIVE LETHALITY RULE:
     * A piece can only deliver check/checkmate using its own native movement.
     * A teleporting Rook landing next to a King does NOT check unless
     * the King is on the Rook's horizontal or vertical axis.
     *
     * @param piece The piece that just moved
     * @param piecePosition The position the piece moved to
     * @param enemyKingPosition The enemy king's position
     * @return true if the piece gives check via native movement
     */
    public boolean validateLethality(Piece piece, Position piecePosition, Position enemyKingPosition) {
        return piece.canNativelyAttack(piecePosition, enemyKingPosition);
    }

    /**
     * Check if a position is under attack by native movement only.
     * Used for check detection with Native Lethality rule.
     */
    public boolean isUnderNativeAttack(Position target, boolean byWhite) {
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                Piece attacker = squares[x][y];
                if (attacker != null && attacker.isWhite() == byWhite) {
                    if (canNativelyReach(attacker, target)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if a piece can natively reach a target square (with path checking for sliders).
     */
    private boolean canNativelyReach(Piece piece, Position target) {
        Position origin = piece.getPosition();

        if (piece.getType() == PieceType.PAWN) {
            // Pawn attacks diagonally
            int direction = piece.isWhite() ? 1 : -1;
            int dx = target.getX() - origin.getX();
            int dy = target.getY() - origin.getY();
            return Math.abs(dx) == 1 && dy == direction;
        }

        for (int[] vector : piece.getNativeMovementVectors()) {
            int vx = vector[0];
            int vy = vector[1];
            boolean isSliding = vector[2] == 1;

            if (isSliding) {
                // Check if target is along this ray with clear path
                int dx = target.getX() - origin.getX();
                int dy = target.getY() - origin.getY();

                if (!isAlongRay(dx, dy, vx, vy)) continue;

                // Check path is clear
                int steps = Math.max(Math.abs(dx), Math.abs(dy));
                boolean pathClear = true;
                for (int i = 1; i < steps; i++) {
                    Position intermediate = origin.offset(vx * i, vy * i);
                    if (getPieceAt(intermediate) != null) {
                        pathClear = false;
                        break;
                    }
                }
                if (pathClear) return true;
            } else {
                Position reached = origin.offset(vx, vy);
                if (reached.equals(target)) return true;
            }
        }

        return false;
    }

    private boolean isAlongRay(int dx, int dy, int vx, int vy) {
        if (dx == 0 && dy == 0) return false;
        if (vx == 0 && dx != 0) return false;
        if (vy == 0 && dy != 0) return false;
        if (vx != 0 && vy != 0) {
            if (Math.abs(dx) != Math.abs(dy)) return false;
        }
        if (vx != 0 && Integer.signum(dx) != vx) return false;
        if (vy != 0 && Integer.signum(dy) != vy) return false;
        return true;
    }

    // ========================================================================
    // Move Generation and Execution
    // ========================================================================

    /**
     * Generate all legal moves for the current side.
     * Includes both native moves and transporter moves.
     */
    public List<Move> generateAllLegalMoves() {
        List<Move> allMoves = new ArrayList<>();

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                Piece piece = squares[x][y];
                if (piece != null && piece.isWhite() == whiteToMove) {
                    allMoves.addAll(generateLegalMovesForPiece(piece));
                }
            }
        }

        return allMoves;
    }

    /**
     * Generate all legal moves for a specific piece.
     */
    public List<Move> generateLegalMovesForPiece(Piece piece) {
        List<Move> moves = new ArrayList<>();

        // Generate native moves
        moves.addAll(generateNativeMoves(piece));

        // Generate transporter moves (Geometric Entanglement)
        List<Piece> rankMates = identifyRankMates(piece);
        moves.addAll(calculateTransporterVector(piece, rankMates));

        // Filter out moves that leave king in check
        moves.removeIf(move -> leavesKingInCheck(move));

        return moves;
    }

    /**
     * Generate native (non-transporter) moves for a piece.
     */
    private List<Move> generateNativeMoves(Piece piece) {
        List<Move> moves = new ArrayList<>();
        Position origin = piece.getPosition();

        switch (piece.getType()) {
            case PAWN:
                moves.addAll(generatePawnMoves(piece));
                break;

            case KING:
                moves.addAll(generateKingMoves(piece));
                break;

            default:
                for (int[] vector : piece.getNativeMovementVectors()) {
                    int dx = vector[0];
                    int dy = vector[1];
                    boolean isSliding = vector[2] == 1;

                    if (isSliding) {
                        for (int dist = 1; dist < 8; dist++) {
                            Position target = origin.offset(dx * dist, dy * dist);
                            if (!target.isValid()) break;

                            Piece targetPiece = getPieceAt(target);
                            if (targetPiece != null) {
                                if (targetPiece.isWhite() != piece.isWhite()) {
                                    moves.add(Move.builder()
                                            .from(origin).to(target)
                                            .piece(piece).capturedPiece(targetPiece)
                                            .build());
                                }
                                break;
                            }
                            moves.add(Move.builder().from(origin).to(target).piece(piece).build());
                        }
                    } else {
                        Position target = origin.offset(dx, dy);
                        if (!target.isValid()) continue;

                        Piece targetPiece = getPieceAt(target);
                        if (targetPiece != null && targetPiece.isWhite() == piece.isWhite()) continue;

                        Move.Builder builder = Move.builder().from(origin).to(target).piece(piece);
                        if (targetPiece != null) builder.capturedPiece(targetPiece);
                        moves.add(builder.build());
                    }
                }
        }

        return moves;
    }

    private List<Move> generatePawnMoves(Piece pawn) {
        List<Move> moves = new ArrayList<>();
        Position origin = pawn.getPosition();
        int direction = pawn.isWhite() ? 1 : -1;
        int startRank = pawn.isWhite() ? 1 : 6;

        // Forward move
        Position oneForward = origin.offset(0, direction);
        if (oneForward.isValid() && getPieceAt(oneForward) == null) {
            if (oneForward.isPromotionRank(pawn.isWhite())) {
                for (PieceType promo : new PieceType[]{PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT}) {
                    moves.add(Move.builder().from(origin).to(oneForward).piece(pawn).promotion(promo).build());
                }
            } else {
                moves.add(Move.builder().from(origin).to(oneForward).piece(pawn).build());
            }

            // Double move from starting position
            if (origin.getRank() == startRank) {
                Position twoForward = origin.offset(0, direction * 2);
                if (getPieceAt(twoForward) == null) {
                    moves.add(Move.builder().from(origin).to(twoForward).piece(pawn).build());
                }
            }
        }

        // Captures
        for (int dx : new int[]{-1, 1}) {
            Position captureSquare = origin.offset(dx, direction);
            if (!captureSquare.isValid()) continue;

            Piece target = getPieceAt(captureSquare);
            if (target != null && target.isWhite() != pawn.isWhite()) {
                if (captureSquare.isPromotionRank(pawn.isWhite())) {
                    for (PieceType promo : new PieceType[]{PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT}) {
                        moves.add(Move.builder().from(origin).to(captureSquare).piece(pawn)
                                .capturedPiece(target).promotion(promo).build());
                    }
                } else {
                    moves.add(Move.builder().from(origin).to(captureSquare).piece(pawn)
                            .capturedPiece(target).build());
                }
            }

            // En passant
            if (enPassantTarget != null && captureSquare.equals(enPassantTarget)) {
                Piece epPawn = getPieceAt(origin.offset(dx, 0));
                if (epPawn != null && epPawn.getType() == PieceType.PAWN) {
                    moves.add(Move.builder().from(origin).to(captureSquare).piece(pawn)
                            .capturedPiece(epPawn).enPassant(true).build());
                }
            }
        }

        return moves;
    }

    private List<Move> generateKingMoves(Piece king) {
        List<Move> moves = new ArrayList<>();
        Position origin = king.getPosition();

        // Normal moves
        for (int[] vector : king.getNativeMovementVectors()) {
            Position target = origin.offset(vector[0], vector[1]);
            if (!target.isValid()) continue;

            Piece targetPiece = getPieceAt(target);
            if (targetPiece != null && targetPiece.isWhite() == king.isWhite()) continue;

            Move.Builder builder = Move.builder().from(origin).to(target).piece(king);
            if (targetPiece != null) builder.capturedPiece(targetPiece);
            moves.add(builder.build());
        }

        // Castling
        if (!king.hasMoved()) {
            boolean canKingside = king.isWhite() ? whiteCanCastleKingside : blackCanCastleKingside;
            boolean canQueenside = king.isWhite() ? whiteCanCastleQueenside : blackCanCastleQueenside;

            if (canKingside) {
                Position rookPos = new Position(7, origin.getRank());
                Piece rook = getPieceAt(rookPos);
                if (rook != null && rook.getType() == PieceType.ROOK && !rook.hasMoved()) {
                    if (getPieceAt(origin.offset(1, 0)) == null &&
                            getPieceAt(origin.offset(2, 0)) == null &&
                            !isUnderNativeAttack(origin, !king.isWhite()) &&
                            !isUnderNativeAttack(origin.offset(1, 0), !king.isWhite()) &&
                            !isUnderNativeAttack(origin.offset(2, 0), !king.isWhite())) {
                        moves.add(Move.builder().from(origin).to(origin.offset(2, 0))
                                .piece(king).castling(true).build());
                    }
                }
            }

            if (canQueenside) {
                Position rookPos = new Position(0, origin.getRank());
                Piece rook = getPieceAt(rookPos);
                if (rook != null && rook.getType() == PieceType.ROOK && !rook.hasMoved()) {
                    if (getPieceAt(origin.offset(-1, 0)) == null &&
                            getPieceAt(origin.offset(-2, 0)) == null &&
                            getPieceAt(origin.offset(-3, 0)) == null &&
                            !isUnderNativeAttack(origin, !king.isWhite()) &&
                            !isUnderNativeAttack(origin.offset(-1, 0), !king.isWhite()) &&
                            !isUnderNativeAttack(origin.offset(-2, 0), !king.isWhite())) {
                        moves.add(Move.builder().from(origin).to(origin.offset(-2, 0))
                                .piece(king).castling(true).build());
                    }
                }
            }
        }

        return moves;
    }

    /**
     * Check if a move leaves the moving side's king in check.
     */
    private boolean leavesKingInCheck(Move move) {
        // Make move temporarily
        Piece piece = move.getPiece();
        Position from = move.getFrom();
        Position to = move.getTo();
        Piece captured = getPieceAt(to);

        squares[from.getX()][from.getY()] = null;
        squares[to.getX()][to.getY()] = piece;
        Position originalPos = piece.getPosition();
        piece.setPosition(to);

        // Handle en passant capture
        Piece epCaptured = null;
        if (move.isEnPassant()) {
            Position epPos = new Position(to.getX(), from.getY());
            epCaptured = getPieceAt(epPos);
            squares[epPos.getX()][epPos.getY()] = null;
        }

        // Find king position
        Position kingPos = findKing(piece.isWhite());
        boolean inCheck = isUnderNativeAttack(kingPos, !piece.isWhite());

        // Unmake move
        squares[from.getX()][from.getY()] = piece;
        squares[to.getX()][to.getY()] = captured;
        piece.setPosition(originalPos);

        if (move.isEnPassant() && epCaptured != null) {
            Position epPos = new Position(to.getX(), from.getY());
            squares[epPos.getX()][epPos.getY()] = epCaptured;
        }

        return inCheck;
    }

    /**
     * Find the king of a given color.
     */
    public Position findKing(boolean isWhite) {
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                Piece piece = squares[x][y];
                if (piece != null && piece.getType() == PieceType.KING && piece.isWhite() == isWhite) {
                    return piece.getPosition();
                }
            }
        }
        return null;
    }

    /**
     * Execute a move on the board.
     */
    public void makeMove(Move move) {
        Position from = move.getFrom();
        Position to = move.getTo();
        Piece piece = move.getPiece();

        // Update en passant target
        enPassantTarget = null;
        if (piece.getType() == PieceType.PAWN) {
            int distance = Math.abs(to.getY() - from.getY());
            if (distance == 2) {
                int epRank = (from.getY() + to.getY()) / 2;
                enPassantTarget = new Position(from.getX(), epRank);
            }
        }

        // Handle en passant capture
        if (move.isEnPassant()) {
            Position epPos = new Position(to.getX(), from.getY());
            squares[epPos.getX()][epPos.getY()] = null;
        }

        // Handle castling
        if (move.isCastling()) {
            if (to.getX() > from.getX()) {
                // Kingside
                Piece rook = squares[7][from.getY()];
                squares[7][from.getY()] = null;
                squares[5][from.getY()] = rook;
                rook.setPosition(new Position(5, from.getY()));
            } else {
                // Queenside
                Piece rook = squares[0][from.getY()];
                squares[0][from.getY()] = null;
                squares[3][from.getY()] = rook;
                rook.setPosition(new Position(3, from.getY()));
            }
        }

        // Move piece
        squares[from.getX()][from.getY()] = null;

        // Handle promotion (including Pawn-Knight Apex)
        if (move.isPromotion()) {
            Piece promoted = new Piece(move.getPromotionType(), piece.isWhite(), to);
            squares[to.getX()][to.getY()] = promoted;
        } else {
            squares[to.getX()][to.getY()] = piece;
            piece.setPosition(to);
        }

        // Update castling rights
        if (piece.getType() == PieceType.KING) {
            if (piece.isWhite()) {
                whiteCanCastleKingside = false;
                whiteCanCastleQueenside = false;
            } else {
                blackCanCastleKingside = false;
                blackCanCastleQueenside = false;
            }
        }
        if (piece.getType() == PieceType.ROOK) {
            if (from.equals(new Position(0, 0))) whiteCanCastleQueenside = false;
            if (from.equals(new Position(7, 0))) whiteCanCastleKingside = false;
            if (from.equals(new Position(0, 7))) blackCanCastleQueenside = false;
            if (from.equals(new Position(7, 7))) blackCanCastleKingside = false;
        }

        moveHistory.add(move);
        whiteToMove = !whiteToMove;
    }

    /**
     * Check if the current side is in check.
     */
    public boolean isInCheck() {
        Position kingPos = findKing(whiteToMove);
        return isUnderNativeAttack(kingPos, !whiteToMove);
    }

    /**
     * Check if the current side is in checkmate.
     */
    public boolean isCheckmate() {
        if (!isInCheck()) return false;
        return generateAllLegalMoves().isEmpty();
    }

    /**
     * Check if the game is a stalemate.
     */
    public boolean isStalemate() {
        if (isInCheck()) return false;
        return generateAllLegalMoves().isEmpty();
    }

    /**
     * Get a text representation of the board.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  a b c d e f g h\n");
        for (int y = 7; y >= 0; y--) {
            sb.append(y + 1).append(" ");
            for (int x = 0; x < 8; x++) {
                Piece piece = squares[x][y];
                if (piece == null) {
                    sb.append(". ");
                } else {
                    sb.append(piece.toString()).append(" ");
                }
            }
            sb.append(y + 1).append("\n");
        }
        sb.append("  a b c d e f g h\n");
        sb.append(whiteToMove ? "White to move" : "Black to move");
        return sb.toString();
    }

    /**
     * Create a deep copy of this board.
     */
    public Board copy() {
        Board copy = new Board();
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                if (squares[x][y] != null) {
                    copy.squares[x][y] = squares[x][y].copy();
                }
            }
        }
        copy.whiteToMove = this.whiteToMove;
        copy.enPassantTarget = this.enPassantTarget;
        copy.whiteCanCastleKingside = this.whiteCanCastleKingside;
        copy.whiteCanCastleQueenside = this.whiteCanCastleQueenside;
        copy.blackCanCastleKingside = this.blackCanCastleKingside;
        copy.blackCanCastleQueenside = this.blackCanCastleQueenside;
        copy.moveHistory.addAll(this.moveHistory);
        return copy;
    }
}
