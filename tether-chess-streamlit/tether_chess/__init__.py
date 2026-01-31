"""
Tether Chess (Tal's Forest) - A Chess Variant with Geometric Entanglement

FOUR UNIQUE RULES:
1. RANK ENTANGLEMENT: Pieces share movement with friendly pieces on same rank
2. PAWN-KNIGHT APEX: Pawn using Knight's jump to 8th rank promotes immediately
3. NATIVE LETHALITY: Only native movement can deliver check/checkmate
4. NO RECURSIVE JUMPING: One teleport per turn maximum

THE DISCONNECTION:
When a piece moves ranks, it immediately loses old allies and gains new ones.
"""

from .models import Position, PieceType, Piece, Move, GameMode
from .board import Board
from .engine import TetherChessEngine

__all__ = ['Position', 'PieceType', 'Piece', 'Move', 'GameMode', 'Board', 'TetherChessEngine']
__version__ = '1.0.0'
