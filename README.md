# Scrabble with AI

A full desktop Scrabble game, written in Java Swing, that lets a human player face off against an AI opponent. The AI's move generator is based on the anchor-square / cross-check backtracking algorithm from Andrew W. Appel and Guy J. Jacobson's classic paper, **["The World's Fastest Scrabble Program"](https://www.cs.upc.edu/prop/data/uploads/fastestscrabble.pdf)** (*Communications of the ACM*, May 1988).

## How it works

Appel and Jacobson's insight was that Scrabble move generation can be reduced from a 2D board search to a series of 1D row searches by precomputing two things for every row before search begins:

- **Anchors** — empty squares adjacent to an already-placed tile. Every legal move must cover at least one anchor, so the search only ever starts from these squares instead of scanning the whole board.
- **Cross-checks** — for each empty square, the set of letters that would form a *valid* perpendicular word if a tile were placed there. This turns a 2D legality check into an O(1) bitset lookup.

From each anchor, the algorithm backtracks in two phases:
1. **Place a left part** — tiles from the rack laid down to the left of the anchor.
2. **Extend right** — grow the word letter by letter, using rack tiles (filtered through the cross-check set) and any tiles already on the board, recording a legal move every time a dictionary word is completed.

The original paper stores the dictionary as a **DAWG** (Directed Acyclic Word Graph) — a trie with duplicate suffix subtrees merged — to keep the ~90k-word lexicon small enough to hold entirely in memory. This implementation follows the same anchor/cross-check/extend-right search strategy, but represents the dictionary as a standard 26-way **trie** (`MyTrie` / `TrieNode`) rather than a minimized DAWG, trading some memory efficiency for a simpler implementation.

The AI evaluates every legal move it finds and greedily plays the highest-scoring one — the same "no real strategy, just brute-force one-ply search" approach the paper describes, which the authors note is still strong enough to beat most human players.

## Project structure

```
application/
├── ai/
│   └── TheAI.java          Move generation: anchors, cross-checks, backtracking search
├── datastructures/
│   ├── MyTrie.java         Dictionary trie (insert, validate, prefix lookup)
│   └── TrieNode.java       Trie node (26 child pointers + end-of-word flag)
└── model/
    ├── Board.java          15x15 board, premium squares, scoring, move validation
    ├── BoardCell.java      Single board cell (tile, premium type, cross-checks)
    ├── Move.java           A candidate/placed move (tiles, positions, score)
    ├── Tile.java           A single letter tile and its point value
    └── TileBag.java        The 100-tile bag, draws, and remaining-tile tracking

gui/
├── components/             Swing components: board grid, tile rack, scoreboard,
│                            remaining-tiles counter, AI move animation, toolbar
├── frames/                 Top-level JFrame windows (main game window, tile-swap window)
└── panels/                 JPanel game logic (turn handling, drag/drop, swapping)

game_results/                Move logs written out during play (see below)
misc/GrpMembers.txt          Project group members
```

## Getting started

1. Make sure you have a JDK installed.
2. Compile the sources (or open the project in your IDE of choice — the `application` and `gui` packages are laid out as standard Java source roots).
3. Run `gui.frames.Frame`, which contains the `main` method:

   ```
   java gui.frames.Frame
   ```

   If you have a pre-built `.jar`, you can just run that instead.

## How to play

- The game opens on a standard 15x15 Scrabble board with double/triple letter and word premium squares.
- Drag tiles from your rack onto the board to form a word, or click the robot icon to let the AI take its turn.
- Use the swap panel to exchange tiles from your rack with the bag instead of playing a word.
- Scores, the remaining tile count, and whose turn it is are all shown live in the side panels.

## Move logs

While the game runs, the AI's search results and move history are written to the `game_results/` folder:

- **`moves.txt`** — a running log of every move played, by whom, and its score.
- **`horiMoves.txt`** — all legal horizontal moves found for the AI's current turn, with their board positions.
- **`vertiMoves.txt`** — the same, for vertical moves.

## Contributions

This was built as a data structures course project. Contributions, bug reports, and improvements are welcome — feel free to fork and extend it.

## Acknowledgments

Thanks to itbaan's instructor, Sir Jawwad Ahmed Farid, for inspiring him to use the Appel & Jacobson paper that this project's move generator is based on.

Enjoy.