"""
Tether Chess (Tal's Forest) - Streamlit UI

A playable chess variant with Geometric Entanglement.
"""

import streamlit as st
from tether_chess import TetherChessEngine, Position, PieceType, Move

# Page configuration
st.set_page_config(
    page_title="Tether Chess (Tal's Forest)",
    page_icon="♞",
    layout="wide"
)

# Custom CSS for the chess board
st.markdown("""
<style>
    .chess-board {
        display: grid;
        grid-template-columns: repeat(8, 60px);
        grid-template-rows: repeat(8, 60px);
        gap: 0;
        border: 3px solid #333;
        margin: 20px auto;
        width: fit-content;
    }
    .chess-square {
        width: 60px;
        height: 60px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 40px;
        cursor: pointer;
        user-select: none;
    }
    .light-square {
        background-color: #f0d9b5;
    }
    .dark-square {
        background-color: #b58863;
    }
    .selected {
        background-color: #7fff00 !important;
    }
    .legal-move {
        background-color: #90ee90 !important;
    }
    .transporter-move {
        background-color: #87ceeb !important;
    }
    .rank-mate {
        box-shadow: inset 0 0 0 4px #ff6b6b;
    }
    .apex-move {
        background-color: #ffd700 !important;
    }
    .check {
        background-color: #ff6b6b !important;
    }
    .rules-box {
        background-color: #1e1e1e;
        padding: 15px;
        border-radius: 10px;
        border-left: 4px solid #4CAF50;
        margin: 10px 0;
    }
    .move-log {
        background-color: #2d2d2d;
        padding: 10px;
        border-radius: 5px;
        max-height: 300px;
        overflow-y: auto;
        font-family: monospace;
    }
    .stButton button {
        width: 100%;
    }
</style>
""", unsafe_allow_html=True)

# Initialize session state
if 'engine' not in st.session_state:
    st.session_state.engine = TetherChessEngine()
    st.session_state.engine.new_game()

if 'selected_square' not in st.session_state:
    st.session_state.selected_square = None

if 'legal_moves_for_selected' not in st.session_state:
    st.session_state.legal_moves_for_selected = []

if 'message' not in st.session_state:
    st.session_state.message = ""

if 'show_transporter_moves' not in st.session_state:
    st.session_state.show_transporter_moves = True


def get_square_class(x: int, y: int, engine: TetherChessEngine) -> str:
    """Determine CSS class for a square."""
    classes = []

    # Base color
    if (x + y) % 2 == 0:
        classes.append("dark-square")
    else:
        classes.append("light-square")

    pos = Position(x, y)

    # Check if selected
    if st.session_state.selected_square == pos:
        classes.append("selected")

    # Check if king is in check
    piece = engine.board.get_piece_at(pos)
    if piece and piece.piece_type == PieceType.KING:
        if piece.is_white == engine.board.white_to_move and engine.board.is_in_check():
            classes.append("check")

    # Check if this is a legal move destination
    for move in st.session_state.legal_moves_for_selected:
        if move.to_pos == pos:
            if move.is_pawn_knight_apex:
                classes.append("apex-move")
            elif move.is_transporter and st.session_state.show_transporter_moves:
                classes.append("transporter-move")
            else:
                classes.append("legal-move")
            break

    # Check if rank-mate of selected piece
    if st.session_state.selected_square:
        rank_mates = engine.get_rank_mates_positions(st.session_state.selected_square)
        if pos in rank_mates:
            classes.append("rank-mate")

    return " ".join(classes)


