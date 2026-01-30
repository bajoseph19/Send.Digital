package com.varscon.sendcorp.SendCorp.tetherchess.models;

/**
 * Represents a move in Tether Chess.
 * Tracks whether the move uses Geometric Entanglement (transporter move)
 * and what piece type's movement was borrowed.
 */
public class Move {
    private final Position from;
    private final Position to;
    private final Piece piece;
    private final Piece capturedPiece;
    private final boolean isTransporterMove;
    private final PieceType borrowedFromType; // The piece type whose movement was borrowed
    private final Piece borrowedFromPiece;    // The actual piece that shared movement
    private final PieceType promotionType;    // For pawn promotion (including Pawn-Knight Apex)
    private final boolean isCastling;
    private final boolean isEnPassant;

    private Move(Builder builder) {
        this.from = builder.from;
        this.to = builder.to;
        this.piece = builder.piece;
        this.capturedPiece = builder.capturedPiece;
        this.isTransporterMove = builder.isTransporterMove;
        this.borrowedFromType = builder.borrowedFromType;
        this.borrowedFromPiece = builder.borrowedFromPiece;
        this.promotionType = builder.promotionType;
        this.isCastling = builder.isCastling;
        this.isEnPassant = builder.isEnPassant;
    }

    public Position getFrom() {
        return from;
    }

    public Position getTo() {
        return to;
    }

    public Piece getPiece() {
        return piece;
    }

    public Piece getCapturedPiece() {
        return capturedPiece;
    }

    public boolean isCapture() {
        return capturedPiece != null;
    }

    /**
     * Returns true if this move uses Geometric Entanglement.
     * The piece borrowed movement from a friendly piece on the same rank.
     */
    public boolean isTransporterMove() {
        return isTransporterMove;
    }

    /**
     * Returns the type of piece whose movement was borrowed.
     * Null if this is a native move.
     */
    public PieceType getBorrowedFromType() {
        return borrowedFromType;
    }

    /**
     * Returns the actual piece that shared its movement.
     */
    public Piece getBorrowedFromPiece() {
        return borrowedFromPiece;
    }

    /**
     * Returns true if this move results in pawn promotion.
     * In Tether Chess, this includes Pawn-Knight Apex promotions.
     */
    public boolean isPromotion() {
        return promotionType != null;
    }

    public PieceType getPromotionType() {
        return promotionType;
    }

    /**
     * Returns true if this is a Pawn-Knight Apex move:
     * A pawn using a Knight's jump to reach the 8th rank.
     */
    public boolean isPawnKnightApex() {
        return piece.getType() == PieceType.PAWN
                && isTransporterMove
                && borrowedFromType == PieceType.KNIGHT
                && isPromotion();
    }

    public boolean isCastling() {
        return isCastling;
    }

    public boolean isEnPassant() {
        return isEnPassant;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (isCastling) {
            return to.getX() > from.getX() ? "O-O" : "O-O-O";
        }

        // Piece symbol (empty for pawn)
        if (piece.getType() != PieceType.PAWN) {
            sb.append(piece.getType().getSymbol());
        }

        // Transporter notation: use ~ to indicate borrowed movement
        if (isTransporterMove) {
            sb.append("~").append(borrowedFromType.getSymbol());
        }

        sb.append(from.toString());

        // Capture indicator
        if (isCapture()) {
            sb.append("x");
        } else {
            sb.append("-");
        }

        sb.append(to.toString());

        // Promotion
        if (isPromotion()) {
            sb.append("=").append(promotionType.getSymbol());
            if (isPawnKnightApex()) {
                sb.append("!"); // Mark Pawn-Knight Apex moves
            }
        }

        return sb.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Position from;
        private Position to;
        private Piece piece;
        private Piece capturedPiece;
        private boolean isTransporterMove;
        private PieceType borrowedFromType;
        private Piece borrowedFromPiece;
        private PieceType promotionType;
        private boolean isCastling;
        private boolean isEnPassant;

        public Builder from(Position from) {
            this.from = from;
            return this;
        }

        public Builder to(Position to) {
            this.to = to;
            return this;
        }

        public Builder piece(Piece piece) {
            this.piece = piece;
            return this;
        }

        public Builder capturedPiece(Piece capturedPiece) {
            this.capturedPiece = capturedPiece;
            return this;
        }

        public Builder transporter(Piece borrowedFromPiece) {
            this.isTransporterMove = true;
            this.borrowedFromPiece = borrowedFromPiece;
            this.borrowedFromType = borrowedFromPiece.getType();
            return this;
        }

        public Builder promotion(PieceType promotionType) {
            this.promotionType = promotionType;
            return this;
        }

        public Builder castling(boolean isCastling) {
            this.isCastling = isCastling;
            return this;
        }

        public Builder enPassant(boolean isEnPassant) {
            this.isEnPassant = isEnPassant;
            return this;
        }

        public Move build() {
            if (from == null || to == null || piece == null) {
                throw new IllegalStateException("Move requires from, to, and piece");
            }
            return new Move(this);
        }
    }
}
