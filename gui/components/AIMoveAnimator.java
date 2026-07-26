package gui.components;

import application.model.Tile;
import gui.panels.Panel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class AIMoveAnimator {
    
    private Panel panel;
    private BoardGridComponent boardGridComponent;
    private TileRackComponent tileRackComponent;
    private JComponent glassPane;
    private JLabel flyingTile;
    private Timer animationTimer;
    
    private int tileIndex;
    private ArrayList<Tile> tilesToPlace;
    private ArrayList<int[]> positionsToPlace;
    
    private Point startPoint;
    private Point endPoint;
    private long startTime;
    private long duration = 400; // 400ms per tile placement
    
    private int currentPlayer;
    
    public AIMoveAnimator(Panel panel, BoardGridComponent boardGridComponent, TileRackComponent tileRackComponent) {
        this.panel = panel;
        this.boardGridComponent = boardGridComponent;
        this.tileRackComponent = tileRackComponent;
    }
    
    public void animateAIMove(ArrayList<Tile> tiles, ArrayList<int[]> positions, int player) {
        if (tiles == null || positions == null || tiles.isEmpty() || positions.isEmpty()) {
            return;
        }
        
        this.tilesToPlace = new ArrayList<>(tiles);
        this.positionsToPlace = new ArrayList<>(positions);
        this.tileIndex = 0;
        this.currentPlayer = player;
        
        // Clear any existing temp positions in the panel
        panel.getTempPositions().clear();
        panel.getTilesSelectedFromRack().clear();
        
        // Get the glass pane from the root pane
        JRootPane rootPane = SwingUtilities.getRootPane(panel);
        if (rootPane == null) return;
        
        glassPane = (JComponent) rootPane.getGlassPane();
        glassPane.setLayout(null);
        glassPane.setVisible(true);
        
        // Start the animation sequence
        animateNextTile();
    }
    
    private void animateNextTile() {
        if (tileIndex >= tilesToPlace.size()) {
            // All tiles placed - now complete the move
            completeAIMove();
            return;
        }
        
        Tile tile = tilesToPlace.get(tileIndex);
        int[] pos = positionsToPlace.get(tileIndex);
        
        // Determine starting position (from the rack area of the current player)
        int rackX, rackY;
        if (currentPlayer == 1) {
            rackX = 720 + (int)(Math.random() * 280);
            rackY = 510 + (int)(Math.random() * 40);
        } else {
            rackX = 720 + (int)(Math.random() * 280);
            rackY = 150 + (int)(Math.random() * 40);
        }
        
        Point start = new Point(rackX, rackY);
        
        // Get the target board cell position
        JButton targetButton = boardGridComponent.getButton(pos[0], pos[1]);
        Point end = SwingUtilities.convertPoint(targetButton.getParent(), targetButton.getLocation(), glassPane);
        end.x += targetButton.getWidth() / 2;
        end.y += targetButton.getHeight() / 2;
        
        startPoint = start;
        endPoint = end;
        startTime = System.currentTimeMillis();
        
        // Create the flying tile label
        String letter = String.valueOf(tile.letter).toUpperCase();
        ImageIcon icon = new ImageIcon("resources/imgs/" + letter + ".png");
        if (icon.getImage() == null) {
            flyingTile = new JLabel(letter);
            flyingTile.setFont(new Font("Arial", Font.BOLD, 30));
            flyingTile.setForeground(Color.BLACK);
            flyingTile.setOpaque(true);
            flyingTile.setBackground(new Color(242, 191, 118));
            flyingTile.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        } else {
            Image image = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            flyingTile = new JLabel(new ImageIcon(image));
        }
        
        flyingTile.setSize(40, 40);
        flyingTile.setLocation(startPoint.x - 20, startPoint.y - 20);
        glassPane.add(flyingTile);
        glassPane.repaint();
        
        // Start the animation timer for this tile
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        
        animationTimer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateTilePosition();
            }
        });
        animationTimer.start();
    }
    
    private void updateTilePosition() {
        long elapsed = System.currentTimeMillis() - startTime;
        float progress = Math.min(1.0f, (float) elapsed / duration);
        
        // Easing function - easeInOut for smoother movement
        float easedProgress = easeInOut(progress);
        
        int x = (int) (startPoint.x + (endPoint.x - startPoint.x) * easedProgress) - 20;
        int y = (int) (startPoint.y + (endPoint.y - startPoint.y) * easedProgress) - 20;
        
        // Add a slight arc to the movement (bounce effect)
        int arcHeight = (int) (80 * Math.sin(Math.PI * progress));
        y -= arcHeight;
        
        flyingTile.setLocation(x, y);
        glassPane.repaint();
        
        if (progress >= 1.0f) {
            animationTimer.stop();
            
            // Remove the flying tile
            glassPane.remove(flyingTile);
            glassPane.repaint();
            
            // Place the tile on the board (add to temp positions)
            placeTileOnBoard(tilesToPlace.get(tileIndex), positionsToPlace.get(tileIndex));
            
            // Move to next tile
            tileIndex++;
            
            // Small delay before next tile starts
            Timer delayTimer = new Timer(120, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    animateNextTile();
                }
            });
            delayTimer.setRepeats(false);
            delayTimer.start();
        }
    }
    
    private float easeInOut(float t) {
        return t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t;
    }
    
    private void placeTileOnBoard(Tile tile, int[] pos) {
        int r = pos[0];
        int c = pos[1];
        
        // Remove from the player's rack
        ArrayList<Tile> rack = (currentPlayer == 1) ? panel.getTiles_present_player1() : panel.getTiles_present_player2();
        for (int i = 0; i < rack.size(); i++) {
            if (rack.get(i).letter == tile.letter) {
                rack.remove(i);
                break;
            }
        }
        
        // Add to panel's temp positions (this is what triggers the green rectangles)
        panel.getTempPositions().add(new int[]{r, c});
        panel.getTilesSelectedFromRack().add(tile);
        
        // Update the UI
        JButton button = boardGridComponent.getButton(r, c);
        String letter = String.valueOf(tile.letter).toUpperCase();
        ImageIcon icon = new ImageIcon("resources/imgs/" + letter + ".png");
        if (icon.getImage() == null) {
            button.setText(letter);
            button.setForeground(Color.BLACK);
        } else {
            Image image = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            button.setIcon(new ImageIcon(image));
        }
        button.setBackground(new Color(242, 191, 118));
        button.repaint();
        
        // Update the rack display
        panel.tile_rack_rearrange();
        
        // Update the score display with potential score
        panel.refresh();
        
        // Recompute word spans to show green rectangles
        panel.recomputeWordSpans();
    }
    
    private void completeAIMove() {
        // Clean up glass pane
        if (glassPane != null) {
            glassPane.setVisible(false);
        }
        
        // Get the current temp positions and tiles
        ArrayList<int[]> tempPositions = panel.getTempPositions();
        ArrayList<Tile> tempTiles = panel.getTilesSelectedFromRack();
        
        if (tempTiles.isEmpty() || tempPositions.isEmpty()) {
            panel.setAIAnimating(false);
            panel.change_turn();
            panel.refresh();
            return;
        }
        
        // Convert positions to encrypted format
        ArrayList<Integer> encryptedPositions = new ArrayList<>();
        for (int[] p : tempPositions) {
            encryptedPositions.add((p[0] * 15) + p[1] + 1);
        }
        
        // Use the board's placeWord method - this handles all the board state updates
        boolean moveSuccess = panel.board.placeWord(tempTiles, encryptedPositions, currentPlayer);
        
        if (moveSuccess) {
            // Update scores
            panel.update_score(currentPlayer);
            
            // Refill rack
            panel.tileBag.rack_update(currentPlayer);
            panel.tile_rack_rearrange();
            
            // Clear temp positions (the board now has the tiles permanently)
            panel.getTempPositions().clear();
            panel.getTilesSelectedFromRack().clear();
            
            // Update the board display (including special tiles)
            panel.AI_tileSetter();
            
            // Write data
            panel.board.writeData();
            
            // Change turn
            panel.change_turn();
            
            // Final refresh
            panel.refresh();
        } else {
            // If placement failed, revert everything
            System.out.println("AI move placement failed - reverting");
            panel.Reset_Tiles(currentPlayer);
            panel.refresh();
        }
        
        // Clear AI state
        panel.board.potentialMove = null;
        panel.board.AI.legalMoves.clear();
        panel.board.AI.legalMoves_Trans.clear();
        panel.board.AI.hor_positions.clear();
        panel.board.AI.ver_positions.clear();
        panel.tileBag.remaining_tiles();
        
        panel.setAIAnimating(false);
    }
    
    public boolean isAnimating() {
        return animationTimer != null && animationTimer.isRunning() || (tilesToPlace != null && tileIndex < tilesToPlace.size());
    }
}