package com.varscon.sendcorp.SendCorp.tetherchess.models;

/**
 * Tether Chess piece types.
 * Each piece has native movement rules that determine:
 * 1. What moves it can share with rank-mates (Geometric Entanglement)
 * 2. What moves can deliver check/checkmate (Native Lethality)
 */
public enum PieceType {
    KING("K", false),
    QUEEN("Q", true),
    ROOK("R", true),
    BISHOP("B", true),
    KNIGHT("N", false),
    PAWN("P", false);

    private final String symbol;
    private final boolean slidingPiece; // For path integrity validation

    PieceType(String symbol, boolean slidingPiece) {
        this.symbol = symbol;
        this.slidingPiece = slidingPiece;
    }

    public String getSymbol() {
        return symbol;
    }

    /**
     * Sliding pieces (Queen, Rook, Bishop) require path integrity checks
     * when their movement is borrowed via Geometric Entanglement.
     */
    public boolean isSlidingPiece() {
        return slidingPiece;
    }

    /**
     * Knights have special significance in Tether Chess:
     * Pawns sharing a rank with a Knight can use the L-jump,
     * enabling instant promotion (Pawn-Knight Apex).
     */
    public boolean isKnight() {
        return this == KNIGHT;
    }

    /**
     * Pawns have unique promotion mechanics in Tether Chess.
     */
    public boolean isPawn() {
        return this == PAWN;
    }

    public boolean isKing() {
        return this == KING;
    }
}
