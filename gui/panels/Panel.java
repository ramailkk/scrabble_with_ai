package gui.panels;

import application.model.Board;
import application.model.BoardCell;
import application.model.Tile;
import application.model.TileBag;
import gui.components.ActionToolbarComponent;
import gui.components.BoardGridComponent;
import gui.components.RemainingTilesComponent;
import gui.components.TileRackComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.Timer;

public class Panel extends JPanel implements ActionListener {
    public boolean swap_active = false;
    private int B_WIDTH = 1280;
    private int B_HEIGHT = 720;
    public Board board = new Board();
    public TileBag tileBag = new TileBag();
    BoardCell[][] ref_board;

    private BoardGridComponent boardGridComponent = new BoardGridComponent();
    private TileRackComponent tileRackComponent = new TileRackComponent();
    private ActionToolbarComponent actionToolbarComponent = new ActionToolbarComponent();
    private RemainingTilesComponent remainingTilesComponent = new RemainingTilesComponent();

    private ArrayList<Tile> tiles_present_player1;
    private ArrayList<Tile> tiles_present_player2;
    private int player = 1;
    private Tile current_letter_selected;
    private ArrayList<Integer> current_tile_selected = new ArrayList<>();
    private ArrayList<Tile> tiles_selected_from_rack = new ArrayList<>();
    private ArrayList<int[]> temp_positions = new ArrayList<>();
    private int Player_score_1 = 0;
    private int Player_score_2 = 0;

    private Timer dropCheckTimer;
    private boolean dragDropInProgress = false;

    
    Swap_Panel swap_panel = new Swap_Panel();

    private boolean gameEnd;

    public Panel() {
        ref_board = board.getTheBoard();
        tiles_present_player1 = tileBag.getRack_player_1();
        tiles_present_player2 = tileBag.getRack_player_2();
        swap_panel.setPanel(this);
        initBoard();

        dropCheckTimer = new Timer(100, e -> checkForDrop());
        dropCheckTimer.start();
    }

