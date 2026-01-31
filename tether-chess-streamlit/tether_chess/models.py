"""
Core models for Tether Chess (Tal's Forest).
"""

from enum import Enum
from dataclasses import dataclass, field
from typing import List, Tuple, Optional


class PieceType(Enum):
    """Chess piece types with their properties."""
    KING = ("K", False, "♔", "♚")
    QUEEN = ("Q", True, "♕", "♛")
    ROOK = ("R", True, "♖", "♜")
    BISHOP = ("B", True, "♗", "♝")
    KNIGHT = ("N", False, "♘", "♞")
    PAWN = ("P", False, "♙", "♟")

    def __init__(self, symbol: str, is_sliding: bool, white_unicode: str, black_unicode: str):
        self.symbol = symbol
        self.is_sliding = is_sliding
        self.white_unicode = white_unicode
        self.black_unicode = black_unicode

    def get_unicode(self, is_white: bool) -> str:
        return self.white_unicode if is_white else self.black_unicode


class EntanglementAxis(Enum):
    """The axis along which pieces share movement potential."""
    RANK = "Rank"  # Horizontal (same row)
    FILE = "File"  # Vertical (same column)


class GameMode(Enum):
    """
    Taxi Chess game modes combining movement style and entanglement axis:

    Movement Styles:
    - LINEAR: Pieces TELEPORT to where tether-mates can go (destinations)
    - QUANTUM: Pieces INHERIT tether-mates' movement abilities (patterns)

    Entanglement Axes:
    - RANK: Horizontal tethering (pieces on same row share abilities)
    - FILE: Vertical tethering (pieces on same column share abilities)
    """
    LINEAR_RANK = "Linear Rank"
    QUANTUM_RANK = "Quantum Rank"
    LINEAR_FILE = "Linear File"
    QUANTUM_FILE = "Quantum File"

    @property
    def is_quantum(self) -> bool:
        return self in (GameMode.QUANTUM_RANK, GameMode.QUANTUM_FILE)

    @property
    def is_file_based(self) -> bool:
        return self in (GameMode.LINEAR_FILE, GameMode.QUANTUM_FILE)

    @property
    def axis(self) -> 'EntanglementAxis':
        return EntanglementAxis.FILE if self.is_file_based else EntanglementAxis.RANK


@dataclass(frozen=True)
class Position:
    """
    Board position using 0-indexed coordinates.
    x (file): 0-7 maps to a-h
    y (rank): 0-7 maps to 1-8
    """
    x: int
    y: int

    @property
    def rank(self) -> int:
        """Get the rank (y-coordinate) for geometric entanglement."""
        return self.y

    @property
    def file(self) -> int:
        """Get the file (x-coordinate)."""
        return self.x

    def is_valid(self) -> bool:
        return 0 <= self.x < 8 and 0 <= self.y < 8

    def offset(self, dx: int, dy: int) -> 'Position':
        return Position(self.x + dx, self.y + dy)

    def is_promotion_rank(self, is_white: bool) -> bool:
        """Check if this position is the promotion rank for a given color."""
        return self.y == 7 if is_white else self.y == 0

    def to_algebraic(self) -> str:
        """Convert to algebraic notation (e.g., 'e4')."""
        return chr(ord('a') + self.x) + str(self.y + 1)

    @classmethod
    def from_algebraic(cls, notation: str) -> 'Position':
        """Parse algebraic notation (e.g., 'e4') to Position."""
        if len(notation) != 2:
            raise ValueError(f"Invalid notation: {notation}")
        x = ord(notation[0].lower()) - ord('a')
        y = int(notation[1]) - 1
        pos = cls(x, y)
        if not pos.is_valid():
            raise ValueError(f"Position out of bounds: {notation}")
        return pos

    def __str__(self) -> str:
        return self.to_algebraic()


