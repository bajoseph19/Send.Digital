package com.varscon.sendcorp.SendCorp.tetherchess.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a chess piece in Tether Chess.
 * Each piece knows its native movement rules, which are critical for:
 * - Sharing movement potential with rank-mates (Geometric Entanglement)
 * - Determining what squares can deliver check (Native Lethality)
 */
public class Piece {
    private final PieceType type;
    private final boolean isWhite;
    private Position position;
    private boolean hasMoved;

    public Piece(PieceType type, boolean isWhite, Position position) {
        this.type = type;
        this.isWhite = isWhite;
        this.position = position;
        this.hasMoved = false;
    }

    public PieceType getType() {
        return type;
    }

    public boolean isWhite() {
        return isWhite;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
        this.hasMoved = true;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    /**
     * Calculate native movement vectors for this piece type.
     * These vectors represent the "soul" of the piece -
     * only these movements can deliver check (Native Lethality rule).
     *
     * Returns list of [dx, dy, isSliding] where isSliding indicates
     * if the move continues along the vector until blocked.
     */
    public List<int[]> getNativeMovementVectors() {
        List<int[]> vectors = new ArrayList<>();

        switch (type) {
            case KING:
                // King moves one square in any direction
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx != 0 || dy != 0) {
                            vectors.add(new int[]{dx, dy, 0}); // 0 = not sliding
                        }
                    }
                }
                break;

            case QUEEN:
                // Queen slides in all 8 directions
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx != 0 || dy != 0) {
                            vectors.add(new int[]{dx, dy, 1}); // 1 = sliding
                        }
                    }
                }
                break;

            case ROOK:
                // Rook slides horizontally and vertically
                vectors.add(new int[]{1, 0, 1});
                vectors.add(new int[]{-1, 0, 1});
                vectors.add(new int[]{0, 1, 1});
                vectors.add(new int[]{0, -1, 1});
                break;

            case BISHOP:
                // Bishop slides diagonally
                vectors.add(new int[]{1, 1, 1});
                vectors.add(new int[]{1, -1, 1});
                vectors.add(new int[]{-1, 1, 1});
                vectors.add(new int[]{-1, -1, 1});
                break;

            case KNIGHT:
                // Knight jumps in L-shape (critical for Pawn-Knight Apex)
                vectors.add(new int[]{2, 1, 0});
                vectors.add(new int[]{2, -1, 0});
                vectors.add(new int[]{-2, 1, 0});
                vectors.add(new int[]{-2, -1, 0});
                vectors.add(new int[]{1, 2, 0});
                vectors.add(new int[]{1, -2, 0});
                vectors.add(new int[]{-1, 2, 0});
                vectors.add(new int[]{-1, -2, 0});
                break;

            case PAWN:
                // Pawn movement depends on color
                int direction = isWhite ? 1 : -1;
                vectors.add(new int[]{0, direction, 0}); // Forward move
                // Double move from starting position handled separately
                // Captures handled separately
                break;
        }

        return vectors;
    }

    /**
     * Get squares this piece can natively attack from its current position.
     * Used for Native Lethality check validation.
     */
    public List<Position> getNativeAttackSquares() {
        List<Position> squares = new ArrayList<>();

        if (type == PieceType.PAWN) {
            // Pawns attack diagonally
            int direction = isWhite ? 1 : -1;
            Position leftCapture = position.offset(-1, direction);
            Position rightCapture = position.offset(1, direction);
            if (leftCapture.isValid()) squares.add(leftCapture);
            if (rightCapture.isValid()) squares.add(rightCapture);
        } else {
            // Other pieces attack along their movement vectors
            for (int[] vector : getNativeMovementVectors()) {
                int dx = vector[0];
                int dy = vector[1];
                boolean isSliding = vector[2] == 1;

                if (isSliding) {
                    // Sliding pieces attack all squares along the ray
                    for (int i = 1; i < 8; i++) {
                        Position target = position.offset(dx * i, dy * i);
                        if (target.isValid()) {
                            squares.add(target);
                        } else {
                            break;
                        }
                    }
                } else {
                    // Non-sliding pieces attack specific squares
                    Position target = position.offset(dx, dy);
                    if (target.isValid()) {
                        squares.add(target);
                    }
                }
            }
        }

        return squares;
    }

    /**
     * Check if this piece can natively attack a target square from a given origin.
     * This is used to validate Native Lethality after a transporter move.
     */
    public boolean canNativelyAttack(Position origin, Position target) {
        if (type == PieceType.PAWN) {
            int direction = isWhite ? 1 : -1;
            int dx = target.getX() - origin.getX();
            int dy = target.getY() - origin.getY();
            return Math.abs(dx) == 1 && dy == direction;
        }

        for (int[] vector : getNativeMovementVectors()) {
            int vx = vector[0];
            int vy = vector[1];
            boolean isSliding = vector[2] == 1;

            int dx = target.getX() - origin.getX();
            int dy = target.getY() - origin.getY();

            if (isSliding) {
                // Check if target is along this ray
                if (vx == 0 && dx != 0) continue;
                if (vy == 0 && dy != 0) continue;
                if (vx != 0 && vy != 0) {
                    if (Math.abs(dx) != Math.abs(dy)) continue;
                    if (Integer.signum(dx) != vx || Integer.signum(dy) != vy) continue;
                } else if (vx != 0) {
                    if (dy != 0 || Integer.signum(dx) != vx) continue;
                } else {
                    if (dx != 0 || Integer.signum(dy) != vy) continue;
                }
                return true;
            } else {
                if (dx == vx && dy == vy) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public String toString() {
        String symbol = type.getSymbol();
        return isWhite ? symbol : symbol.toLowerCase();
    }

    /**
     * Create a copy of this piece.
     */
    public Piece copy() {
        Piece copy = new Piece(type, isWhite, position);
        copy.hasMoved = this.hasMoved;
        return copy;
    }
}
