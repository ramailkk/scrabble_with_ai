# Scrabble Game with AI

## Overview

A complete implementation of the classic Scrabble board game featuring a powerful AI opponent. This project implements the algorithm described in **"The World's Fastest Scrabble Program"** by Andrew W. Appel and Guy J. Jacobson, utilizing efficient data structures for rapid move generation and word validation.

### 📄 Reference Paper
[The World's Fastest Scrabble Program](https://www.cs.upc.edu/prop/data/uploads/fastestscrabble.pdf) – Andrew W. Appel and Guy J. Jacobson, Princeton University

---

### Core Gameplay
- **Two-player mode** – Human vs. Human or Human vs. AI
- **Intelligent AI opponent** – Powered by the Appel-Jacobson algorithm
- **Standard Scrabble rules** – Official tile distribution, scoring, and special squares
- **Real-time scoring** – Dynamic score updates with potential move preview

### AI Implementation
- **Trie-based dictionary** – Efficient O(word_length) word validation and prefix matching
- **Anchor square search** – Only explores promising positions adjacent to existing tiles
- **Cross-check optimization** – Pre-computed valid letters for each board cell
- **Greedy scoring** – Selects the highest-scoring valid move from all candidates
- **Move logging** – All AI move candidates saved for analysis

### User Interface
- **Drag-and-drop** – Intuitive tile placement by dragging from rack to board
- **Tile movement** – Drag tiles between board cells or back to the rack
- **Visual feedback**
  - Green highlights for valid words
  - Red warnings for invalid placements
  - Structural error indicators (misaligned tiles, gaps)
- **AI move animation** – Watch the AI place tiles one by one
- **Tile swap dialog** – Exchange unwanted tiles from your rack
- **Remaining tiles display** – Real-time view of tile bag contents with vowel/consonant counts

### Game Logging
- **moves.txt** – Complete move history with scores
- **horiMoves.txt** – All horizontal AI move candidates
- **vertiMoves.txt** – All vertical AI move candidates

---

## Project Structure
ramailkk-scrabble_with_ai/
├── application/
│ ├── ai/
│ │ └── TheAI.java # AI move generation and scoring
│ ├── datastructures/
│ │ ├── MyTrie.java # Trie dictionary implementation
│ │ └── TrieNode.java # Trie node structure
│ └── model/
│ ├── Board.java # Game board logic and state
│ ├── BoardCell.java # Individual cell with cross-checks
│ ├── Move.java # Move data structure
│ ├── Tile.java # Tile with letter and value
│ └── TileBag.java # Tile bag with distribution
├── gui/
│ ├── components/
│ │ ├── ActionToolbarComponent.java # Control buttons
│ │ ├── AIMoveAnimator.java # AI tile placement animation
│ │ ├── BoardGridComponent.java # Board rendering and interaction
│ │ ├── RemainingTilesComponent.java # Tile bag visualization
│ │ ├── RoundedButton.java # Custom button styling
│ │ ├── ScoreComponent.java # Score display with preview
│ │ └── TileRackComponent.java # Player tile racks
│ ├── frames/
│ │ ├── Frame.java # Main game window
│ │ └── Swap_Frame.java # Swap dialog window
│ └── panels/
│ ├── Panel.java # Main game panel (controller)
│ └── Swap_Panel.java # Swap panel logic
├── game_results/ # Game logs
│ ├── moves.txt # All moves with scores
│ ├── horiMoves.txt # Horizontal AI moves
│ └── vertiMoves.txt # Vertical AI moves
├── resources/
│ ├── imgs/ # Tile images and icons
│ └── Dictionaries/
│ └── words.txt # Word list dictionary
└── misc/
└── GrpMembers.txt # Team member information

---

## How the AI Works

The AI follows the **Appel-Jacobson algorithm** with the following key steps:

### 1. Anchor Square Identification
- Identifies empty squares adjacent to existing tiles on the board
- These anchor squares are the only positions where new words can be formed

### 2. Cross-Check Computation
- For each anchor square, determines which letters can legally be placed there
- Considers vertical word formations when building horizontal words (and vice versa)
- Pre-computes valid letters to prune the search space

### 3. Move Generation
- For each anchor, explores left and right (or up and down) directions
- Uses the trie to efficiently generate all valid words
- Tracks partial words and extends them only if they form valid prefixes

### 4. Scoring
- Calculates score for each valid move including:
  - Tile values (1-10 points)
  - Letter bonuses (DL, TL)
  - Word bonuses (DW, TW)
  - Bingo bonus (+50 for using all 7 tiles)
- Selects the highest-scoring move

---

## Game Flow

1. **Player 1 starts** – Place tiles on the board by clicking rack tiles then board cells, or by drag-and-drop
2. **Valid move required**
   - Tiles must form valid dictionary words
   - Words must connect to existing tiles (except first move at center)
   - All placed tiles must be in a single row or column without gaps
3. **Score calculation** – Points awarded based on tile values, bonuses, and bingo
4. **Tile replenishment** – Rack refilled to 7 tiles from the tile bag
5. **Turn alternates** – Player 1 ↔ Player 2
6. **Pressing AI Button** Let the AI make a move on your behalf |
7. **Game ends** – When tile bag is empty and a player cannot make a valid move