@dataclass
class Piece:
    """
    A chess piece with native movement rules.

    Native movement determines:
    - What moves can be shared with rank-mates (Geometric Entanglement)
    - What moves can deliver check (Native Lethality)
    """
    piece_type: PieceType
    is_white: bool
    position: Position
    has_moved: bool = False

    def get_native_movement_vectors(self) -> List[Tuple[int, int, bool]]:
        """
        Get native movement vectors as (dx, dy, is_sliding).
        These represent the 'soul' of the piece for Native Lethality.
        """
        vectors = []

        if self.piece_type == PieceType.KING:
            for dx in [-1, 0, 1]:
                for dy in [-1, 0, 1]:
                    if dx != 0 or dy != 0:
                        vectors.append((dx, dy, False))

        elif self.piece_type == PieceType.QUEEN:
            for dx in [-1, 0, 1]:
                for dy in [-1, 0, 1]:
                    if dx != 0 or dy != 0:
                        vectors.append((dx, dy, True))

        elif self.piece_type == PieceType.ROOK:
            vectors = [(1, 0, True), (-1, 0, True), (0, 1, True), (0, -1, True)]

        elif self.piece_type == PieceType.BISHOP:
            vectors = [(1, 1, True), (1, -1, True), (-1, 1, True), (-1, -1, True)]

        elif self.piece_type == PieceType.KNIGHT:
            vectors = [
                (2, 1, False), (2, -1, False), (-2, 1, False), (-2, -1, False),
                (1, 2, False), (1, -2, False), (-1, 2, False), (-1, -2, False)
            ]

        elif self.piece_type == PieceType.PAWN:
            direction = 1 if self.is_white else -1
            vectors = [(0, direction, False)]

        return vectors

    def get_native_attack_squares(self) -> List[Position]:
        """Get squares this piece can natively attack from current position."""
        squares = []

        if self.piece_type == PieceType.PAWN:
            direction = 1 if self.is_white else -1
            for dx in [-1, 1]:
                target = self.position.offset(dx, direction)
                if target.is_valid():
                    squares.append(target)
        else:
            for dx, dy, is_sliding in self.get_native_movement_vectors():
                if is_sliding:
                    for i in range(1, 8):
                        target = self.position.offset(dx * i, dy * i)
                        if target.is_valid():
                            squares.append(target)
                        else:
                            break
                else:
                    target = self.position.offset(dx, dy)
                    if target.is_valid():
                        squares.append(target)

        return squares

    def can_natively_attack(self, origin: Position, target: Position) -> bool:
        """Check if this piece can natively attack target from origin."""
        if self.piece_type == PieceType.PAWN:
            direction = 1 if self.is_white else -1
            dx = target.x - origin.x
            dy = target.y - origin.y
            return abs(dx) == 1 and dy == direction

        for vx, vy, is_sliding in self.get_native_movement_vectors():
            dx = target.x - origin.x
            dy = target.y - origin.y

            if is_sliding:
                if not self._is_along_ray(dx, dy, vx, vy):
                    continue
                return True
            else:
                if dx == vx and dy == vy:
                    return True

        return False

    def _is_along_ray(self, dx: int, dy: int, vx: int, vy: int) -> bool:
        """Check if (dx, dy) is along the ray defined by (vx, vy)."""
        if dx == 0 and dy == 0:
            return False
        if vx == 0 and dx != 0:
            return False
        if vy == 0 and dy != 0:
            return False
        if vx != 0 and vy != 0:
            if abs(dx) != abs(dy):
                return False
        if vx != 0 and (dx > 0) != (vx > 0):
            return False
        if vy != 0 and (dy > 0) != (vy > 0):
            return False
        return True

    def get_symbol(self) -> str:
        """Get the piece symbol."""
        return self.piece_type.symbol if self.is_white else self.piece_type.symbol.lower()

    def get_unicode(self) -> str:
        """Get the Unicode chess symbol."""
        return self.piece_type.get_unicode(self.is_white)

    def copy(self) -> 'Piece':
        """Create a copy of this piece."""
        p = Piece(self.piece_type, self.is_white, self.position, self.has_moved)
        return p

    def __str__(self) -> str:
        return self.get_unicode()


@dataclass
class Move:
    """
    Represents a move in Tether Chess.
    Tracks transporter moves (borrowed movement via Geometric Entanglement).
    """
    from_pos: Position
    to_pos: Position
    piece: Piece
    captured_piece: Optional[Piece] = None
    is_transporter: bool = False
    borrowed_from: Optional[Piece] = None
    promotion_type: Optional[PieceType] = None
    is_castling: bool = False
    is_en_passant: bool = False

    @property
    def is_capture(self) -> bool:
        return self.captured_piece is not None

    @property
    def is_promotion(self) -> bool:
        return self.promotion_type is not None

    @property
    def is_pawn_knight_apex(self) -> bool:
        """Check if this is a Pawn-Knight Apex move (instant promotion via Knight jump)."""
        return (
            self.piece.piece_type == PieceType.PAWN
            and self.is_transporter
            and self.borrowed_from is not None
            and self.borrowed_from.piece_type == PieceType.KNIGHT
            and self.is_promotion
        )

    def to_notation(self) -> str:
        """Convert to algebraic notation."""
        if self.is_castling:
            return "O-O" if self.to_pos.x > self.from_pos.x else "O-O-O"

        parts = []

        # Piece symbol (empty for pawn)
        if self.piece.piece_type != PieceType.PAWN:
            parts.append(self.piece.piece_type.symbol)

        # Transporter notation
        if self.is_transporter and self.borrowed_from:
            parts.append(f"~{self.borrowed_from.piece_type.symbol}")

        parts.append(str(self.from_pos))

        # Capture indicator
        parts.append("x" if self.is_capture else "-")

        parts.append(str(self.to_pos))

        # Promotion
        if self.is_promotion:
            parts.append(f"={self.promotion_type.symbol}")
            if self.is_pawn_knight_apex:
                parts.append("!")  # Mark Pawn-Knight Apex

        return "".join(parts)

    def __str__(self) -> str:
        return self.to_notation()
