package com.varscon.sendcorp.SendCorp.tetherchess.models;

/**
 * Represents a position on the chess board.
 * Uses 0-indexed coordinates where:
 * - x (file): 0-7 maps to a-h
 * - y (rank): 0-7 maps to 1-8
 */
public class Position {
    private final int x; // file (0-7 = a-h)
    private final int y; // rank (0-7 = 1-8)

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    /**
     * Get the rank (y-coordinate) for geometric entanglement calculations.
     * Pieces on the same rank share movement potential.
     */
    public int getRank() {
        return y;
    }

    /**
     * Get the file (x-coordinate).
     */
    public int getFile() {
        return x;
    }

    public boolean isValid() {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }

    public Position offset(int dx, int dy) {
        return new Position(x + dx, y + dy);
    }

    /**
     * Calculate the vector from this position to another.
     */
    public int[] vectorTo(Position other) {
        return new int[]{other.x - this.x, other.y - this.y};
    }

    /**
     * Check if this position is on the promotion rank for a given color.
     */
    public boolean isPromotionRank(boolean isWhite) {
        return isWhite ? y == 7 : y == 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Position position = (Position) obj;
        return x == position.x && y == position.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    @Override
    public String toString() {
        char file = (char) ('a' + x);
        int rank = y + 1;
        return "" + file + rank;
    }

    /**
     * Parse algebraic notation (e.g., "e4") to Position.
     */
    public static Position fromAlgebraic(String notation) {
        if (notation == null || notation.length() != 2) {
            throw new IllegalArgumentException("Invalid notation: " + notation);
        }
        char file = notation.charAt(0);
        char rank = notation.charAt(1);
        int x = file - 'a';
        int y = rank - '1';
        Position pos = new Position(x, y);
        if (!pos.isValid()) {
            throw new IllegalArgumentException("Position out of bounds: " + notation);
        }
        return pos;
    }
}
