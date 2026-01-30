# Tether Chess (Tal's Forest)

A chess variant with **Geometric Entanglement** - pieces on the same rank share movement potential.

## Quick Start

```bash
pip install -r requirements.txt
streamlit run app.py
```

## The Four Unique Rules

1. **Rank Entanglement** - Pieces share movement with friendly pieces on same rank
2. **Pawn-Knight Apex** - Pawn using Knight's jump to 8th rank promotes instantly
3. **Native Lethality** - Only native movement can deliver check/checkmate
4. **No Recursive Jumping** - One teleport per turn maximum

## The Disconnection

When a piece moves ranks, it immediately loses old allies and gains new ones. The entanglement is position-based.

## Deploy to Streamlit Cloud

1. Push this folder to a GitHub repository
2. Go to [share.streamlit.io](https://share.streamlit.io)
3. Connect your GitHub account
4. Select the repository and `app.py`
5. Deploy!

## License

MIT
