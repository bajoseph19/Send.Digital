package com.varscon.sendcorp.SendCorp.tetherchess.api.dto;

import com.varscon.sendcorp.SendCorp.tetherchess.engine.TetherChessEngine;

/**
 * Response DTO for move execution result.
 */
public class MoveResponse {
    private boolean success;
    private String message;
    private String moveNotation;
    private boolean isTransporterMove;
    private boolean isPawnKnightApex;
    private boolean givesCheck;
    private boolean isCheckmate;
    private GameStateResponse gameState;

    public static MoveResponse fromResult(TetherChessEngine.MoveResult result, TetherChessEngine engine) {
        MoveResponse response = new MoveResponse();
        response.setSuccess(result.isSuccess());
        response.setMessage(result.getMessage());

        if (result.isSuccess() && result.getMove() != null) {
            response.setMoveNotation(result.getMove().toString());
            response.setTransporterMove(result.getMove().isTransporterMove());
            response.setPawnKnightApex(result.getMove().isPawnKnightApex());
            response.setGivesCheck(result.givesCheck());
            response.setCheckmate(result.isCheckmate());
        }

        response.setGameState(GameStateResponse.fromEngine(engine));
        return response;
    }

    public static MoveResponse error(String message, TetherChessEngine engine) {
        MoveResponse response = new MoveResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setGameState(GameStateResponse.fromEngine(engine));
        return response;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getMoveNotation() { return moveNotation; }
    public void setMoveNotation(String moveNotation) { this.moveNotation = moveNotation; }

    public boolean isTransporterMove() { return isTransporterMove; }
    public void setTransporterMove(boolean transporterMove) { isTransporterMove = transporterMove; }

    public boolean isPawnKnightApex() { return isPawnKnightApex; }
    public void setPawnKnightApex(boolean pawnKnightApex) { isPawnKnightApex = pawnKnightApex; }

    public boolean isGivesCheck() { return givesCheck; }
    public void setGivesCheck(boolean givesCheck) { this.givesCheck = givesCheck; }

    public boolean isCheckmate() { return isCheckmate; }
    public void setCheckmate(boolean checkmate) { isCheckmate = checkmate; }

    public GameStateResponse getGameState() { return gameState; }
    public void setGameState(GameStateResponse gameState) { this.gameState = gameState; }
}
