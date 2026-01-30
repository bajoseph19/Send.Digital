package com.varscon.sendcorp.SendCorp.tetherchess.api.dto;

/**
 * Request DTO for making a move in Tether Chess.
 */
public class MoveRequest {
    private String from;        // Algebraic notation (e.g., "e2")
    private String to;          // Algebraic notation (e.g., "e4")
    private String promotion;   // Optional: "Q", "R", "B", "N" for pawn promotion
    private String notation;    // Alternative: full move notation (e.g., "e2-e4", "O-O")

    public MoveRequest() {}

    public MoveRequest(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getPromotion() { return promotion; }
    public void setPromotion(String promotion) { this.promotion = promotion; }

    public String getNotation() { return notation; }
    public void setNotation(String notation) { this.notation = notation; }
}