def handle_square_click(x: int, y: int):
    """Handle clicking on a square."""
    engine = st.session_state.engine
    pos = Position(x, y)
    needs_rerun = False

    if st.session_state.selected_square is None:
        # First click - select a piece
        piece = engine.board.get_piece_at(pos)
        if piece and piece.is_white == engine.board.white_to_move:
            st.session_state.selected_square = pos
            moves = engine.get_legal_moves_for_piece(pos)
            st.session_state.legal_moves_for_selected = moves
            native_count = len([m for m in moves if not m.is_transporter])
            trans_count = len([m for m in moves if m.is_transporter])
            st.session_state.message = f"Selected {piece.piece_type.name} at {pos} ({native_count} native + {trans_count} transporter = {len(moves)} total moves)"
            needs_rerun = True
    else:
        # Second click - try to make a move
        from_pos = st.session_state.selected_square

        if pos == from_pos:
            # Deselect
            st.session_state.selected_square = None
            st.session_state.legal_moves_for_selected = []
            st.session_state.message = ""
            needs_rerun = True
        else:
            # Try to move
            result = engine.make_move(from_pos, pos)

            if result.success:
                st.session_state.message = result.message
                st.session_state.selected_square = None
                st.session_state.legal_moves_for_selected = []
                needs_rerun = True
            else:
                # Maybe selecting a different piece?
                piece = engine.board.get_piece_at(pos)
                if piece and piece.is_white == engine.board.white_to_move:
                    st.session_state.selected_square = pos
                    st.session_state.legal_moves_for_selected = engine.get_legal_moves_for_piece(pos)
                    st.session_state.message = f"Selected {piece.piece_type.name} at {pos}"
                    needs_rerun = True
                else:
                    st.session_state.message = result.message
                    needs_rerun = True

    if needs_rerun:
        st.rerun()


def get_square_info(engine: TetherChessEngine, x: int, y: int):
    """Get display info for a square."""
    pos = Position(x, y)
    piece = engine.board.get_piece_at(pos)

    # Base color
    is_light = (x + y) % 2 == 1
    bg_color = "#f0d9b5" if is_light else "#b58863"

    # Move indicator
    indicator = ""
    move_type = None

    # Selected square
    if st.session_state.selected_square == pos:
        bg_color = "#7fff00"

    # Legal move destinations
    for move in st.session_state.legal_moves_for_selected:
        if move.to_pos == pos:
            if move.is_pawn_knight_apex:
                indicator = "⭐"  # Gold star for apex
                move_type = "apex"
            elif move.is_transporter:
                indicator = "🔵"  # Blue for transporter
                move_type = "transporter"
            else:
                indicator = "🟢"  # Green for native
                move_type = "native"
            break

    # Rank-mate indicator
    is_rank_mate = False
    if st.session_state.selected_square:
        rank_mates = engine.get_rank_mates_positions(st.session_state.selected_square)
        if pos in rank_mates:
            is_rank_mate = True

    # Check highlighting
    is_check = False
    if piece and piece.piece_type == PieceType.KING:
        if piece.is_white == engine.board.white_to_move and engine.board.is_in_check():
            bg_color = "#ff6b6b"
            is_check = True

    return {
        "piece": piece,
        "bg_color": bg_color,
        "indicator": indicator,
        "move_type": move_type,
        "is_rank_mate": is_rank_mate,
        "is_check": is_check
    }


def render_board(engine: TetherChessEngine):
    """Render the chess board."""
    # File labels
    cols = st.columns([0.5] + [1] * 8 + [0.5])
    with cols[0]:
        st.write("")
    for i, file in enumerate("abcdefgh"):
        with cols[i + 1]:
            st.markdown(f"<div style='text-align: center; font-weight: bold;'>{file}</div>",
                       unsafe_allow_html=True)

    # Board squares
    for y in range(7, -1, -1):
        cols = st.columns([0.5] + [1] * 8 + [0.5])

        # Rank label
        with cols[0]:
            st.markdown(f"<div style='line-height: 50px; font-weight: bold;'>{y + 1}</div>",
                       unsafe_allow_html=True)

        for x in range(8):
            with cols[x + 1]:
                info = get_square_info(engine, x, y)
                piece = info["piece"]

                # Build button label with piece and indicator
                piece_symbol = piece.get_unicode() if piece else ""
                indicator = info["indicator"]

                # Create label: piece on top, indicator below (or just indicator if empty)
                if piece_symbol and indicator:
                    label = f"{piece_symbol}\n{indicator}"
                elif piece_symbol:
                    label = piece_symbol
                elif indicator:
                    label = indicator
                else:
                    label = " "

                # Add rank-mate marker
                if info["is_rank_mate"]:
                    label = f"🔴{label}"

                if st.button(
                    label,
                    key=f"sq_{x}_{y}",
                    help=f"{Position(x, y)} - Click to select/move",
                    use_container_width=True
                ):
                    handle_square_click(x, y)

        # Rank label (right side)
        with cols[9]:
            st.markdown(f"<div style='line-height: 50px; font-weight: bold;'>{y + 1}</div>",
                       unsafe_allow_html=True)


