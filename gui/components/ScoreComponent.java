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

    private static final Font PLAYER_NAME_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font SCORE_FONT = new Font("Segoe UI", Font.BOLD, 32);
    private static final Font POTENTIAL_SCORE_FONT = new Font("Segoe UI", Font.BOLD, 20);

    private static final Color PANEL_BG = new Color(245, 242, 235);
    private static final Color PANEL_BORDER = new Color(200, 190, 180);
    private static final Color SCORE_COLOR = new Color(40, 40, 40);
    private static final Color TURN_BORDER_COLOR = new Color(46, 204, 113);
    private static final Color POTENTIAL_COLOR = new Color(46, 204, 113);

    public ScoreComponent(Board board, BoardCell[][] refBoard) {
        this.board = board;
        this.refBoard = refBoard;
        this.tempPositions = new ArrayList<>();
        this.tilesSelectedFromRack = new ArrayList<>();
    }

    public void setTempData(java.util.List<int[]> tempPositions, java.util.List<Tile> tilesSelectedFromRack) {
        this.tempPositions = tempPositions;
        this.tilesSelectedFromRack = tilesSelectedFromRack;
    }

    /**
     * Checks if a cell is adjacent to an existing occupied tile on the board.
     */
    private boolean isCellAdjacentToExistingTile(int row, int col) {
        if (board.isEmpty()) {
            return row == 7 && col == 7;
        }
        if (refBoard[row][col].isOccupied) {
            return true;
        }
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            if (newRow >= 0 && newRow < 15 && newCol >= 0 && newCol < 15) {
                if (refBoard[newRow][newCol].isOccupied) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if a word span is connected to existing tiles.
     */
    private boolean isSpanConnected(int r1, int c1, int r2, int c2) {
        if (r1 == r2) {
            for (int c = c1; c <= c2; c++) {
                if (isCellAdjacentToExistingTile(r1, c)) {
                    return true;
                }
            }
        } else {
            for (int r = r1; r <= r2; r++) {
                if (isCellAdjacentToExistingTile(r, c1)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks structural validity: tiles on one line with no gaps.
     */
    private boolean isStructurallyValid() {
        if (tempPositions.size() <= 1) return true;

        int r0 = tempPositions.get(0)[0];
        int c0 = tempPositions.get(0)[1];
        boolean rLock = true, cLock = true;
        for (int[] p : tempPositions) {
            if (p[0] != r0) rLock = false;
            if (p[1] != c0) cLock = false;
        }

        if (!rLock && !cLock) return false;

        if (rLock) {
            int cMin = c0, cMax = c0;
            for (int[] p : tempPositions) {
                cMin = Math.min(cMin, p[1]);
                cMax = Math.max(cMax, p[1]);
            }
            for (int c = cMin; c <= cMax; c++) {
                if (!isTempPositionAt(r0, c) && !refBoard[r0][c].isOccupied) {
                    return false;
                }
            }
        } else {
            int rMin = r0, rMax = r0;
            for (int[] p : tempPositions) {
                rMin = Math.min(rMin, p[0]);
                rMax = Math.max(rMax, p[0]);
            }
            for (int r = rMin; r <= rMax; r++) {
                if (!isTempPositionAt(r, c0) && !refBoard[r][c0].isOccupied) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isTempPositionAt(int r, int c) {
        for (int[] p : tempPositions) {
            if (p[0] == r && p[1] == c) return true;
        }
        return false;
    }

    /**
     * Checks if there is at least one valid connected word in the current placement.
     */
    private boolean hasValidConnectedWord() {
        if (tempPositions == null || tempPositions.isEmpty()) {
            return false;
        }

        if (!isStructurallyValid()) {
            return false;
        }

        char[][] grid = new char[15][15];
        boolean[][] occ = new boolean[15][15];
        for (int r = 0; r < 15; r++) {
            for (int c = 0; c < 15; c++) {
                if (refBoard[r][c].isOccupied) {
                    grid[r][c] = Character.toUpperCase(refBoard[r][c].tile.letter);
                    occ[r][c] = true;
                }
            }
        }
        for (int i = 0; i < tempPositions.size() && i < tilesSelectedFromRack.size(); i++) {
            int r = tempPositions.get(i)[0];
            int c = tempPositions.get(i)[1];
            grid[r][c] = Character.toUpperCase(tilesSelectedFromRack.get(i).letter);
            occ[r][c] = true;
        }

        Set<String> seen = new HashSet<>();
        for (int[] pos : tempPositions) {
            int r = pos[0], c = pos[1];

            int c1 = c, c2 = c;
            while (c1 - 1 >= 0 && occ[r][c1 - 1]) c1--;
            while (c2 + 1 <= 14 && occ[r][c2 + 1]) c2++;
            if (c2 > c1) {
                String key = "H" + r + "_" + c1 + "_" + c2;
                if (seen.add(key)) {
                    StringBuilder sb = new StringBuilder();
                    for (int cc = c1; cc <= c2; cc++) sb.append(grid[r][cc]);
                    String word = sb.toString();
                    if (word.length() > 1 && board.dictionary.validateWord(word)) {
                        if (isSpanConnected(r, c1, r, c2)) {
                            return true;
                        }
                    }
                }
            }

            int r1 = r, r2 = r;
            while (r1 - 1 >= 0 && occ[r1 - 1][c]) r1--;
            while (r2 + 1 <= 14 && occ[r2 + 1][c]) r2++;
            if (r2 > r1) {
                String key = "V" + c + "_" + r1 + "_" + r2;
                if (seen.add(key)) {
                    StringBuilder sb = new StringBuilder();
                    for (int rr = r1; rr <= r2; rr++) sb.append(grid[rr][c]);
                    String word = sb.toString();
                    if (word.length() > 1 && board.dictionary.validateWord(word)) {
                        if (isSpanConnected(r1, c, r2, c)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private int calculatePotentialScore() {
        if (tempPositions == null || tempPositions.isEmpty() || 
            tilesSelectedFromRack == null || tilesSelectedFromRack.isEmpty()) {
            return 0;
        }

        // Only calculate score if there is at least one valid connected word
        if (!hasValidConnectedWord()) {
            return 0;
        }
        
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
        
        for (int i = 0; i < tempPositions.size(); i++) {
            int r = tempPositions.get(i)[0];
            int c = tempPositions.get(i)[1];
            tempBoard[r][c].setTileOnCell(tilesSelectedFromRack.get(i));
        }
        
        ArrayList<ArrayList<Integer>> allWordsOnTheMove = new ArrayList<>();
        ArrayList<String> allWordStrings = new ArrayList<>();
        Set<String> seenWords = new HashSet<>();
        
        for (int[] pos : tempPositions) {
            int r = pos[0];
            int c = pos[1];
            
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
                // Only include valid words that are connected
                if (wordStr.length() > 1 && board.dictionary.validateWord(wordStr) && 
                    isSpanConnected(r, cStart, r, cEnd) && seenWords.add(key)) {
                    allWordsOnTheMove.add(wordPos);
                    allWordStrings.add(wordStr);
                }
            }
            
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
                if (wordStr.length() > 1 && board.dictionary.validateWord(wordStr) && 
                    isSpanConnected(rStart, c, rEnd, c) && seenWords.add(key)) {
                    allWordsOnTheMove.add(wordPos);
                    allWordStrings.add(wordStr);
                }
            }
        }
        
        if (allWordsOnTheMove.isEmpty()) {
            return 0;
        }
        
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
                
                int letterScore = tileValue;
                
                if (tempBoard[r][c].speciality == 1) {
                    if (!spVisited[r][c]) {
                        letterScore = tileValue * 2;
                        spVisited[r][c] = true;
                    }
                } else if (tempBoard[r][c].speciality == 2) {
                    if (!spVisited[r][c]) {
                        letterScore = tileValue * 3;
                        spVisited[r][c] = true;
                    }
                } else if (tempBoard[r][c].speciality == 3) {
                    if (!spVisited[r][c]) {
                        dw = true;
                        spVisited[r][c] = true;
                    }
                } else if (tempBoard[r][c].speciality == 4) {
                    if (!spVisited[r][c]) {
                        tw = true;
                        spVisited[r][c] = true;
                    }
                }
                
                wordScore += letterScore;
            }
            
            if (tw) wordScore *= 3;
            if (dw) wordScore *= 2;
            
            score += wordScore;
        }
        
        if (bingo) score += 50;
        return score;
    }

    public void drawScores(Graphics2D g, int player1Score, int player2Score, int currentPlayer) {
        int panelWidth = 200;
        int panelHeight = 75;
        int gap = 20;
        int startX = 720;
        int y = 15;
        
        // Draw Player 1 (left)
        drawPlayerScore(g, startX, y, panelWidth, panelHeight, 
                       "Player 1", player1Score, currentPlayer == 1);
        
        // Draw Player 2 (right)
        drawPlayerScore(g, startX + panelWidth + gap, y, panelWidth, panelHeight,
                       "Player 2", player2Score, currentPlayer == 2);
    }

    private void drawPlayerScore(Graphics2D g, int x, int y, int width, int height, 
                                  String playerName, int score, boolean isCurrentTurn) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Shadow
        g.setColor(new Color(0, 0, 0, 25));
        g.fillRoundRect(x + 3, y + 3, width, height, 12, 12);
        
        // Main panel background
        g.setColor(PANEL_BG);
        g.fillRoundRect(x, y, width, height, 12, 12);
        
        // Border - green if current turn, gray otherwise
        if (isCurrentTurn) {
            g.setColor(TURN_BORDER_COLOR);
            g.setStroke(new BasicStroke(3f));
        } else {
            g.setColor(PANEL_BORDER);
            g.setStroke(new BasicStroke(1.5f));
        }
        g.drawRoundRect(x, y, width, height, 12, 12);
        
        int padding = 15;
        int textX = x + padding;
        
        // Player name
        g.setFont(PLAYER_NAME_FONT);
        g.setColor(Color.BLACK);
        g.drawString(playerName, textX, y + 26);
        
        // Score - large and bold
        int potentialScore = calculatePotentialScore();
        
        // Only show potential score if:
        // 1. It's the current player's turn
        // 2. There are tiles placed (tempPositions not empty)
        // 3. There is at least one valid connected word (potentialScore > 0)
        boolean showPotential = isCurrentTurn && 
                               !tempPositions.isEmpty() && 
                               potentialScore > 0;
        
        if (showPotential) {
            // Draw base score in gray
            g.setFont(SCORE_FONT);
            g.setColor(new Color(150, 150, 150));
            String baseScore = String.valueOf(score);
            g.drawString(baseScore, textX, y + 65);
            
            // Draw "+" and potential in green (closer together)
            g.setFont(POTENTIAL_SCORE_FONT);
            g.setColor(POTENTIAL_COLOR);
            String plusText = "+" + potentialScore;
            int baseWidth = g.getFontMetrics(SCORE_FONT).stringWidth(baseScore);
            g.drawString(plusText, textX + baseWidth + 2, y + 62);
            
            // Draw "=" and total
            g.setFont(SCORE_FONT);
            g.setColor(SCORE_COLOR);
            String totalText = "=" + (score + potentialScore);
            int plusWidth = g.getFontMetrics(POTENTIAL_SCORE_FONT).stringWidth(plusText);
            g.drawString(totalText, textX + baseWidth + plusWidth + 4, y + 65);
        } else {
            g.setFont(SCORE_FONT);
            g.setColor(SCORE_COLOR);
            g.drawString(String.valueOf(score), textX, y + 65);
        }
    }

    public int getCurrentPotentialScore() {
        return calculatePotentialScore();
    }
}