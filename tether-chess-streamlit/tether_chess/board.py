"""
Tether Chess Board - implements the Geometric Entanglement system.

FOUR UNIQUE RULES OF TETHER CHESS (Tal's Forest):
1. RANK ENTANGLEMENT: Pieces on the same horizontal rank share movement potential
2. INSTANT PAWN PROMOTION: Pawn using Knight's L-jump to 8th rank promotes immediately
3. NATIVE LETHALITY: Only native movement can deliver check/checkmate
4. NO RECURSIVE JUMPING: Cannot chain borrowed moves (one teleport per turn)

THE DISCONNECTION:
When a piece moves from Rank A to Rank B, it IMMEDIATELY loses all borrowed
capabilities from Rank A allies. On the next turn, it gains new capabilities
from any allies on Rank B. The entanglement is position-based, not persistent.
"""

from typing import List, Optional, Dict, Tuple
from .models import Position, PieceType, Piece, Move


class Board:
    """Tether Chess board with Geometric Entanglement."""

    def __init__(self):
        self.squares: List[List[Optional[Piece]]] = [[None for _ in range(8)] for _ in range(8)]
        self.white_to_move: bool = True
        self.en_passant_target: Optional[Position] = None
        self.white_can_castle_kingside: bool = True
        self.white_can_castle_queenside: bool = True
        self.black_can_castle_kingside: bool = True
        self.black_can_castle_queenside: bool = True
        self.move_history: List[Move] = []

    def setup_starting_position(self):
        """Initialize board to standard starting position."""
        # Clear board
        self.squares = [[None for _ in range(8)] for _ in range(8)]

        # White pieces
        back_rank = [PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
                     PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK]

        for x, piece_type in enumerate(back_rank):
            self.squares[x][0] = Piece(piece_type, True, Position(x, 0))
            self.squares[x][7] = Piece(piece_type, False, Position(x, 7))

        for x in range(8):
            self.squares[x][1] = Piece(PieceType.PAWN, True, Position(x, 1))
            self.squares[x][6] = Piece(PieceType.PAWN, False, Position(x, 6))

        # Reset game state
        self.white_to_move = True
        self.en_passant_target = None
        self.white_can_castle_kingside = True
        self.white_can_castle_queenside = True
        self.black_can_castle_kingside = True
        self.black_can_castle_queenside = True
        self.move_history.clear()

    def get_piece_at(self, pos: Position) -> Optional[Piece]:
        if not pos.is_valid():
            return None
        return self.squares[pos.x][pos.y]

    def set_piece_at(self, pos: Position, piece: Optional[Piece]):
        self.squares[pos.x][pos.y] = piece
        if piece:
            piece.position = pos

    # ========================================================================
    # TETHER CHESS CORE ALGORITHM: The Three Checks
    # ========================================================================

    def identify_rank_mates(self, piece: Piece) -> List[Piece]:
        """
        STEP 1: identifyRankMates(piece)
        Scans the current y-axis (rank) for friendly units.

        THE DISCONNECTION: This uses the piece's CURRENT position.
        When a piece moves from Rank 3 to Rank 4:
        - It NO LONGER has access to Rank 3 allies' movement
        - It NOW has access to any Rank 4 allies' movement
        """
        rank_mates = []
        rank = piece.position.rank

        for x in range(8):
            other = self.squares[x][rank]
            if other and other is not piece and other.is_white == piece.is_white:
                rank_mates.append(other)

        return rank_mates

    def calculate_transporter_vector(self, piece: Piece, mates: List[Piece]) -> List[Move]:
        """
        STEP 2: calculateTransporterVector(piece, mates)
        The moving piece can TELEPORT to any square that its rank-mates
        can actually reach from THEIR current positions.

        KEY RULES:
        - Teleportation: Piece goes to where rank-mates CAN go (not borrowing pattern)
        - Path Integrity: Rank-mate's path must be clear from THEIR position
        - No Recursive Jumping: Each transporter move is marked (cannot chain)
        """
        transporter_moves = []
        seen_targets = set()  # Avoid duplicate moves to same square

        for mate in mates:
            mate_origin = mate.position

            for dx, dy, is_sliding in mate.get_native_movement_vectors():
                if is_sliding:
                    # Sliding piece: check path from MATE's position
                    for distance in range(1, 8):
                        target = mate_origin.offset(dx * distance, dy * distance)
                        if not target.is_valid():
                            break

                        target_piece = self.get_piece_at(target)

                        # Can't teleport to where the moving piece already is
                        if target == piece.position:
                            if target_piece:
                                break  # Path blocked by moving piece
                            continue

                        if target_piece:
                            # Can capture enemy piece (teleport and capture)
                            if target_piece.is_white != piece.is_white:
                                if target not in seen_targets:
                                    move = self._create_transporter_move(
                                        piece, target, mate, target_piece
                                    )
                                    if move:
                                        transporter_moves.append(move)
                                        seen_targets.add(target)
                            break  # Path blocked

                        # Empty square - can teleport there
                        if target not in seen_targets:
                            move = self._create_transporter_move(piece, target, mate, None)
                            if move:
                                transporter_moves.append(move)
                                seen_targets.add(target)
                else:
                    # Non-sliding (Knight, King): single jump from MATE's position
                    target = mate_origin.offset(dx, dy)
                    if not target.is_valid():
                        continue

                    # Can't teleport to own position
                    if target == piece.position:
                        continue

                    target_piece = self.get_piece_at(target)

                    # Can't teleport onto friendly pieces (except self, handled above)
                    if target_piece and target_piece.is_white == piece.is_white:
                        continue

                    if target not in seen_targets:
                        move = self._create_transporter_move(piece, target, mate, target_piece)
                        if move:
                            transporter_moves.append(move)
                            seen_targets.add(target)

        return transporter_moves

    def _create_transporter_move(
        self, piece: Piece, target: Position, mate: Piece, captured: Optional[Piece]
    ) -> Optional[Move]:
        """Create a transporter move, handling Pawn-Knight Apex promotion."""
        promotion_type = None

        # PAWN-KNIGHT APEX: Instant promotion when Knight-jumping to 8th rank!
        if piece.piece_type == PieceType.PAWN and target.is_promotion_rank(piece.is_white):
            promotion_type = PieceType.QUEEN  # Default to Queen

        return Move(
            from_pos=piece.position,
            to_pos=target,
            piece=piece,
            captured_piece=captured,
            is_transporter=True,
            borrowed_from=mate,
            promotion_type=promotion_type
        )

    def validate_lethality(self, piece: Piece, piece_position: Position,
                           enemy_king_position: Position) -> bool:
        """
        STEP 3: validateLethality(piece, position, enemyKingPosition)

        NATIVE LETHALITY RULE:
        A piece can only deliver check using its own native movement.
        A teleporting Rook next to a King does NOT check unless
        the King is on the Rook's file or rank.
        """
        return piece.can_natively_attack(piece_position, enemy_king_position)

    def is_under_native_attack(self, target: Position, by_white: bool) -> bool:
        """Check if a position is under attack by native movement only."""
        for x in range(8):
            for y in range(8):
                attacker = self.squares[x][y]
                if attacker and attacker.is_white == by_white:
                    if self._can_natively_reach(attacker, target):
                        return True
        return False

    def _can_natively_reach(self, piece: Piece, target: Position) -> bool:
        """Check if a piece can natively reach a target (with path checking)."""
        origin = piece.position

        if piece.piece_type == PieceType.PAWN:
            direction = 1 if piece.is_white else -1
            dx = target.x - origin.x
            dy = target.y - origin.y
            return abs(dx) == 1 and dy == direction

        for vx, vy, is_sliding in piece.get_native_movement_vectors():
            if is_sliding:
                dx = target.x - origin.x
                dy = target.y - origin.y

                if not piece._is_along_ray(dx, dy, vx, vy):
                    continue

                # Check path is clear
                steps = max(abs(dx), abs(dy))
                path_clear = True
                for i in range(1, steps):
                    intermediate = origin.offset(vx * i, vy * i)
                    if self.get_piece_at(intermediate):
                        path_clear = False
                        break

                if path_clear:
                    return True
            else:
                if origin.offset(vx, vy) == target:
                    return True

        return False

    # ========================================================================
    # Move Generation
    # ========================================================================

    def generate_all_legal_moves(self) -> List[Move]:
        """Generate all legal moves for the current side."""
        all_moves = []

        for x in range(8):
            for y in range(8):
                piece = self.squares[x][y]
                if piece and piece.is_white == self.white_to_move:
                    all_moves.extend(self.generate_legal_moves_for_piece(piece))

        return all_moves

    def generate_legal_moves_for_piece(self, piece: Piece, skip_check_filter: bool = False) -> List[Move]:
        """Generate all legal moves for a specific piece."""
        moves = []

        # Native moves
        moves.extend(self._generate_native_moves(piece))

        # Transporter moves (Geometric Entanglement)
        rank_mates = self.identify_rank_mates(piece)
        moves.extend(self.calculate_transporter_vector(piece, rank_mates))

        # Filter moves that leave king in check (standard chess legality)
        if not skip_check_filter:
            legal_moves = []
            for move in moves:
                if not self._leaves_king_in_check(move):
                    legal_moves.append(move)
            moves = legal_moves

        return moves

    def _generate_native_moves(self, piece: Piece) -> List[Move]:
        """Generate native (non-transporter) moves."""
        moves = []
        origin = piece.position

        if piece.piece_type == PieceType.PAWN:
            moves.extend(self._generate_pawn_moves(piece))
        elif piece.piece_type == PieceType.KING:
            moves.extend(self._generate_king_moves(piece))
        else:
            for dx, dy, is_sliding in piece.get_native_movement_vectors():
                if is_sliding:
                    for dist in range(1, 8):
                        target = origin.offset(dx * dist, dy * dist)
                        if not target.is_valid():
                            break

                        target_piece = self.get_piece_at(target)
                        if target_piece:
                            if target_piece.is_white != piece.is_white:
                                moves.append(Move(origin, target, piece, target_piece))
                            break
                        moves.append(Move(origin, target, piece))
                else:
                    target = origin.offset(dx, dy)
                    if not target.is_valid():
                        continue
                    target_piece = self.get_piece_at(target)
                    if target_piece and target_piece.is_white == piece.is_white:
                        continue
                    moves.append(Move(origin, target, piece, target_piece))

        return moves

    def _generate_pawn_moves(self, pawn: Piece) -> List[Move]:
        """Generate pawn moves."""
        moves = []
        origin = pawn.position
        direction = 1 if pawn.is_white else -1
        start_rank = 1 if pawn.is_white else 6

        # Forward move
        one_forward = origin.offset(0, direction)
        if one_forward.is_valid() and not self.get_piece_at(one_forward):
            if one_forward.is_promotion_rank(pawn.is_white):
                for promo in [PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT]:
                    moves.append(Move(origin, one_forward, pawn, promotion_type=promo))
            else:
                moves.append(Move(origin, one_forward, pawn))

                # Double move
                if origin.rank == start_rank:
                    two_forward = origin.offset(0, direction * 2)
                    if not self.get_piece_at(two_forward):
                        moves.append(Move(origin, two_forward, pawn))

        # Captures
        for dx in [-1, 1]:
            capture_sq = origin.offset(dx, direction)
            if not capture_sq.is_valid():
                continue

            target = self.get_piece_at(capture_sq)
            if target and target.is_white != pawn.is_white:
                if capture_sq.is_promotion_rank(pawn.is_white):
                    for promo in [PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT]:
                        moves.append(Move(origin, capture_sq, pawn, target, promotion_type=promo))
                else:
                    moves.append(Move(origin, capture_sq, pawn, target))

            # En passant
            if self.en_passant_target and capture_sq == self.en_passant_target:
                ep_pawn = self.get_piece_at(origin.offset(dx, 0))
                if ep_pawn and ep_pawn.piece_type == PieceType.PAWN:
                    moves.append(Move(origin, capture_sq, pawn, ep_pawn, is_en_passant=True))

        return moves

    def _generate_king_moves(self, king: Piece) -> List[Move]:
        """Generate king moves including castling."""
        moves = []
        origin = king.position

        # Normal moves
        for dx, dy, _ in king.get_native_movement_vectors():
            target = origin.offset(dx, dy)
            if not target.is_valid():
                continue
            target_piece = self.get_piece_at(target)
            if target_piece and target_piece.is_white == king.is_white:
                continue
            moves.append(Move(origin, target, king, target_piece))

        # Castling
        if not king.has_moved:
            can_ks = self.white_can_castle_kingside if king.is_white else self.black_can_castle_kingside
            can_qs = self.white_can_castle_queenside if king.is_white else self.black_can_castle_queenside

            if can_ks:
                rook_pos = Position(7, origin.rank)
                rook = self.get_piece_at(rook_pos)
                if rook and rook.piece_type == PieceType.ROOK and not rook.has_moved:
                    if (not self.get_piece_at(origin.offset(1, 0)) and
                        not self.get_piece_at(origin.offset(2, 0)) and
                        not self.is_under_native_attack(origin, not king.is_white) and
                        not self.is_under_native_attack(origin.offset(1, 0), not king.is_white) and
                        not self.is_under_native_attack(origin.offset(2, 0), not king.is_white)):
                        moves.append(Move(origin, origin.offset(2, 0), king, is_castling=True))

            if can_qs:
                rook_pos = Position(0, origin.rank)
                rook = self.get_piece_at(rook_pos)
                if rook and rook.piece_type == PieceType.ROOK and not rook.has_moved:
                    if (not self.get_piece_at(origin.offset(-1, 0)) and
                        not self.get_piece_at(origin.offset(-2, 0)) and
                        not self.get_piece_at(origin.offset(-3, 0)) and
                        not self.is_under_native_attack(origin, not king.is_white) and
                        not self.is_under_native_attack(origin.offset(-1, 0), not king.is_white) and
                        not self.is_under_native_attack(origin.offset(-2, 0), not king.is_white)):
                        moves.append(Move(origin, origin.offset(-2, 0), king, is_castling=True))

        return moves

    def _leaves_king_in_check(self, move: Move) -> bool:
        """Check if a move leaves the moving side's king in check."""
        # Make move temporarily
        piece = move.piece
        from_pos = move.from_pos
        to_pos = move.to_pos
        captured = self.get_piece_at(to_pos)

        self.squares[from_pos.x][from_pos.y] = None
        self.squares[to_pos.x][to_pos.y] = piece
        original_pos = piece.position
        piece.position = to_pos

        # Handle en passant
        ep_captured = None
        if move.is_en_passant:
            ep_pos = Position(to_pos.x, from_pos.y)
            ep_captured = self.get_piece_at(ep_pos)
            self.squares[ep_pos.x][ep_pos.y] = None

        # Find king and check
        king_pos = self.find_king(piece.is_white)
        in_check = self.is_under_native_attack(king_pos, not piece.is_white) if king_pos else False

        # Unmake move
        self.squares[from_pos.x][from_pos.y] = piece
        self.squares[to_pos.x][to_pos.y] = captured
        piece.position = original_pos

        if move.is_en_passant and ep_captured:
            ep_pos = Position(to_pos.x, from_pos.y)
            self.squares[ep_pos.x][ep_pos.y] = ep_captured

        return in_check

    def find_king(self, is_white: bool) -> Optional[Position]:
        """Find the king of a given color. Returns its actual board position."""
        for x in range(8):
            for y in range(8):
                piece = self.squares[x][y]
                if piece and piece.piece_type == PieceType.KING and piece.is_white == is_white:
                    # Return the actual position on the board, not piece.position
                    # This ensures correctness during temporary move simulation
                    return Position(x, y)
        return None

    # ========================================================================
    # Move Execution
    # ========================================================================

    def make_move(self, move: Move):
        """Execute a move on the board."""
        from_pos = move.from_pos
        to_pos = move.to_pos
        piece = move.piece

        # Update en passant target
        self.en_passant_target = None
        if piece.piece_type == PieceType.PAWN:
            if abs(to_pos.y - from_pos.y) == 2:
                self.en_passant_target = Position(from_pos.x, (from_pos.y + to_pos.y) // 2)

        # Handle en passant capture
        if move.is_en_passant:
            ep_pos = Position(to_pos.x, from_pos.y)
            self.squares[ep_pos.x][ep_pos.y] = None

        # Handle castling
        if move.is_castling:
            if to_pos.x > from_pos.x:  # Kingside
                rook = self.squares[7][from_pos.y]
                self.squares[7][from_pos.y] = None
                self.squares[5][from_pos.y] = rook
                rook.position = Position(5, from_pos.y)
                rook.has_moved = True
            else:  # Queenside
                rook = self.squares[0][from_pos.y]
                self.squares[0][from_pos.y] = None
                self.squares[3][from_pos.y] = rook
                rook.position = Position(3, from_pos.y)
                rook.has_moved = True

        # Move piece
        self.squares[from_pos.x][from_pos.y] = None

        # Handle promotion
        if move.is_promotion:
            promoted = Piece(move.promotion_type, piece.is_white, to_pos, True)
            self.squares[to_pos.x][to_pos.y] = promoted
        else:
            self.squares[to_pos.x][to_pos.y] = piece
            piece.position = to_pos
            piece.has_moved = True

        # Update castling rights
        if piece.piece_type == PieceType.KING:
            if piece.is_white:
                self.white_can_castle_kingside = False
                self.white_can_castle_queenside = False
            else:
                self.black_can_castle_kingside = False
                self.black_can_castle_queenside = False

        if piece.piece_type == PieceType.ROOK:
            if from_pos == Position(0, 0):
                self.white_can_castle_queenside = False
            elif from_pos == Position(7, 0):
                self.white_can_castle_kingside = False
            elif from_pos == Position(0, 7):
                self.black_can_castle_queenside = False
            elif from_pos == Position(7, 7):
                self.black_can_castle_kingside = False

        self.move_history.append(move)
        self.white_to_move = not self.white_to_move

    def is_in_check(self) -> bool:
        """Check if the current side is in check."""
        king_pos = self.find_king(self.white_to_move)
        return self.is_under_native_attack(king_pos, not self.white_to_move) if king_pos else False

    def is_checkmate(self) -> bool:
        """Check if the current side is in checkmate."""
        if not self.is_in_check():
            return False
        return len(self.generate_all_legal_moves()) == 0

    def is_stalemate(self) -> bool:
        """Check if the game is a stalemate."""
        if self.is_in_check():
            return False
        return len(self.generate_all_legal_moves()) == 0

    def to_string(self) -> str:
        """Get a text representation of the board."""
        lines = ["  a b c d e f g h"]
        for y in range(7, -1, -1):
            row = [str(y + 1) + " "]
            for x in range(8):
                piece = self.squares[x][y]
                if piece:
                    row.append(piece.get_unicode() + " ")
                else:
                    row.append(". ")
            row.append(str(y + 1))
            lines.append("".join(row))
        lines.append("  a b c d e f g h")
        lines.append("White to move" if self.white_to_move else "Black to move")
        return "\n".join(lines)

    def copy(self) -> 'Board':
        """Create a deep copy of this board."""
        new_board = Board()
        for x in range(8):
            for y in range(8):
                if self.squares[x][y]:
                    new_board.squares[x][y] = self.squares[x][y].copy()
        new_board.white_to_move = self.white_to_move
        new_board.en_passant_target = self.en_passant_target
        new_board.white_can_castle_kingside = self.white_can_castle_kingside
        new_board.white_can_castle_queenside = self.white_can_castle_queenside
        new_board.black_can_castle_kingside = self.black_can_castle_kingside
        new_board.black_can_castle_queenside = self.black_can_castle_queenside
        new_board.move_history = self.move_history.copy()
        return new_board
