package gui.components;

import application.model.Board;
import application.model.BoardCell;
import application.model.Tile;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class ScoreComponent {

    private Board board;
    private BoardCell[][] refBoard;
    private java.util.List<int[]> tempPositions;
    private java.util.List<Tile> tilesSelectedFromRack;

    public ScoreComponent(Board board, BoardCell[][] refBoard) {
        this.board = board;
        this.refBoard = refBoard;
        this.tempPositions = new ArrayList<>();
        this.tilesSelectedFromRack = new ArrayList<>();
    }

    /**
     * Updates the temporary positions and tiles references for score calculation
     */
    public void setTempData(java.util.List<int[]> tempPositions, java.util.List<Tile> tilesSelectedFromRack) {
        this.tempPositions = tempPositions;
        this.tilesSelectedFromRack = tilesSelectedFromRack;
    }

    /**
     * Calculates the potential score of the current move being built.
     * Uses the same scoring logic as the Board's phantomScorer method.
     */
    private int calculatePotentialScore() {
        if (tempPositions == null || tempPositions.isEmpty() || 
            tilesSelectedFromRack == null || tilesSelectedFromRack.isEmpty()) {
            return 0;
        }
        
        // Create a temporary board with the current placements
        BoardCell[][] tempBoard = new BoardCell[15][15];
        for (int r = 0; r < 15; r++) {
            for (int c = 0; c < 15; c++) {
                try {
                    tempBoard[r][c] = (BoardCell) refBoard[r][c].clone();
                } catch (Exception e) {
                    tempBoard[r][c] = refBoard[r][c];
                }
            }
        }
        
        // Place temporary tiles
        for (int i = 0; i < tempPositions.size(); i++) {
            int r = tempPositions.get(i)[0];
            int c = tempPositions.get(i)[1];
            tempBoard[r][c].setTileOnCell(tilesSelectedFromRack.get(i));
        }
        
        // Find all words formed
        ArrayList<ArrayList<Integer>> allWordsOnTheMove = new ArrayList<>();
        ArrayList<String> allWordStrings = new ArrayList<>();
        Set<String> seenWords = new HashSet<>();
        
        // For each placed tile, find horizontal and vertical words
        for (int[] pos : tempPositions) {
            int r = pos[0];
            int c = pos[1];
            
            // Check horizontal word
            int cStart = c, cEnd = c;
            while (cStart - 1 >= 0 && tempBoard[r][cStart - 1].isOccupied) cStart--;
            while (cEnd + 1 <= 14 && tempBoard[r][cEnd + 1].isOccupied) cEnd++;
            if (cEnd > cStart) {
                ArrayList<Integer> wordPos = new ArrayList<>();
                StringBuilder word = new StringBuilder();
                for (int cc = cStart; cc <= cEnd; cc++) {
                    wordPos.add(board.encryptPostion(r, cc));
                    word.append(tempBoard[r][cc].tile.letter);
                }
                String wordStr = word.toString();
                String key = "H" + r + "_" + cStart + "_" + cEnd;
                if (wordStr.length() > 1 && board.dictionary.validateWord(wordStr) && seenWords.add(key)) {
                    allWordsOnTheMove.add(wordPos);
                    allWordStrings.add(wordStr);
                }
            }
            
            // Check vertical word
            int rStart = r, rEnd = r;
            while (rStart - 1 >= 0 && tempBoard[rStart - 1][c].isOccupied) rStart--;
            while (rEnd + 1 <= 14 && tempBoard[rEnd + 1][c].isOccupied) rEnd++;
            if (rEnd > rStart) {
                ArrayList<Integer> wordPos = new ArrayList<>();
                StringBuilder word = new StringBuilder();
                for (int rr = rStart; rr <= rEnd; rr++) {
                    wordPos.add(board.encryptPostion(rr, c));
                    word.append(tempBoard[rr][c].tile.letter);
                }
                String wordStr = word.toString();
                String key = "V" + c + "_" + rStart + "_" + rEnd;
                if (wordStr.length() > 1 && board.dictionary.validateWord(wordStr) && seenWords.add(key)) {
                    allWordsOnTheMove.add(wordPos);
                    allWordStrings.add(wordStr);
                }
            }
        }
        
        // If no valid words formed, return 0
        if (allWordsOnTheMove.isEmpty()) {
            return 0;
        }
        
        // Calculate score using the same logic as Board.phantomScorer
        ArrayList<ArrayList<Tile>> wordsTile = new ArrayList<>();
        for (String w : allWordStrings) {
            ArrayList<Tile> wT = new ArrayList<>();
            for (int i = 0; i < w.length(); i++) {
                wT.add(new Tile(w.charAt(i)));
            }
            wordsTile.add(wT);
        }
        
        boolean bingo = false;
        if (tilesSelectedFromRack.size() >= 7) {
            int placedCount = 0;
            for (int[] pos : tempPositions) {
                if (!refBoard[pos[0]][pos[1]].isOccupied) {
                    placedCount++;
                }
            }
            bingo = (placedCount == 7);
        }
        
        int score = 0;
        boolean[][] spVisited = new boolean[15][15];
        
        for (int i = 0; i < allWordsOnTheMove.size(); i++) {
            boolean dw = false;
            boolean tw = false;
            int wordScore = 0;
            
            for (int j = 0; j < allWordsOnTheMove.get(i).size(); j++) {
                int pos = allWordsOnTheMove.get(i).get(j);
                int r = board.decryptPosition(pos)[0];
                int c = board.decryptPosition(pos)[1];
                int tileValue = wordsTile.get(i).get(j).value;
                
                // Calculate letter score with bonuses
                int letterScore = tileValue;
                
                if (tempBoard[r][c].speciality == 1) { // Double Letter
                    if (!spVisited[r][c]) {
                        letterScore = tileValue * 2;
                        spVisited[r][c] = true;
                    }
                } else if (tempBoard[r][c].speciality == 2) { // Triple Letter
                    if (!spVisited[r][c]) {
                        letterScore = tileValue * 3;
                        spVisited[r][c] = true;
                    }
                } else if (tempBoard[r][c].speciality == 3) { // Double Word
                    if (!spVisited[r][c]) {
                        dw = true;
                        spVisited[r][c] = true;
                    }
                } else if (tempBoard[r][c].speciality == 4) { // Triple Word
                    if (!spVisited[r][c]) {
                        tw = true;
                        spVisited[r][c] = true;
                    }
                }
                
                wordScore += letterScore;
            }
            
            // Apply word multipliers
            if (tw) wordScore *= 3;
            if (dw) wordScore *= 2;
            
            score += wordScore;
        }
        
        if (bingo) score += 50;
        return score;
    }

    /**
     * Draws the score display for a player
     */
    public void drawScore(Graphics2D g, int x, int y, int currentScore, int player, boolean isCurrentTurn) {
        int potentialScore = calculatePotentialScore();
        
        // Draw player label and turn indicator
        g.setColor(Color.BLACK);
        Font originalFont = g.getFont();
        g.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        // Player name and turn indicator on the left side
        String playerText = "Player " + player;
        g.drawString(playerText, x, y+8);
        
        if (isCurrentTurn) {
            g.setColor(new Color(0, 150, 0));
            g.setFont(new Font("Segoe UI", Font.BOLD, 32));
            g.drawString("Your turn", x + 100, y);
            g.setColor(Color.BLACK);
        }
        
        // Score on the right side with proper spacing
        int fontSize = 28;
        g.setFont(new Font("Consolas", Font.BOLD, fontSize));
        int scoreX = x + 290; // Position score to the right
        
        if (potentialScore > 0 && isCurrentTurn && !tempPositions.isEmpty()) {
            // Show: Score = X + Y = Z
            String scoreText = "Score = " + currentScore;
            String plusText = " +" + potentialScore;
            String equalsText = " = " + (currentScore + potentialScore);
            
            int currentX = scoreX;
            int baselineY = y + 45;
            
            // Draw base score
            g.setColor(Color.BLACK);
            g.drawString(scoreText, currentX, baselineY);
            currentX += g.getFontMetrics().stringWidth(scoreText);
            
            // Draw +potential in green
            g.setColor(new Color(0, 150, 0));
            g.setFont(new Font("Consolas", Font.BOLD, fontSize));
            g.drawString(plusText, currentX, baselineY);
            currentX += g.getFontMetrics().stringWidth(plusText);
            
            // Draw equals and total
            g.setColor(Color.BLACK);
            g.setFont(new Font("Consolas", Font.BOLD, fontSize));
            g.drawString(equalsText, currentX, baselineY);
        } else {
            // Just show the current score
            g.setColor(Color.BLACK);
            g.drawString("Score = " + currentScore, scoreX, y + 25);
        }
        
        g.setFont(originalFont);
    }

    /**
     * Draws the score display for both players
     */
    public void drawScores(Graphics2D g, int player1Score, int player2Score, int currentPlayer) {
        // Draw player 1 score (top rack area)
        drawScore(g, 720, 35, player1Score, 1, currentPlayer == 1);
        
        // Draw player 2 score (bottom rack area) - adjusted y position
        drawScore(g, 720, 485, player2Score, 2, currentPlayer == 2);
    }

    /**
     * Returns the current potential score without drawing
     */
    public int getCurrentPotentialScore() {
        return calculatePotentialScore();
    }
}