package gui.panels;

import application.model.Board;
import application.model.BoardCell;
import application.model.Tile;
import application.model.TileBag;
import gui.components.ActionToolbarComponent;
import gui.components.BoardGridComponent;
import gui.components.RemainingTilesComponent;
import gui.components.TileRackComponent;
import gui.components.ScoreComponent;  // Add this import

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class Panel extends JPanel implements ActionListener {
    public boolean swap_active = false;
    private int B_WIDTH = 1280;
    private int B_HEIGHT = 720;
    public Board board = new Board();
    public TileBag tileBag = new TileBag();
    BoardCell[][] ref_board;
    private boolean needsRepaint = false;
    
    private javax.swing.Timer coalescedRepaintTimer;


    private BoardGridComponent boardGridComponent = new BoardGridComponent();
    private TileRackComponent tileRackComponent = new TileRackComponent();
    private ActionToolbarComponent actionToolbarComponent = new ActionToolbarComponent();
    private RemainingTilesComponent remainingTilesComponent = new RemainingTilesComponent();
    private ScoreComponent scoreComponent;  // Add this

    private ArrayList<Tile> tiles_present_player1;
    private ArrayList<Tile> tiles_present_player2;
    private int player = 1;
    private Tile current_letter_selected;
    private ArrayList<Integer> current_tile_selected = new ArrayList<>();
    private ArrayList<Tile> tiles_selected_from_rack = new ArrayList<>();
    private ArrayList<int[]> temp_positions = new ArrayList<>();
    private int Player_score_1 = 0;
    private int Player_score_2 = 0;

    Swap_Panel swap_panel = new Swap_Panel();

    private boolean gameEnd;

    /**
     * Pluggable word-validity check, used to color the rings drawn around
     * words formed by this turn's tile placements. Defaults to the board's
     * actual dictionary (Board.dictionary is a MyTrie), so this works out
     * of the box; call setWordValidator(...) if you ever want to swap the
     * check for something else (e.g. during testing).
     */
    private WordValidator wordValidator = word -> board.dictionary.validateWord(word);

    public interface WordValidator {
        boolean isValid(String word);
    }

    public void setWordValidator(WordValidator wordValidator) {
        this.wordValidator = wordValidator;
    }

    /** A single word formed on the board, expressed as its cell span. */
    private static class WordSpan {
        final int r1, c1, r2, c2; // inclusive endpoints; same row (horizontal) or same col (vertical)
        final String word;
        final boolean valid;

        WordSpan(int r1, int c1, int r2, int c2, String word, boolean valid) {
            this.r1 = r1;
            this.c1 = c1;
            this.r2 = r2;
            this.c2 = c2;
            this.word = word;
            this.valid = valid;
        }
    }

    // Cached so paint doesn't recompute word validity 60x/sec -- only ever
    // rebuilt when temp_positions/tiles_selected_from_rack actually change
    // (see recomputeWordSpans()).
    private java.util.List<WordSpan> cachedWordSpans = new ArrayList<>();

    // A small, dedicated always-on-top layer that only draws the cached
    // rings. Living in its own JLayeredPane layer (rather than being drawn
    // directly by Panel.paintComponent) means Swing correctly composites it
    // whenever a board button underneath repaints itself (e.g. on hover) --
    // ordinary overlapping siblings in a plain JPanel aren't guaranteed
    // that, but layers in a JLayeredPane are. It only ever redraws the
    // cached list, so its content never changes just because *something*
    // triggered a repaint.
    private JLayeredPane boardLayeredPane;
    private JComponent ringOverlay;

    public Panel() {
        ref_board = board.getTheBoard();
        tiles_present_player1 = tileBag.getRack_player_1();
        tiles_present_player2 = tileBag.getRack_player_2();
        swap_panel.setPanel(this);

        // Initialize the score component
        scoreComponent = new ScoreComponent(board, ref_board);

        initBoard();
        coalescedRepaintTimer = new javax.swing.Timer(16, e -> {
            if (needsRepaint) {
                needsRepaint = false;
                repaint();
            }
        });
    }

    private void initBoard() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(B_WIDTH, B_HEIGHT));
        setFocusable(true);
        // Without this, JPanel's default FlowLayout would fight our manual
        // setBounds() calls (e.g. whenever revalidate()/updateComponentTreeUI
        // runs), which is why bounds used to have to be reasserted on every
        // single paint. With it, bounds set once simply stick.
        setLayout(null);

        // Board buttons live in their own layered pane so the ring overlay
        // (added above them, in a higher layer) composites correctly.
        boardLayeredPane = new JLayeredPane();
        boardLayeredPane.setBounds(0, 0, B_WIDTH, B_HEIGHT);
        boardLayeredPane.setOpaque(false);
        boardLayeredPane.setBackground(Color.WHITE); // RoundedButton blends corners with parent.getBackground()
        add(boardLayeredPane);

        boardGridComponent.initGrid(this, boardLayeredPane, ref_board);
        tileRackComponent.initRacks(this, this, tiles_present_player1, tiles_present_player2);
        actionToolbarComponent.initToolbar(this, this);

        ringOverlay = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                drawWordRings((Graphics2D) g);
            }
        };
        ringOverlay.setOpaque(false);
        ringOverlay.setBounds(0, 0, B_WIDTH, B_HEIGHT);
        boardLayeredPane.add(ringOverlay, JLayeredPane.PALETTE_LAYER);

        // Bounds never change mid-game -- set them once instead of on every paint.
        boardGridComponent.layoutBoundsOnce(50, 50, 40, 40);
        tileRackComponent.layoutPlayer1Once(720, 50, 40, 40);
        tileRackComponent.layoutPlayer2Once(720, 500, 40, 40);
        actionToolbarComponent.layoutToolbarOnce(680, 600, 100, 100);

        // Wire up drag-and-drop: rack tiles report drops directly to this panel.
        tileRackComponent.setBoardGridComponent(boardGridComponent);
        tileRackComponent.setDropListener(this::handleTileDrop);

        // Wire up dragging tiles that are already on the board (but only
        // this turn's not-yet-submitted ones) to another cell or back to the rack.
        boardGridComponent.setDragListener(new BoardGridComponent.BoardTileDragListener() {
            @Override
            public boolean isMovable(int row, int col) {
                return findTempIndexAt(row, col) != -1;
            }

            @Override
            public void onBoardTileMoved(int fromRow, int fromCol, int toRow, int toCol) {
                handleBoardTileMoved(fromRow, fromCol, toRow, toCol);
            }

            @Override
            public void onBoardTileReturnedToRack(int fromRow, int fromCol) {
                handleBoardTileReturnedToRack(fromRow, fromCol);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boardGridComponent.drawGridLabels(graphics2D, 50, 50, 40, 40);

        // Update score component with current temp data
        scoreComponent.setTempData(temp_positions, tiles_selected_from_rack);
        scoreComponent.drawScores(graphics2D, Player_score_1, Player_score_2, player);

        remainingTilesComponent.drawRemaining(graphics2D, 680, 250, tileBag.getRemaining());
    }

    /**
     * Rebuilds the cached word spans from the current temp_positions /
     * tiles_selected_from_rack and repaints just the ring overlay (not the
     * whole panel). Call this any time those two collections change.
     */
    private void recomputeWordSpans() {
        cachedWordSpans = computeWordSpans();
        if (ringOverlay != null) {
            ringOverlay.repaint();
        }
    }

    /**
     * Draws the cached word spans -- green if a valid dictionary word, red
     * otherwise. This is the ring overlay's paintComponent body: it only
     * ever draws cachedWordSpans, so repeated/incidental repaints (e.g. a
     * board button's hover repaint triggering the overlay to redraw for
     * proper compositing) always render the exact same rings, never fading
     * or changing them.
     */
    private void drawWordRings(Graphics2D g) {
        if (cachedWordSpans.isEmpty()) return;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke(3f));
        int margin = 4;
        int arc = 16;

        for (WordSpan span : cachedWordSpans) {
            Rectangle start = boardGridComponent.getButton(span.r1, span.c1).getBounds();
            Rectangle end = boardGridComponent.getButton(span.r2, span.c2).getBounds();

            int x = start.x - margin;
            int y = start.y - margin;
            int w = (end.x + end.width) - start.x + margin * 2;
            int h = (end.y + end.height) - start.y + margin * 2;

            g.setColor(span.valid ? new Color(0, 170, 0) : new Color(220, 50, 50));
            g.drawRoundRect(x, y, w, h, arc, arc);
        }

        g.setStroke(oldStroke);
    }

    /**
     * Builds the board+in-progress-move as one combined letter grid, then
     * for every tile placed this turn walks outward in both directions to
     * find the horizontal word and the vertical word through it (a tile
     * can be part of both -- e.g. it extends the main word and also forms
     * a new cross word with an existing tile). Each distinct span (by
     * orientation + row/col + start/end) is only reported once, even if
     * multiple placed tiles belong to the same word.
     */
    private java.util.List<WordSpan> computeWordSpans() {
        java.util.List<WordSpan> spans = new ArrayList<>();
        if (temp_positions.isEmpty()) return spans;

        char[][] grid = new char[15][15];
        boolean[][] occ = new boolean[15][15];
        for (int r = 0; r < 15; r++) {
            for (int c = 0; c < 15; c++) {
                if (ref_board[r][c].isOccupied) {
                    grid[r][c] = Character.toUpperCase(ref_board[r][c].tile.letter);
                    occ[r][c] = true;
                }
            }
        }
        for (int i = 0; i < temp_positions.size() && i < tiles_selected_from_rack.size(); i++) {
            int r = temp_positions.get(i)[0];
            int c = temp_positions.get(i)[1];
            grid[r][c] = Character.toUpperCase(tiles_selected_from_rack.get(i).letter);
            occ[r][c] = true;
        }

        Set<String> seen = new HashSet<>();
        for (int[] pos : temp_positions) {
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
                    spans.add(new WordSpan(r, c1, r, c2, word, wordValidator.isValid(word)));
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
                    spans.add(new WordSpan(r1, c, r2, c, word, wordValidator.isValid(word)));
                }
            }
        }
        return spans;
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

    /**
     * True only if the cell is free on the committed board AND isn't
     * already occupied by a tile placed earlier this same turn (which
     * boardGridComponent.isFreeTile alone can't see, since temp placements
     * aren't written into ref_board until the move is submitted).
     */
    private boolean isCellAvailable(int r, int c) {
        if (!boardGridComponent.isFreeTile(ref_board, r, c)) return false;
        for (int[] pos : temp_positions) {
            if (pos[0] == r && pos[1] == c) return false;
        }
        return true;
    }

    private void tile_setter(int i) {
    if (current_letter_selected != null && !current_tile_selected.isEmpty()) {
        if (!isCellAvailable(current_tile_selected.get(0), current_tile_selected.get(1))) {
            current_tile_selected.clear();
            current_letter_selected = null;
            return;
        }

        // Use a local variable for the button to avoid repeated lookups
        JButton targetButton = boardGridComponent.getButton(
            current_tile_selected.get(0), 
            current_tile_selected.get(1)
        );
        
        ImageIcon icon = new ImageIcon("resources/imgs/" + 
            String.valueOf(current_letter_selected.letter).toUpperCase() + ".png");
        Image image = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        icon = new ImageIcon(image);
        targetButton.setIcon(icon);

        if (player == 1) {
            tiles_present_player1.remove(current_letter_selected);
            tiles_selected_from_rack.add(current_letter_selected);
        } else {
            tiles_present_player2.remove(current_letter_selected);
            tiles_selected_from_rack.add(current_letter_selected);
        }
        
        // Only update rack buttons that changed
        tileRackComponent.rearrange(player, tiles_present_player1, tiles_present_player2);
        
        int[] rc = {current_tile_selected.get(0), current_tile_selected.get(1)};
        if (!boardGridComponent.isSpecialTile(ref_board, current_tile_selected.get(0), current_tile_selected.get(1))) {
            targetButton.setBackground(new Color(242, 191, 118));
        }
        temp_positions.add(rc);
        current_letter_selected = null;
        current_tile_selected.clear();
        recomputeWordSpans();

        // Repaint only the affected areas
        targetButton.repaint();
        refresh();
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
        recomputeWordSpans();
        refresh();
    }

    public void refresh() {
         needsRepaint = true;
        coalescedRepaintTimer.restart();
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
        recomputeWordSpans();

        // Only repaint if the panel is visible
        if (isVisible()) {
            needsRepaint = true;
            coalescedRepaintTimer.restart();
        }
    }

    public ArrayList<Tile> getTiles_present_player1() {
        return tiles_present_player1;
    }

    public ArrayList<Tile> getTiles_present_player2() {
        return tiles_present_player2;
    }
    

    /**
     * Called directly (no polling) by TileRackComponent when a rack tile is
     * dropped onto a board cell.
     */
    private void handleTileDrop(int dropPlayer, int tileIndex, int row, int col) {
        if (dropPlayer != this.player) {
            JOptionPane.showMessageDialog(this, "It's not your turn!", "Invalid Move", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!isCellAvailable(row, col)) {
            JOptionPane.showMessageDialog(this, "This square is already occupied!", "Invalid Move", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ArrayList<Tile> rack = (dropPlayer == 1) ? tiles_present_player1 : tiles_present_player2;
        if (tileIndex < 0 || tileIndex >= rack.size()) {
            return;
        }
        Tile tileToPlace = rack.get(tileIndex);

        ImageIcon icon = new ImageIcon("resources/imgs/" + String.valueOf(tileToPlace.letter).toUpperCase() + ".png");
        Image image = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        icon = new ImageIcon(image);
        boardGridComponent.getButton(row, col).setIcon(icon);

        rack.remove(tileToPlace);
        tiles_selected_from_rack.add(tileToPlace);
        tile_rack_rearrange();

        if (!boardGridComponent.isSpecialTile(ref_board, row, col)) {
            boardGridComponent.getButton(row, col).setBackground(new Color(242, 191, 118));
        }
        temp_positions.add(new int[]{row, col});
        recomputeWordSpans();

        refresh();
    }

    /** Index into temp_positions/tiles_selected_from_rack for the cell (r, c) this turn, or -1 if it's not one of this turn's placements. */
    private int findTempIndexAt(int r, int c) {
        for (int i = 0; i < temp_positions.size(); i++) {
            if (temp_positions.get(i)[0] == r && temp_positions.get(i)[1] == c) return i;
        }
        return -1;
    }

    /**
     * Called by BoardGridComponent when a tile placed this turn is dragged
     * from one board cell to a different, empty one.
     */
    private void handleBoardTileMoved(int fromRow, int fromCol, int toRow, int toCol) {
        int idx = findTempIndexAt(fromRow, fromCol);
        if (idx == -1) return; // shouldn't happen; isMovable already gated this

        if (!isCellAvailable(toRow, toCol)) {
            JOptionPane.showMessageDialog(this, "This square is already occupied!", "Invalid Move", JOptionPane.WARNING_MESSAGE);
            return; // origin cell's icon/background were already restored by BoardGridComponent
        }

        Tile tile = tiles_selected_from_rack.get(idx);

        // Revert the old cell back to its default board appearance.
        boardGridComponent.resetSingleTile(fromRow, fromCol, ref_board);

        // Paint the tile onto its new cell.
        ImageIcon icon = new ImageIcon("resources/imgs/" + String.valueOf(tile.letter).toUpperCase() + ".png");
        Image image = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        icon = new ImageIcon(image);
        boardGridComponent.getButton(toRow, toCol).setIcon(icon);
        if (!boardGridComponent.isSpecialTile(ref_board, toRow, toCol)) {
            boardGridComponent.getButton(toRow, toCol).setBackground(new Color(242, 191, 118));
        }

        temp_positions.set(idx, new int[]{toRow, toCol});
        recomputeWordSpans();

        refresh();
    }

    /**
     * Called by BoardGridComponent when a tile placed this turn is dragged
     * off the board entirely -- sends it back to the owning player's rack.
     */
    private void handleBoardTileReturnedToRack(int fromRow, int fromCol) {
        int idx = findTempIndexAt(fromRow, fromCol);
        if (idx == -1) return;

        Tile tile = tiles_selected_from_rack.remove(idx);
        temp_positions.remove(idx);

        if (player == 1) {
            tiles_present_player1.add(tile);
        } else {
            tiles_present_player2.add(tile);
        }

        boardGridComponent.resetSingleTile(fromRow, fromCol, ref_board);
        tile_rack_rearrange();
        recomputeWordSpans();

        refresh();
    }
}