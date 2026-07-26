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

    private int calculatePotentialScore() {
        if (tempPositions == null || tempPositions.isEmpty() || 
            tilesSelectedFromRack == null || tilesSelectedFromRack.isEmpty()) {
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
                if (wordStr.length() > 1 && board.dictionary.validateWord(wordStr) && seenWords.add(key)) {
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
                if (wordStr.length() > 1 && board.dictionary.validateWord(wordStr) && seenWords.add(key)) {
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
        
        if (potentialScore > 0 && isCurrentTurn && !tempPositions.isEmpty()) {
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