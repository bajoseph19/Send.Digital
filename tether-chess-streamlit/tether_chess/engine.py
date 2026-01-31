"""
TETHER CHESS ENGINE (Tal's Forest)

The main orchestrator for the Tether Chess variant.
Implements the complete game logic including the Four Unique Rules:

1. RANK ENTANGLEMENT (Geometric Entanglement)
   Every piece on a horizontal rank shares its movement potential
   with all friendly pieces on that same rank.

2. INSTANT PAWN PROMOTION (Pawn-Knight Apex)
   A Pawn sharing a rank with a Knight can utilize the Knight's L-jump.
   If that jump lands the Pawn on the 8th rank, it promotes immediately.

3. NATIVE LETHALITY
   A piece can only deliver Check or Checkmate using its own native
   movement rules. A teleporting Rook landing next to a King does not
   check the King unless the King is on the Rook's file or rank.

4. NO RECURSIVE JUMPING
   Cannot chain borrowed moves. Each turn allows one transporter move.
   The borrowed movement cannot itself be used to borrow again.

THE DISCONNECTION:
When a piece moves ranks, it immediately loses old allies and gains new ones.
"""

from enum import Enum
from typing import List, Optional, Dict, Any
from dataclasses import dataclass

from .models import Position, PieceType, Piece, Move
from .board import Board


class GameState(Enum):
    """Game state enumeration."""
    ONGOING = "ongoing"
    WHITE_WINS_CHECKMATE = "white_wins_checkmate"
    BLACK_WINS_CHECKMATE = "black_wins_checkmate"
    STALEMATE = "stalemate"
    DRAW_BY_REPETITION = "draw_by_repetition"
    DRAW_BY_FIFTY_MOVES = "draw_by_fifty_moves"


@dataclass
class MoveResult:
    """Result of a move attempt."""
    success: bool
    message: str
    move: Optional[Move] = None
    gives_check: bool = False
    is_checkmate: bool = False