    private void initBoard() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(B_WIDTH, B_HEIGHT));
        setFocusable(true);
        boardGridComponent.initGrid(this, this, ref_board);
        tileRackComponent.initRacks(this, this, tiles_present_player1, tiles_present_player2);
        actionToolbarComponent.initToolbar(this, this);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boardGridComponent.layoutGrid(graphics2D, 50, 50, 40, 40);
        tileRackComponent.layoutPlayer1(graphics2D, 720, 50, 40, 40, player, Player_score_1);
        tileRackComponent.layoutPlayer2(graphics2D, 720, 500, 40, 40, player, Player_score_2);
        actionToolbarComponent.layoutToolbar(680, 600, 100, 100);
        remainingTilesComponent.drawRemaining(graphics2D, 680, 250, tileBag.getRemaining());
    }

    @Override
    public void paintComponents(Graphics g) {
    }

    public void tile_rack_rearrange() {
        tileRackComponent.rearrange(player, tiles_present_player1, tiles_present_player2);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JButton[][] buttons = boardGridComponent.getButtons();
        for (int i = 0; i < buttons.length; i++) {
            for (int j = 0; j < buttons[0].length; j++) {
                if (e.getSource() == buttons[i][j]) {
                    current_tile_selected.clear();
                    current_tile_selected.add(i);
                    current_tile_selected.add(j);
                }
            }
        }

        JButton[] p1Racks = tileRackComponent.getTileRackPlayer1();
        for (int i = 0; i < tiles_present_player1.size(); i++) {
            if (e.getSource() == p1Racks[i] && player == 1) {
                if (swap_active && swap_panel.getCurrent_selected_array() == tiles_present_player1) {
                    swap_panel.swap_selections.add(tiles_present_player1.remove(i));
                    tile_rack_rearrange();
                    swap_panel.swap_button_rearrange();
                } else if (!swap_active) {
                    current_letter_selected = tiles_present_player1.get(i);
                    tile_setter(i);
                }
            }
        }

        JButton[] p2Racks = tileRackComponent.getTileRackPlayer2();
        for (int i = 0; i < tiles_present_player2.size(); i++) {
            if (e.getSource() == p2Racks[i] && player == 2) {
                if (swap_active && swap_panel.getCurrent_selected_array() == tiles_present_player2) {
                    swap_panel.swap_selections.add(tiles_present_player2.remove(i));
                    tile_rack_rearrange();
                    swap_panel.swap_button_rearrange();
                } else if (!swap_active) {
                    current_letter_selected = tiles_present_player2.get(i);
                    tile_setter(i);
                }
            }
        }

        JButton[] options = actionToolbarComponent.getOptionsButtons();
        if (e.getSource() == options[0] && !swap_active) {
            Reset_Tiles(player);
            System.out.println("Reset");
        } else if (e.getSource() == options[1] && !swap_active) {
            Reset_Tiles(player);
            System.out.println("AI MOVE");

            if (player == 1) board.setAiRack(tileBag.getRack_player_1());
            else board.setAiRack(tileBag.getRack_player_2());

            if (!gameEnd) {
                board.AI_TEST();
                board.transAI_TEST();
                board.AI.writeToFile();
                board.readData("game_results/horiMoves.txt", true);
                board.readData("game_results/vertiMoves.txt", false);
                board.AI_placeWord(player);
                AI_tileSetter();
                update_score(player);
                tileBag.rack_update(player);
                tile_rack_rearrange();

                if (tileBag.getTiles().size() == 0 && (board.potentialMove == null || (tiles_present_player1.size() == 0 || tiles_present_player2.size() == 0))) {
                    gameEnd = true;
                    if (tiles_present_player1.size() == 0 && tiles_present_player2.size() != 0) {
                        for (Tile t : tiles_present_player2) {
                            board.p1Score += t.value;
                        }
                    }

                    if (tiles_present_player2.size() == 0 && tiles_present_player1.size() != 0) {
                        for (Tile t : tiles_present_player1) {
                            board.p2Score += t.value;
                        }
                    }

                    if (tiles_present_player1.size() != 0 && tiles_present_player2.size() != 0) {
                        for (Tile t : tiles_present_player1) {
                            board.p1Score -= t.value;
                        }

                        for (Tile t : tiles_present_player2) {
                            board.p2Score -= t.value;
                        }
                    }

                    board.stuff += "GAME ENDED" + "\n----------\nFINAL SCORE: \nPlayer1: " + board.p1Score + "\nPlayer2: " + board.p2Score + "\n";
                    tileBag.getTiles().addAll(tiles_present_player1);
                    tileBag.getTiles().addAll(tiles_present_player2);
                    tiles_present_player2.clear();
                    tiles_present_player1.clear();
                    tile_rack_rearrange();
                    refresh();
                }
                board.writeData();
            }

            board.potentialMove = null;
            board.AI.legalMoves.clear();
            board.AI.legalMoves_Trans.clear();
            board.AI.hor_positions.clear();
            board.AI.ver_positions.clear();
            tileBag.remaining_tiles();
            SwingUtilities.updateComponentTreeUI(this);
            change_turn();

        } else if (e.getSource() == options[2] && !swap_active) {
            System.out.println("Skip");
            Reset_Tiles(player);
            change_turn();
            refresh();
        } else if (e.getSource() == options[3] && !swap_active) {
            if (!gameEnd) {
                ArrayList<Integer> pos = new ArrayList<>();
                for (int i = 0; i < temp_positions.size(); i++) {
                    pos.add((temp_positions.get(i)[0] * 15) + temp_positions.get(i)[1] + 1);
                }

                if (board.placeWord(tiles_selected_from_rack, pos, player)) {
                    update_score(player);
                    tileBag.rack_update(player);
                    tile_rack_rearrange();
                    change_turn();
                    board.writeData();
                    SwingUtilities.updateComponentTreeUI(this);
                } else {
                    Reset_Tiles(player);
                    pos.clear();
                }
            }
        } else if (e.getSource() == options[4] && !swap_active) {
            Reset_Tiles(player);
            System.out.println("Swap");
            swap_active = true;
            System.out.println(player);
            if (player == 1) {
                swap_panel.setCurrent_selected_array(getTiles_present_player1());
            } else {
                swap_panel.setCurrent_selected_array(getTiles_present_player2());
            }
            gui.frames.Swap_Frame swap_frame = new gui.frames.Swap_Frame(swap_panel);
        } else if (e.getSource() == options[5] && !swap_active) {
            Reset_Tiles(player);
            System.out.println("Resign");
            gameEnd = true;
            if (tiles_present_player1.size() == 0 && tiles_present_player2.size() != 0) {
                for (Tile t : tiles_present_player2) {
                    board.p1Score += t.value;
                }
            }

            if (tiles_present_player2.size() == 0 && tiles_present_player1.size() != 0) {
                for (Tile t : tiles_present_player1) {
                    board.p2Score += t.value;
                }
            }

            if (tiles_present_player1.size() != 0 && tiles_present_player2.size() != 0) {
                for (Tile t : tiles_present_player1) {
                    board.p1Score -= t.value;
                }

                for (Tile t : tiles_present_player2) {
                    board.p2Score -= t.value;
                }
            }
            board.stuff += "GAME ENDED" + "\n----------\nFINAL SCORE: \nPlayer1: " + board.p1Score + "\nPlayer2: " + board.p2Score + "\n";
            board.writeData();

            tileBag.getTiles().addAll(tiles_present_player1);
            tileBag.getTiles().addAll(tiles_present_player2);
            tiles_present_player2.clear();
            tiles_present_player1.clear();
            tile_rack_rearrange();
            refresh();
        }

        JButton[] shuffles = tileRackComponent.getShuffleButtons();
        if (e.getSource() == shuffles[0] && player == 1) {
            Collections.shuffle(tiles_present_player1);
            tile_rack_rearrange();
            refresh();
        } else if (e.getSource() == shuffles[1] && player == 2) {
            Collections.shuffle(tiles_present_player2);
            tile_rack_rearrange();
            refresh();
        }
    }

    public void update_score(int player) {
        if (player == 1) {
            Player_score_1 = board.p1Score;
        } else {
            Player_score_2 = board.p2Score;
        }
    }

    public void placeWordMessage() {
        ArrayList<Integer> pos = new ArrayList<>();
        for (int i = 0; i < temp_positions.size(); i++) {
            pos.add((temp_positions.get(i)[0] * 15) + temp_positions.get(i)[1]);
        }
        if (board.placeWord(tiles_selected_from_rack, pos, player)) {
            tileBag.rack_update(player);
            change_turn();
        }
    }

    private void tile_setter(int i) {
        if (current_letter_selected != null && !current_tile_selected.isEmpty()) {
            if (!boardGridComponent.isFreeTile(ref_board, current_tile_selected.get(0), current_tile_selected.get(1))) {
                current_tile_selected.clear();
                current_letter_selected = null;
                return;
            }

            ImageIcon icon = new ImageIcon("resources/imgs/" + String.valueOf(current_letter_selected.letter) + ".png");
            Image image = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            icon = new ImageIcon(image);
            boardGridComponent.getButton(current_tile_selected.get(0), current_tile_selected.get(1)).setIcon(icon);

            if (player == 1) {
                tiles_present_player1.remove(current_letter_selected);
                tiles_selected_from_rack.add(current_letter_selected);
            } else {
                tiles_present_player2.remove(current_letter_selected);
                tiles_selected_from_rack.add(current_letter_selected);
            }
            tile_rack_rearrange();
            int[] rc = {current_tile_selected.get(0), current_tile_selected.get(1)};
            if (!boardGridComponent.isSpecialTile(ref_board, current_tile_selected.get(0), current_tile_selected.get(1))) {
                boardGridComponent.getButton(current_tile_selected.get(0), current_tile_selected.get(1)).setBackground(new Color(242, 191, 118));
            }
            temp_positions.add(rc);
            current_letter_selected = null;
            current_tile_selected.clear();
        }
    }

    public void AI_tileSetter() {
        boardGridComponent.AI_tileSetter(ref_board);
    }

    private void Reset_Tiles(int player) {
        if (player == 1) {
            tiles_present_player1.addAll(tiles_selected_from_rack);
        } else {
            tiles_present_player2.addAll(tiles_selected_from_rack);
        }
        boardGridComponent.resetTiles(temp_positions, ref_board);
        temp_positions.clear();
        tile_rack_rearrange();
        tiles_selected_from_rack.clear();
    }

    public void refresh() {
        SwingUtilities.updateComponentTreeUI(this);
    }

    public void change_turn() {
        if (player == 1) {
            player = 2;
        } else {
            player = 1;
        }

        temp_positions.clear();
        tiles_selected_from_rack.clear();
        current_letter_selected = null;
        current_tile_selected.clear();
    }

    public ArrayList<Tile> getTiles_present_player1() {
        return tiles_present_player1;
    }

    public ArrayList<Tile> getTiles_present_player2() {
        return tiles_present_player2;
    }

    // Add this method to check for drops
