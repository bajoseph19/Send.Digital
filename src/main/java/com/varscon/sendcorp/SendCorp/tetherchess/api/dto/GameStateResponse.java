package com.varscon.sendcorp.SendCorp.tetherchess.api.dto;

import com.varscon.sendcorp.SendCorp.tetherchess.engine.TetherChessEngine;
import com.varscon.sendcorp.SendCorp.tetherchess.models.Move;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO for Tether Chess game state.
 */
public class GameStateResponse {
    private String boardDisplay;
    private String gameState;
    private boolean whiteToMove;
    private boolean inCheck;
    private List<String> legalMoves;
    private List<String> transporterMoves;
    private List<String> pawnKnightApexMoves;
    private List<String> gameLog;
    private TetherChessRules rules;

    public GameStateResponse() {
        this.rules = new TetherChessRules();
    }

    public static GameStateResponse fromEngine(TetherChessEngine engine) {
        GameStateResponse response = new GameStateResponse();
        response.setBoardDisplay(engine.getBoardDisplay());
        response.setGameState(engine.getGameState().toString());
        response.setWhiteToMove(engine.isWhiteToMove());
        response.setInCheck(engine.getBoard().isInCheck());
        response.setLegalMoves(engine.getLegalMoves().stream()
                .map(Move::toString)
                .collect(Collectors.toList()));
        response.setTransporterMoves(engine.getTransporterMoves().stream()
                .map(Move::toString)
                .collect(Collectors.toList()));
        response.setPawnKnightApexMoves(engine.getPawnKnightApexMoves().stream()
                .map(Move::toString)
                .collect(Collectors.toList()));
        response.setGameLog(engine.getGameLog());
        return response;
    }

    // Getters and Setters
    public String getBoardDisplay() { return boardDisplay; }
    public void setBoardDisplay(String boardDisplay) { this.boardDisplay = boardDisplay; }

    public String getGameState() { return gameState; }
    public void setGameState(String gameState) { this.gameState = gameState; }

    public boolean isWhiteToMove() { return whiteToMove; }
    public void setWhiteToMove(boolean whiteToMove) { this.whiteToMove = whiteToMove; }

    public boolean isInCheck() { return inCheck; }
    public void setInCheck(boolean inCheck) { this.inCheck = inCheck; }

    public List<String> getLegalMoves() { return legalMoves; }
    public void setLegalMoves(List<String> legalMoves) { this.legalMoves = legalMoves; }

    public List<String> getTransporterMoves() { return transporterMoves; }
    public void setTransporterMoves(List<String> transporterMoves) { this.transporterMoves = transporterMoves; }

    public List<String> getPawnKnightApexMoves() { return pawnKnightApexMoves; }
    public void setPawnKnightApexMoves(List<String> pawnKnightApexMoves) { this.pawnKnightApexMoves = pawnKnightApexMoves; }

    public List<String> getGameLog() { return gameLog; }
    public void setGameLog(List<String> gameLog) { this.gameLog = gameLog; }

    public TetherChessRules getRules() { return rules; }
    public void setRules(TetherChessRules rules) { this.rules = rules; }

    /**
     * Embedded rules summary for API consumers.
     */
    public static class TetherChessRules {
        private final String name = "Tether Chess (Tal's Forest)";
        private final String rule1 = "RANK ENTANGLEMENT: Pieces on the same horizontal rank share movement potential with friendly pieces";
        private final String rule2 = "PAWN-KNIGHT APEX: A Pawn using Knight's L-jump to the 8th rank promotes immediately";
        private final String rule3 = "NATIVE LETHALITY: Only native movement can deliver check/checkmate";
        private final String rule4 = "NO RECURSIVE JUMPING: Cannot chain borrowed moves (one teleport per turn)";
        private final String openingTip = "THE MICHAEL TAL: Use back-rank Knights to teleport Queen/Rooks over the pawn wall on Move 1";

        public String getName() { return name; }
        public String getRule1() { return rule1; }
        public String getRule2() { return rule2; }
        public String getRule3() { return rule3; }
        public String getRule4() { return rule4; }
        public String getOpeningTip() { return openingTip; }
    }
}