class TetherChessEngine:
    """Main orchestrator for Tether Chess."""

    def __init__(self):
        self.board = Board()
        self.game_state = GameState.ONGOING
        self.game_log: List[str] = []

    def new_game(self):
        """Initialize a new game."""
        self.board.setup_starting_position()
        self.game_state = GameState.ONGOING
        self.game_log.clear()
        self.game_log.append("=== TETHER CHESS (Tal's Forest) ===")
        self.game_log.append("Game started. White to move.")
        self.game_log.append("")
        self.game_log.append("FOUR UNIQUE RULES:")
        self.game_log.append("1. Rank Entanglement - Pieces share movement with rank-mates")
        self.game_log.append("2. Pawn-Knight Apex - Pawn + Knight jump to 8th = instant promotion")
        self.game_log.append("3. Native Lethality - Only native moves can deliver check")
        self.game_log.append("4. No Recursive Jumping - One teleport per turn maximum")
        self.game_log.append("")

    def get_legal_moves(self) -> List[Move]:
        """Get all legal moves for the current position."""
        if self.game_state != GameState.ONGOING:
            return []
        return self.board.generate_all_legal_moves()

    def get_legal_moves_for_piece(self, position: Position) -> List[Move]:
        """Get legal moves for a specific piece."""
        if self.game_state != GameState.ONGOING:
            return []

        piece = self.board.get_piece_at(position)
        if not piece or piece.is_white != self.board.white_to_move:
            return []

        return self.board.generate_legal_moves_for_piece(piece)

    def get_transporter_moves(self) -> List[Move]:
        """Get all transporter (borrowed movement) moves."""
        return [m for m in self.get_legal_moves() if m.is_transporter]

    def get_pawn_knight_apex_moves(self) -> List[Move]:
        """Get Pawn-Knight Apex moves (instant promotion via Knight jump)."""
        return [m for m in self.get_legal_moves() if m.is_pawn_knight_apex]

    def make_move(self, from_pos: Position, to_pos: Position,
                  promotion_type: Optional[PieceType] = None) -> MoveResult:
        """Execute a move given by positions."""
        if self.game_state != GameState.ONGOING:
            return MoveResult(False, f"Game is over: {self.game_state.value}")

        piece = self.board.get_piece_at(from_pos)
        if not piece:
            return MoveResult(False, f"No piece at {from_pos}")

        if piece.is_white != self.board.white_to_move:
            return MoveResult(False, "Not your turn")

        # Find matching legal move
        legal_moves = self.board.generate_legal_moves_for_piece(piece)
        matching_move = None

        for move in legal_moves:
            if move.to_pos == to_pos:
                if move.is_promotion:
                    promo = promotion_type or PieceType.QUEEN
                    if move.promotion_type == promo:
                        matching_move = move
                        break
                else:
                    matching_move = move
                    break

        if not matching_move:
            return MoveResult(False, f"Illegal move: {from_pos} to {to_pos}")

        return self._execute_move(matching_move)

    def make_move_from_notation(self, notation: str) -> MoveResult:
        """Execute a move from algebraic notation."""
        if self.game_state != GameState.ONGOING:
            return MoveResult(False, f"Game is over: {self.game_state.value}")

        move = self._parse_move(notation)
        if not move:
            return MoveResult(False, f"Invalid move notation: {notation}")

        return self._execute_move(move)

    def _execute_move(self, move: Move) -> MoveResult:
        """Execute a move and update game state."""
        self.board.make_move(move)

        gives_check = self.board.is_in_check()
        is_checkmate = self.board.is_checkmate()
        is_stalemate = self.board.is_stalemate()

        annotation = ""
        if is_checkmate:
            annotation = "#"
            self.game_state = (GameState.BLACK_WINS_CHECKMATE
                              if self.board.white_to_move
                              else GameState.WHITE_WINS_CHECKMATE)
        elif is_stalemate:
            self.game_state = GameState.STALEMATE
        elif gives_check:
            annotation = "+"

        # Build message
        msg_parts = [move.to_notation() + annotation]

        if move.is_transporter and move.borrowed_from:
            msg_parts.append(f"(Transporter: borrowed {move.borrowed_from.piece_type.name} movement)")

        if move.is_pawn_knight_apex:
            msg_parts.append("[PAWN-KNIGHT APEX! Instant promotion!]")

        if is_checkmate:
            msg_parts.append("CHECKMATE!")
        elif is_stalemate:
            msg_parts.append("STALEMATE!")
        elif gives_check:
            msg_parts.append("Check!")

        message = " ".join(msg_parts)
        self.game_log.append(message)

        return MoveResult(True, message, move, gives_check, is_checkmate)

    def _parse_move(self, notation: str) -> Optional[Move]:
        """Parse move notation to find matching move."""
        notation = notation.strip()

        # Handle castling
        if notation in ["O-O", "0-0"]:
            king_pos = self.board.find_king(self.board.white_to_move)
            if king_pos:
                return self._find_move(king_pos, king_pos.offset(2, 0))

        if notation in ["O-O-O", "0-0-0"]:
            king_pos = self.board.find_king(self.board.white_to_move)
            if king_pos:
                return self._find_move(king_pos, king_pos.offset(-2, 0))

        # Parse "e2-e4" or "e2e4" style
        notation = notation.replace("-", "").replace("x", "")
        if len(notation) >= 4:
            try:
                from_str = notation[:2]
                to_str = notation[2:4]
                from_pos = Position.from_algebraic(from_str)
                to_pos = Position.from_algebraic(to_str)
                return self._find_move(from_pos, to_pos)
            except ValueError:
                pass

        return None

    def _find_move(self, from_pos: Position, to_pos: Position) -> Optional[Move]:
        """Find a move in legal moves list."""
        piece = self.board.get_piece_at(from_pos)
        if not piece:
            return None

        for move in self.board.generate_legal_moves_for_piece(piece):
            if move.to_pos == to_pos:
                return move
        return None

    # ========================================================================
    # Analysis Methods
    # ========================================================================

    def get_michael_tal_opening_moves(self) -> List[Move]:
        """
        Get the "Michael Tal" opening moves.
        Back-rank Knights teleporting Queen or Rooks on Move 1.
        """
        if self.board.move_history:
            return []

        tal_moves = []
        back_rank = 0 if self.board.white_to_move else 7

        for move in self.get_legal_moves():
            if not move.is_transporter or not move.borrowed_from:
                continue

            # Check if borrowing from Knight on back rank
            if (move.borrowed_from.piece_type == PieceType.KNIGHT and
                move.borrowed_from.position.rank == back_rank):

                # Check if moving Queen or Rook from back rank
                if (move.piece.piece_type in [PieceType.QUEEN, PieceType.ROOK] and
                    move.from_pos.rank == back_rank):
                    tal_moves.append(move)

        return tal_moves

    def get_checking_moves(self) -> List[Move]:
        """Get moves that deliver check via Native Lethality."""
        checking_moves = []
        enemy_king_pos = self.board.find_king(not self.board.white_to_move)

        if not enemy_king_pos:
            return []

        for move in self.get_legal_moves():
            if self.board.validate_lethality(move.piece, move.to_pos, enemy_king_pos):
                checking_moves.append(move)

        return checking_moves

    def get_rank_mates_positions(self, piece_position: Position) -> List[Position]:
        """Get positions of rank-mates for a piece (for UI highlighting)."""
        piece = self.board.get_piece_at(piece_position)
        if not piece:
            return []

        # identify_rank_mates now returns List[Tuple[Piece, Position]]
        return [pos for _, pos in self.board.identify_rank_mates(piece, piece_position)]

    # ========================================================================
    # Getters
    # ========================================================================

    def is_white_to_move(self) -> bool:
        return self.board.white_to_move

    def get_board_display(self) -> str:
        return self.board.to_string()

    def get_game_log(self) -> List[str]:
        return self.game_log.copy()

    def get_game_info(self) -> Dict[str, Any]:
        """Get complete game information."""
        return {
            "game_state": self.game_state.value,
            "white_to_move": self.board.white_to_move,
            "in_check": self.board.is_in_check(),
            "legal_moves_count": len(self.get_legal_moves()),
            "transporter_moves_count": len(self.get_transporter_moves()),
            "pawn_knight_apex_available": len(self.get_pawn_knight_apex_moves()) > 0,
            "michael_tal_available": len(self.get_michael_tal_opening_moves()) > 0,
            "move_count": len(self.board.move_history),
        }