private void checkForDrop() {
    if (boardGridComponent.getLastDropInfo() != null) {
        BoardGridComponent.DropInfo dropInfo = boardGridComponent.getLastDropInfo();
        boardGridComponent.clearLastDropInfo();
        
        int row = dropInfo.row;
        int col = dropInfo.col;
        int player = dropInfo.player;
        int tileIndex = dropInfo.tileIndex;
        
        // Check if the player matches the current turn
        if (player != this.player) {
            JOptionPane.showMessageDialog(this, "It's not your turn!", "Invalid Move", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Check if the tile is already placed
        if (!boardGridComponent.isFreeTile(ref_board, row, col)) {
            JOptionPane.showMessageDialog(this, "This square is already occupied!", "Invalid Move", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get the tile from the appropriate rack
        Tile tileToPlace = null;
        if (player == 1 && tileIndex < tiles_present_player1.size()) {
            tileToPlace = tiles_present_player1.get(tileIndex);
        } else if (player == 2 && tileIndex < tiles_present_player2.size()) {
            tileToPlace = tiles_present_player2.get(tileIndex);
        }
        
        if (tileToPlace != null) {
            // Place the tile on the board
            current_tile_selected.clear();
            current_tile_selected.add(row);
            current_tile_selected.add(col);
            current_letter_selected = tileToPlace;
            
            // Use the existing tile_setter logic
            tile_setter_drag(tileIndex, player);
        }
    }
}

// Add this method for drag placement
private void tile_setter_drag(int index, int player) {
    if (current_letter_selected != null && !current_tile_selected.isEmpty()) {
        int row = current_tile_selected.get(0);
        int col = current_tile_selected.get(1);
        
        if (!boardGridComponent.isFreeTile(ref_board, row, col)) {
            current_tile_selected.clear();
            current_letter_selected = null;
            return;
        }

        // Place the tile on the board visually
        ImageIcon icon = new ImageIcon("resources/imgs/" + String.valueOf(current_letter_selected.letter).toUpperCase() + ".png");
        Image image = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        icon = new ImageIcon(image);
        boardGridComponent.getButton(row, col).setIcon(icon);

        // Remove from rack
        if (player == 1) {
            tiles_present_player1.remove(current_letter_selected);
        } else {
            tiles_present_player2.remove(current_letter_selected);
        }
        
        tiles_selected_from_rack.add(current_letter_selected);
        tile_rack_rearrange();
        
        int[] rc = {row, col};
        if (!boardGridComponent.isSpecialTile(ref_board, row, col)) {
            boardGridComponent.getButton(row, col).setBackground(new Color(242, 191, 118));
        }
        temp_positions.add(rc);
        
        current_letter_selected = null;
        current_tile_selected.clear();
        
        refresh();
    }
}
}
