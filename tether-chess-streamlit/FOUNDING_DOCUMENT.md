# TETHER CHESS (Tal's Forest)
## The Founding Document

---

## THE FOUR UNIQUE RULES

### 1. RANK ENTANGLEMENT (Geometric Entanglement)
Every piece on a horizontal rank shares its movement potential with all friendly pieces on that same rank.

**The Transporter:** Piece A can TELEPORT to any square that Piece B (on the same rank) can legally reach from Piece B's position. The moving piece goes WHERE its rank-mates CAN go.

**Example:** Queen at d1 with Knights at b1 and g1 can teleport to a3, c3 (b1 Knight's reachable squares) or f3, h3 (g1 Knight's reachable squares).

**Path Integrity:** For sliding rank-mates (Bishop/Rook/Queen), the path must be clear from the RANK-MATE's position, not the moving piece.

---

### 2. PAWN-KNIGHT APEX (Instant Promotion)
A Pawn sharing a rank with a Knight can utilize the Knight's L-jump. **If that jump lands the Pawn on the 8th rank, it promotes IMMEDIATELY.**

This creates explosive tactical possibilities:
- A pawn on the 6th rank with a Knight ally can teleport-promote in one move
- The threat of Pawn-Knight Apex forces defensive awareness across ranks

---

### 3. NATIVE LETHALITY (Check Rule)
A piece can **only** deliver Check or Checkmate using its own **native** movement rules.

A teleporting Rook landing next to a King does **NOT** check the King unless the King is on the Rook's horizontal or vertical axis.

**You can fly across the board, but you must land in a position where your own soul (your native move) can strike.**

**STEALTH CAPTURE PREVENTION:** While Check is only triggered by native attacks, the King is **forbidden** from moving into ANY square that can be attacked by enemy pieces - including squares reachable via transporter moves. The King must avoid all danger, not just direct lines of attack.

---

### 4. NO RECURSIVE JUMPING
Cannot chain borrowed moves. Each turn allows **one** transporter move maximum.

The borrowed movement cannot itself be used to borrow again. This prevents infinite movement chains and keeps the game grounded.

---

## THE DISCONNECTION

When a piece moves from one rank to another, it **immediately** loses all borrowed capabilities from its former rank-mates.

**Example:** A Rook on Rank 3 sharing with a Knight can use Knight-jumps. When that Rook moves to Rank 4, it instantly loses access to the Knight's movement (The Disconnection). On subsequent turns, it gains access to whatever allies exist on Rank 4.

The entanglement is **position-based**, not persistent. Your allies are determined by where you stand, not where you came from.

---

## THE MICHAEL TAL OPENING

Named after the legendary attacking player, this opening involves using the back-rank Knights to teleport the Queen or Rooks over the pawn wall on Move 1.

**Example:** White's Queen on d1 shares the first rank with the Knight on b1. The Queen can borrow the Knight's L-jump to land on c3, e3, or other Knight-reachable squares on the first move!

---

## IMPLEMENTATION LOGIC

The engine executes three checks every turn:

```
1. identify_rank_mates(piece)
   → Scans the current y-axis for friendly units

2. calculate_transporter_vector(piece, mates)
   → Generates move-set from union of all mates.native_moves
   → Applied to piece.origin

3. validate_lethality(target_square)
   → After move, checks if active_piece.native_movement
   → Intersects with enemy_king.position
```

---

## STRATEGIC "QUIET RESPECT"

Despite the high-octane teleportation, the game remains grounded by the Native Lethality rule. The ability to appear anywhere is balanced by the requirement to threaten with your own essence.

> *"In Tal's Forest, every piece carries its soul with it. You may borrow wings, but you strike with your own sword."*

---

## FILE STRUCTURE

```
tether-chess-streamlit/
├── app.py                    # Streamlit UI
├── tether_chess/
│   ├── __init__.py          # Package init
│   ├── models.py            # Position, PieceType, Piece, Move
│   ├── board.py             # Board + three core checks
│   └── engine.py            # TetherChessEngine orchestrator
├── FOUNDING_DOCUMENT.md     # This file
└── requirements.txt         # Dependencies
```

---

## RUNNING THE GAME

```bash
cd tether-chess-streamlit
pip install -r requirements.txt
streamlit run app.py
```

---

## UI FEATURES

| Color | Meaning |
|-------|---------|
| 🟢 Green | Legal native moves |
| 🔵 Blue | Transporter moves (borrowed movement) |
| 🟡 Gold | Pawn-Knight Apex (instant promotion) |
| 🔴 Red border | Rank-mates (pieces sharing movement) |
| 🔴 Red background | King in check |

---

*Tether Chess: Where geometry becomes destiny.*
