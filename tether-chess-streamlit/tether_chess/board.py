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

GAME MODES:
- LINEAR: Pieces teleport to destinations their rank-mates can reach
- QUANTUM: All pieces share combined abilities; each position calculates moves with union of all abilities
"""

from typing import List, Optional, Dict, Tuple, Set
from .models import Position, PieceType, Piece, Move, GameMode


class Board:
    """Tether Chess board with Geometric Entanglement."""

    def __init__(self, game_mode: GameMode = GameMode.LINEAR):
        self.squares: List[List[Optional[Piece]]] = [[None for _ in range(8)] for _ in range(8)]
        self.white_to_move: bool = True
        self.en_passant_target: Optional[Position] = None
        self.white_can_castle_kingside: bool = True
        self.white_can_castle_queenside: bool = True
        self.black_can_castle_kingside: bool = True
        self.black_can_castle_queenside: bool = True
        self.move_history: List[Move] = []
        self.game_mode: GameMode = game_mode  # LINEAR or QUANTUM

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

    def _find_piece_on_board(self, piece: Piece) -> Optional[Position]:
        """Find the actual board position of a piece by scanning the board."""
        for x in range(8):
            for y in range(8):
                if self.squares[x][y] is piece:
                    return Position(x, y)
        return None

    def identify_rank_mates(self, piece: Piece, piece_board_pos: Optional[Position] = None) -> List[Tuple[Piece, Position]]:
        """
        STEP 1: identifyRankMates(piece)
        Scans the current y-axis (rank) for friendly units.
        Returns list of (piece, actual_board_position) tuples.

        THE DISCONNECTION: This uses the piece's CURRENT position on the board.
        When a piece moves from Rank 3 to Rank 4:
        - It NO LONGER has access to Rank 3 allies' movement
        - It NOW has access to any Rank 4 allies' movement
        """
        # Find the actual board position of the piece
        if piece_board_pos is None:
            piece_board_pos = self._find_piece_on_board(piece)
        if piece_board_pos is None:
            return []

        rank = piece_board_pos.y
        rank_mates = []

        for x in range(8):
            other = self.squares[x][rank]
            if other and other is not piece and other.is_white == piece.is_white:
                # Return both the piece and its ACTUAL board position
                rank_mates.append((other, Position(x, rank)))

        return rank_mates

    def calculate_transporter_vector(self, piece: Piece, mates: List[Tuple[Piece, Position]],
                                      piece_board_pos: Optional[Position] = None) -> List[Move]:
        """
        STEP 2: calculateTransporterVector(piece, mates)
        Dispatches to LINEAR or QUANTUM mode based on game_mode.
        """
        if self.game_mode == GameMode.QUANTUM:
            return self._calculate_quantum_transporter(piece, mates, piece_board_pos)
        else:
            return self._calculate_linear_transporter(piece, mates, piece_board_pos)

    def _calculate_linear_transporter(self, piece: Piece, mates: List[Tuple[Piece, Position]],
                                       piece_board_pos: Optional[Position] = None) -> List[Move]:
        """
        LINEAR MODE: Pieces TELEPORT to where rank-mates can go.
        The moving piece goes to destinations reachable by rank-mates from THEIR positions.
        """
        if piece_board_pos is None:
            piece_board_pos = self._find_piece_on_board(piece)
        if piece_board_pos is None:
            return []

        transporter_moves = []
        seen_targets: Set[Position] = set()

        for mate, mate_origin in mates:
            for dx, dy, is_sliding in mate.get_native_movement_vectors():
                if is_sliding:
                    for distance in range(1, 8):
                        target = mate_origin.offset(dx * distance, dy * distance)
                        if not target.is_valid():
                            break

                        target_piece = self.get_piece_at(target)

                        if target == piece_board_pos:
                            if target_piece:
                                break
                            continue

                        if target_piece:
                            if target_piece.is_white != piece.is_white:
                                if target not in seen_targets:
                                    move = self._create_transporter_move(
                                        piece, piece_board_pos, target, mate, target_piece
                                    )
                                    if move:
                                        transporter_moves.append(move)
                                        seen_targets.add(target)
                            break

                        if target not in seen_targets:
                            move = self._create_transporter_move(piece, piece_board_pos, target, mate, None)
                            if move:
                                transporter_moves.append(move)
                                seen_targets.add(target)
                else:
                    target = mate_origin.offset(dx, dy)
                    if not target.is_valid():
                        continue

                    if target == piece_board_pos:
                        continue

                    target_piece = self.get_piece_at(target)

                    if target_piece and target_piece.is_white == piece.is_white:
                        continue

                    if target not in seen_targets:
                        move = self._create_transporter_move(piece, piece_board_pos, target, mate, target_piece)
                        if move:
                            transporter_moves.append(move)
                            seen_targets.add(target)

        return transporter_moves

    def _calculate_quantum_transporter(self, piece: Piece, mates: List[Tuple[Piece, Position]],
                                        piece_board_pos: Optional[Position] = None) -> List[Move]:
        """
        QUANTUM MODE: All pieces on a rank inherit ALL abilities from each other.

        Algorithm:
        1. Collect combined movement vectors from ALL pieces on the rank (including moving piece)
        2. For EACH piece position on the rank, calculate reachable squares using combined abilities
        3. The UNION of all reachable squares is available to the moving piece
        """
        if piece_board_pos is None:
            piece_board_pos = self._find_piece_on_board(piece)
        if piece_board_pos is None:
            return []

        transporter_moves = []
        seen_targets: Set[Position] = set()

        # Step 1: Collect ALL movement vectors from ALL pieces on the rank (combined abilities)
        all_vectors: Set[Tuple[int, int, bool]] = set()

        # Add moving piece's native vectors
        for dx, dy, is_sliding in piece.get_native_movement_vectors():
            all_vectors.add((dx, dy, is_sliding))

        # Add all rank-mates' vectors
        for mate, _ in mates:
            for dx, dy, is_sliding in mate.get_native_movement_vectors():
                all_vectors.add((dx, dy, is_sliding))

        # All pieces on the rank (including moving piece)
        all_rank_pieces: List[Tuple[Piece, Position]] = [(piece, piece_board_pos)] + mates

        # Step 2: For EACH piece position, calculate reachable squares using combined abilities
        for source_piece, source_pos in all_rank_pieces:
            for dx, dy, is_sliding in all_vectors:
                if is_sliding:
                    for distance in range(1, 8):
                        target = source_pos.offset(dx * distance, dy * distance)
                        if not target.is_valid():
                            break

                        # Skip if target is the moving piece's current position
                        if target == piece_board_pos:
                            continue

                        target_piece = self.get_piece_at(target)

                        # Skip friendly pieces (can't land on them)
                        if target_piece and target_piece.is_white == piece.is_white:
                            break  # Path blocked by friendly

                        if target_piece:
                            # Can capture enemy
                            if target not in seen_targets:
                                move = self._create_transporter_move(
                                    piece, piece_board_pos, target, source_piece, target_piece
                                )
                                if move:
                                    transporter_moves.append(move)
                                    seen_targets.add(target)
                            break  # Path blocked

                        # Empty square
                        if target not in seen_targets:
                            move = self._create_transporter_move(
                                piece, piece_board_pos, target, source_piece, None
                            )
                            if move:
                                transporter_moves.append(move)
                                seen_targets.add(target)
                else:
                    # Non-sliding (Knight L-jump, King step)
                    target = source_pos.offset(dx, dy)
                    if not target.is_valid():
                        continue

                    # Skip moving piece's own position
                    if target == piece_board_pos:
                        continue

                    target_piece = self.get_piece_at(target)

                    # Skip friendly pieces
                    if target_piece and target_piece.is_white == piece.is_white:
                        continue

                    if target not in seen_targets:
                        move = self._create_transporter_move(
                            piece, piece_board_pos, target, source_piece, target_piece
                        )
                        if move:
                            transporter_moves.append(move)
                            seen_targets.add(target)

        return transporter_moves

    def _create_transporter_move(
        self, piece: Piece, from_pos: Position, target: Position, mate: Piece, captured: Optional[Piece]
    ) -> Optional[Move]:
        """Create a transporter move, handling Pawn-Knight Apex promotion."""
        promotion_type = None

        # PAWN-KNIGHT APEX: Instant promotion when Knight-jumping to 8th rank!
        if piece.piece_type == PieceType.PAWN and target.is_promotion_rank(piece.is_white):
            promotion_type = PieceType.QUEEN  # Default to Queen

        return Move(
            from_pos=from_pos,
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
                    # Use board position (x,y) not piece.position for robustness
                    if self._can_natively_reach_from(attacker, Position(x, y), target):
                        return True
        return False

    def is_under_any_attack(self, target: Position, by_white: bool) -> bool:
        """
        STEALTH CAPTURE PREVENTION:
        Check if a position is under attack by ANY move (native OR transporter).

        The King is forbidden from moving into any square that can be attacked
        by any enemy piece, including squares reachable via Tethered/Borrowed moves.

        This is different from Check detection, which only uses native attacks.
        """
        # First check native attacks (fast path)
        if self.is_under_native_attack(target, by_white):
            return True

        # Now check transporter attacks:
        # A piece can attack any square that its rank-mates can reach
        for x in range(8):
            for y in range(8):
                piece = self.squares[x][y]
                if piece and piece.is_white == by_white:
                    # Find rank-mates for this piece
                    piece_pos = Position(x, y)
                    rank = y

                    for mate_x in range(8):
                        if mate_x == x:
                            continue
                        mate = self.squares[mate_x][rank]
                        if mate and mate.is_white == by_white:
                            # Check if mate can reach target from MATE's position
                            mate_origin = Position(mate_x, rank)
                            if self._can_transporter_reach(mate, mate_origin, target, piece_pos):
                                return True

        return False

    def _can_transporter_reach(self, mate: Piece, mate_origin: Position,
                                target: Position, moving_piece_pos: Position) -> bool:
        """
        Check if a transporter move from mate's position can reach target.
        The moving piece would teleport from moving_piece_pos to target via mate's movement.
        """
        for dx, dy, is_sliding in mate.get_native_movement_vectors():
            if is_sliding:
                for distance in range(1, 8):
                    check_pos = mate_origin.offset(dx * distance, dy * distance)
                    if not check_pos.is_valid():
                        break

                    if check_pos == target:
                        return True

                    # Skip the moving piece's own position (it would move away)
                    if check_pos == moving_piece_pos:
                        continue

                    # Path blocked by another piece
                    if self.get_piece_at(check_pos):
                        break
            else:
                # Non-sliding: Knight, King - single jump
                check_pos = mate_origin.offset(dx, dy)
                if check_pos.is_valid() and check_pos == target:
                    return True

        return False

    def _can_natively_reach_from(self, piece: Piece, origin: Position, target: Position) -> bool:
        """Check if a piece can natively reach target from origin (with path checking)."""

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
                    # Pass actual board position to avoid redundant lookup
                    all_moves.extend(self.generate_legal_moves_for_piece(
                        piece, piece_board_pos=Position(x, y)
                    ))

        return all_moves

    def generate_legal_moves_for_piece(self, piece: Piece, skip_check_filter: bool = False,
                                         piece_board_pos: Optional[Position] = None) -> List[Move]:
        """Generate all legal moves for a specific piece."""
        # Find the actual board position
        if piece_board_pos is None:
            piece_board_pos = self._find_piece_on_board(piece)
        if piece_board_pos is None:
            return []

        moves = []

        # Native moves (pass board position for consistency)
        moves.extend(self._generate_native_moves(piece, piece_board_pos))

        # Transporter moves (Geometric Entanglement)
        # identify_rank_mates returns List[Tuple[Piece, Position]] with actual board positions
        rank_mates = self.identify_rank_mates(piece, piece_board_pos)
        moves.extend(self.calculate_transporter_vector(piece, rank_mates, piece_board_pos))

        # Filter moves that leave king in check (standard chess legality)
        if not skip_check_filter:
            legal_moves = []
            for move in moves:
                if not self._leaves_king_in_check(move):
                    legal_moves.append(move)
            moves = legal_moves

        return moves

    def _generate_native_moves(self, piece: Piece, origin: Optional[Position] = None) -> List[Move]:
        """Generate native (non-transporter) moves."""
        moves = []
        # Use provided origin or find it on the board
        if origin is None:
            origin = self._find_piece_on_board(piece)
        if origin is None:
            return []

        if piece.piece_type == PieceType.PAWN:
            moves.extend(self._generate_pawn_moves(piece, origin))
        elif piece.piece_type == PieceType.KING:
            moves.extend(self._generate_king_moves(piece, origin))
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

    def _generate_pawn_moves(self, pawn: Piece, origin: Optional[Position] = None) -> List[Move]:
        """Generate pawn moves."""
        moves = []
        if origin is None:
            origin = self._find_piece_on_board(pawn)
        if origin is None:
            return []

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

    def _generate_king_moves(self, king: Piece, origin: Optional[Position] = None) -> List[Move]:
        """Generate king moves including castling."""
        moves = []
        if origin is None:
            origin = self._find_piece_on_board(king)
        if origin is None:
            return []

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
                    # STEALTH CAPTURE: King cannot pass through ANY attackable squares
                    if (not self.get_piece_at(origin.offset(1, 0)) and
                        not self.get_piece_at(origin.offset(2, 0)) and
                        not self.is_under_any_attack(origin, not king.is_white) and
                        not self.is_under_any_attack(origin.offset(1, 0), not king.is_white) and
                        not self.is_under_any_attack(origin.offset(2, 0), not king.is_white)):
                        moves.append(Move(origin, origin.offset(2, 0), king, is_castling=True))

            if can_qs:
                rook_pos = Position(0, origin.rank)
                rook = self.get_piece_at(rook_pos)
                if rook and rook.piece_type == PieceType.ROOK and not rook.has_moved:
                    # STEALTH CAPTURE: King cannot pass through ANY attackable squares
                    if (not self.get_piece_at(origin.offset(-1, 0)) and
                        not self.get_piece_at(origin.offset(-2, 0)) and
                        not self.get_piece_at(origin.offset(-3, 0)) and
                        not self.is_under_any_attack(origin, not king.is_white) and
                        not self.is_under_any_attack(origin.offset(-1, 0), not king.is_white) and
                        not self.is_under_any_attack(origin.offset(-2, 0), not king.is_white)):
                        moves.append(Move(origin, origin.offset(-2, 0), king, is_castling=True))

        return moves

    def _leaves_king_in_check(self, move: Move) -> bool:
        """
        Check if a move leaves the moving side's king in check.

        STEALTH CAPTURE RULE:
        - For King moves: Check is_under_any_attack (native + transporter)
        - For other pieces: Check is_under_native_attack only (standard chess check)
        """
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

        if king_pos:
            if piece.piece_type == PieceType.KING:
                # STEALTH CAPTURE: King cannot move to ANY attackable square
                in_danger = self.is_under_any_attack(king_pos, not piece.is_white)
            else:
                # Other pieces: standard check detection (native attacks only)
                in_danger = self.is_under_native_attack(king_pos, not piece.is_white)
        else:
            in_danger = False

        # Unmake move
        self.squares[from_pos.x][from_pos.y] = piece
        self.squares[to_pos.x][to_pos.y] = captured
        piece.position = original_pos

        if move.is_en_passant and ep_captured:
            ep_pos = Position(to_pos.x, from_pos.y)
            self.squares[ep_pos.x][ep_pos.y] = ep_captured

        return in_danger

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