def main():
    engine = st.session_state.engine

    st.title("♞ Tether Chess (Tal's Forest)")

    # Layout
    col1, col2 = st.columns([2, 1])

    with col1:
        # Game status
        info = engine.get_game_info()

        status_col1, status_col2, status_col3 = st.columns(3)
        with status_col1:
            turn = "⚪ White" if info["white_to_move"] else "⚫ Black"
            st.metric("Turn", turn)
        with status_col2:
            st.metric("Moves", info["move_count"])
        with status_col3:
            status = "In Check!" if info["in_check"] else info["game_state"].replace("_", " ").title()
            st.metric("Status", status)

        # Message
        if st.session_state.message:
            if "CHECKMATE" in st.session_state.message:
                st.error(st.session_state.message)
            elif "Check" in st.session_state.message:
                st.warning(st.session_state.message)
            elif "APEX" in st.session_state.message:
                st.success(st.session_state.message)
            elif "Transporter" in st.session_state.message:
                st.info(st.session_state.message)
            else:
                st.info(st.session_state.message)

        # Board
        st.markdown("---")
        render_board(engine)
        st.markdown("---")

        # Legend
        st.markdown("""
        **Legend (indicators on squares):**
        - 🟢 = Legal native move
        - 🔵 = Transporter move (borrowed movement)
        - ⭐ = Pawn-Knight Apex (instant promotion!)
        - 🔴 prefix = Rank-mate (piece sharing movement with selected)
        """)

    with col2:
        # Controls
        st.subheader("Controls")

        if st.button("🔄 New Game", use_container_width=True):
            st.session_state.engine.new_game()
            st.session_state.selected_square = None
            st.session_state.legal_moves_for_selected = []
            st.session_state.message = "New game started!"
            st.rerun()

        st.checkbox("Show Transporter Moves", value=True, key="show_transporter_moves")

        # Special moves available
        st.subheader("Special Moves Available")

        michael_tal = engine.get_michael_tal_opening_moves()
        if michael_tal:
            st.success(f"🎯 Michael Tal Opening: {len(michael_tal)} moves!")
            with st.expander("Show Michael Tal moves"):
                for move in michael_tal:
                    st.write(f"• {move.to_notation()}")

        apex_moves = engine.get_pawn_knight_apex_moves()
        if apex_moves:
            st.warning(f"⚡ Pawn-Knight Apex: {len(apex_moves)} promotions available!")
            with st.expander("Show Apex moves"):
                for move in apex_moves:
                    st.write(f"• {move.to_notation()}")

        transporter_count = len(engine.get_transporter_moves())
        if transporter_count > 0:
            st.info(f"🚀 Transporter moves: {transporter_count}")

        # Rules
        st.subheader("📜 The Four Rules")

        with st.expander("1. Rank Entanglement", expanded=False):
            st.markdown("""
            Pieces on the same **horizontal rank** share movement potential.
            A piece can borrow the movement pattern of any friendly piece on its rank.
            """)

        with st.expander("2. Pawn-Knight Apex", expanded=False):
            st.markdown("""
            A Pawn using a Knight's L-jump to reach the 8th rank
            promotes **IMMEDIATELY**. (Shown in gold)
            """)

        with st.expander("3. Native Lethality", expanded=False):
            st.markdown("""
            Only a piece's **native** movement can deliver check.
            A Rook teleporting next to a King doesn't check
            unless the King is on the Rook's file or rank.
            """)

        with st.expander("4. No Recursive Jumping", expanded=False):
            st.markdown("""
            Cannot chain borrowed moves.
            **One teleport per turn maximum.**
            """)

        with st.expander("The Disconnection", expanded=False):
            st.markdown("""
            When a piece moves to a new rank, it **immediately**
            loses its old rank-mates and gains new ones.
            Your allies are determined by where you stand.
            """)

        # Move log
        st.subheader("📝 Move Log")
        log = engine.get_game_log()
        log_text = "\n".join(log[-20:])  # Show last 20 entries
        st.text_area("", log_text, height=200, disabled=True)


if __name__ == "__main__":
    main()
