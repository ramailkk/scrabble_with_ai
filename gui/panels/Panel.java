package gui.panels;

import application.model.Board;
import application.model.BoardCell;
import application.model.Tile;
import application.model.TileBag;
import gui.components.ActionToolbarComponent;
import gui.components.AIMoveAnimator;
import gui.components.BoardGridComponent;
import gui.components.RemainingTilesComponent;
import gui.components.TileRackComponent;
import gui.components.ScoreComponent;

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
    private ScoreComponent scoreComponent;

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

    // AI Animation fields
    private AIMoveAnimator aiAnimator;
    private boolean isAIAnimating = false;

    private WordValidator wordValidator = word -> board.dictionary.validateWord(word);

    public interface WordValidator {
        boolean isValid(String word);
    }

    // Getters for AI animation
    public ArrayList<Tile> getTiles_present_player1() {
        return tiles_present_player1;
    }

    public ArrayList<Tile> getTiles_present_player2() {
        return tiles_present_player2;
    }

    public ArrayList<Tile> getTilesSelectedFromRack() {
        return tiles_selected_from_rack;
    }

    public ArrayList<int[]> getTempPositions() {
        return temp_positions;
    }

    public void setAIAnimating(boolean animating) {
        this.isAIAnimating = animating;
    }

    // Structural validity of this turn's not-yet-submitted placement (temp_positions):
    // are all the tiles placed this turn on one line, with no unfilled gaps?
    private boolean structurallyValid = true;
    private java.util.List<int[]> cachedMisalignedCells = new ArrayList<>();
    private java.util.List<int[]> cachedGapCells = new ArrayList<>();

    /**
     * Checks whether this turn's placed tiles form a structurally legal
     * Scrabble placement: all in a single row or a single column, with no
     * empty gap between them (a gap is fine only if an already-committed
     * board tile fills it).
     */
    private void computeStructuralIssues() {
        cachedMisalignedCells = new ArrayList<>();
        cachedGapCells = new ArrayList<>();
        structurallyValid = true;

        if (temp_positions.size() <= 1) return;

        int r0 = temp_positions.get(0)[0];
        int c0 = temp_positions.get(0)[1];
        boolean rLock = true, cLock = true;
        for (int[] p : temp_positions) {
            if (p[0] != r0) rLock = false;
            if (p[1] != c0) cLock = false;
        }

        if (!rLock && !cLock) {
            structurallyValid = false;
            Map<Integer, Integer> rowCounts = new HashMap<>();
            Map<Integer, Integer> colCounts = new HashMap<>();
            for (int[] p : temp_positions) {
                rowCounts.merge(p[0], 1, Integer::sum);
                colCounts.merge(p[1], 1, Integer::sum);
            }

            int bestRow = r0, bestRowCount = 0;
            for (Map.Entry<Integer, Integer> e : rowCounts.entrySet()) {
                if (e.getValue() > bestRowCount) { bestRow = e.getKey(); bestRowCount = e.getValue(); }
            }
            int bestCol = c0, bestColCount = 0;
            for (Map.Entry<Integer, Integer> e : colCounts.entrySet()) {
                if (e.getValue() > bestColCount) { bestCol = e.getKey(); bestColCount = e.getValue(); }
            }

            if (bestRowCount >= bestColCount) {
                for (int[] p : temp_positions) if (p[0] != bestRow) cachedMisalignedCells.add(p);
            } else {
                for (int[] p : temp_positions) if (p[1] != bestCol) cachedMisalignedCells.add(p);
            }
            return;
        }

        // All on one line - check for gaps
        if (rLock) {
            int cMin = c0, cMax = c0;
            for (int[] p : temp_positions) { cMin = Math.min(cMin, p[1]); cMax = Math.max(cMax, p[1]); }
            for (int c = cMin; c <= cMax; c++) {
                if (findTempIndexAt(r0, c) == -1 && !ref_board[r0][c].isOccupied) {
                    cachedGapCells.add(new int[]{r0, c});
                }
            }
        } else {
            int rMin = r0, rMax = r0;
            for (int[] p : temp_positions) { rMin = Math.min(rMin, p[0]); rMax = Math.max(rMax, p[0]); }
            for (int r = rMin; r <= rMax; r++) {
                if (findTempIndexAt(r, c0) == -1 && !ref_board[r][c0].isOccupied) {
                    cachedGapCells.add(new int[]{r, c0});
                }
            }
        }

        if (!cachedGapCells.isEmpty()) structurallyValid = false;
    }

    /**
     * Checks if a cell is adjacent to any existing occupied tile on the board.
     * For the first move, the center square (7,7) is considered "connected".
     */
    private boolean isCellAdjacentToExistingTile(int row, int col) {
        if (board.isEmpty()) {
            return row == 7 && col == 7;
        }
        if (ref_board[row][col].isOccupied) {
            return true;
        }
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            if (newRow >= 0 && newRow < 15 && newCol >= 0 && newCol < 15) {
                if (ref_board[newRow][newCol].isOccupied) {
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

    /** Draws markers over cells that break the structural line */
    private void drawStructuralWarnings(Graphics2D g) {
        if (cachedMisalignedCells.isEmpty() && cachedGapCells.isEmpty()) return;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Stroke oldStroke = g.getStroke();
        g.setColor(new Color(220, 50, 50));
        int margin = 3;
        int arc = 12;

        g.setStroke(new BasicStroke(3f));
        for (int[] p : cachedMisalignedCells) {
            Rectangle b = boardGridComponent.getButton(p[0], p[1]).getBounds();
            g.drawRoundRect(b.x - margin, b.y - margin, b.width + margin * 2, b.height + margin * 2, arc, arc);
        }

        float[] dash = {6f, 6f};
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, dash, 0));
        for (int[] p : cachedGapCells) {
            Rectangle b = boardGridComponent.getButton(p[0], p[1]).getBounds();
            g.drawRoundRect(b.x - margin, b.y - margin, b.width + margin * 2, b.height + margin * 2, arc, arc);
        }

        g.setStroke(oldStroke);
    }

    public void Reset_Tiles(int player) {
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

    private static class WordSpan {
        final int r1, c1, r2, c2;
        final String word;
        final boolean valid;
        final boolean connected;

        WordSpan(int r1, int c1, int r2, int c2, String word, boolean valid, boolean connected) {
            this.r1 = r1;
            this.c1 = c1;
            this.r2 = r2;
            this.c2 = c2;
            this.word = word;
            this.valid = valid;
            this.connected = connected;
        }
    }

    private java.util.List<WordSpan> cachedWordSpans = new ArrayList<>();
    private JLayeredPane boardLayeredPane;
    private JComponent ringOverlay;

    public Panel() {
        ref_board = board.getTheBoard();
        tiles_present_player1 = tileBag.getRack_player_1();
        tiles_present_player2 = tileBag.getRack_player_2();
        swap_panel.setPanel(this);
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
        setLayout(null);

        boardLayeredPane = new JLayeredPane();
        boardLayeredPane.setBounds(0, 0, B_WIDTH, B_HEIGHT);
        boardLayeredPane.setOpaque(false);
        boardLayeredPane.setBackground(Color.WHITE);
        add(boardLayeredPane);

        boardGridComponent.initGrid(this, boardLayeredPane, ref_board);
        tileRackComponent.initRacks(this, this, tiles_present_player1, tiles_present_player2);
        actionToolbarComponent.initToolbar(this, this);

        ringOverlay = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                drawWordRings((Graphics2D) g);
                drawStructuralWarnings((Graphics2D) g);
            }
        };
        ringOverlay.setOpaque(false);
        ringOverlay.setBounds(0, 0, B_WIDTH, B_HEIGHT);
        boardLayeredPane.add(ringOverlay, JLayeredPane.PALETTE_LAYER);

        boardGridComponent.layoutBoundsOnce(50, 50, 40, 40);
        tileRackComponent.layoutPlayer2Once(720, 150, 40, 40);
        tileRackComponent.layoutPlayer1Once(720, 510, 40, 40);
        actionToolbarComponent.layoutToolbarOnce(440, 570, 1000, 60);

        tileRackComponent.setBoardGridComponent(boardGridComponent);
        tileRackComponent.setDropListener(this::handleTileDrop);

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

        // Initialize AI Animator
        aiAnimator = new AIMoveAnimator(this, boardGridComponent, tileRackComponent);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boardGridComponent.drawGridLabels(graphics2D, 50, 50, 40, 40);

        scoreComponent.setTempData(temp_positions, tiles_selected_from_rack);
        scoreComponent.drawScores(graphics2D, Player_score_1, Player_score_2, player);

        remainingTilesComponent.drawRemaining(graphics2D, 680, 300, tileBag.getRemaining());
    }

    public void recomputeWordSpans() {
        computeStructuralIssues();
        cachedWordSpans = computeWordSpans();
        if (ringOverlay != null) {
            ringOverlay.repaint();
        }
    }

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

            // Green: valid word AND connected to existing tiles AND structurally valid
            // Red: invalid word OR isolated (not connected) OR structurally invalid
            boolean isValidWord = span.valid && structurallyValid && span.connected;
            g.setColor(isValidWord ? new Color(0, 170, 0) : new Color(220, 50, 50));
            g.drawRoundRect(x, y, w, h, arc, arc);
        }

        g.setStroke(oldStroke);
    }

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
                    boolean valid = wordValidator.isValid(word);
                    boolean connected = isSpanConnected(r, c1, r, c2);
                    spans.add(new WordSpan(r, c1, r, c2, word, valid, connected));
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
                    boolean valid = wordValidator.isValid(word);
                    boolean connected = isSpanConnected(r1, c, r2, c);
                    spans.add(new WordSpan(r1, c, r2, c, word, valid, connected));
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

        // Handle Reset buttons from tile rack
        JButton[] resetButtons = tileRackComponent.getResetButtons();
        if (e.getSource() == resetButtons[0] || e.getSource() == resetButtons[1]) {
            Reset_Tiles(player);
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

        // Handle toolbar buttons: Submit(0), Skip(1), AI(2), Swap(3), Resign(4)
        JButton[] options = actionToolbarComponent.getOptionsButtons();
        if (e.getSource() == options[0] && !swap_active) { // Submit
            if (!gameEnd && !isAIAnimating) {
                // Check if there's at least one valid connected word
                boolean hasValidConnectedWord = false;
                for (WordSpan span : cachedWordSpans) {
                    if (span.valid && span.connected && structurallyValid) {
                        hasValidConnectedWord = true;
                        break;
                    }
                }

                if (!hasValidConnectedWord && !board.isEmpty()) {
                    JOptionPane.showMessageDialog(this, 
                        "Your tiles must form at least one valid word connected to existing tiles!", 
                        "Invalid Move", JOptionPane.WARNING_MESSAGE);
                    return;
                }

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
        } else if (e.getSource() == options[1] && !swap_active) { // Skip
            System.out.println("Skip");
            Reset_Tiles(player);
            change_turn();
            refresh();
        } else if (e.getSource() == options[2] && !swap_active) { // AI
            if (isAIAnimating) {
                System.out.println("AI animation already in progress");
                return;
            }
            
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
                
                if (board.potentialMove != null) {
                    isAIAnimating = true;
                    
                    // Filter down to just the new cells (not already occupied)
                    ArrayList<Tile> tiles = new ArrayList<>();
                    ArrayList<int[]> positions = new ArrayList<>();
                    for (int i = 0; i < board.potentialMove.positions.size(); i++) {
                        int pos = board.potentialMove.positions.get(i);
                        int[] rc = board.decryptPosition(pos);
                        if (!board.getTheBoard()[rc[0]][rc[1]].isOccupied) {
                            tiles.add(board.potentialMove.tiles.get(i));
                            positions.add(rc);
                        }
                    }
                    
                    temp_positions.clear();
                    tiles_selected_from_rack.clear();
                    
                    aiAnimator.animateAIMove(tiles, positions, player);
                } else {
                    board.stuff += "AI: No valid move found\n";
                    change_turn();
                    refresh();
                }
            }
        } else if (e.getSource() == options[3] && !swap_active) { // Swap
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
        } else if (e.getSource() == options[4] && !swap_active) { // Resign
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

    private boolean isCellAvailable(int r, int c) {
        if (!boardGridComponent.isFreeTile(ref_board, r, c)) return false;
        for (int[] pos : temp_positions) {
            if (pos[0] == r && pos[1] == c) return false;
        }
        return true;
    }

    private void tile_setter(int i) {
        if (current_letter_selected != null && !current_tile_selected.isEmpty()) {
            int row = current_tile_selected.get(0);
            int col = current_tile_selected.get(1);

            if (!isCellAvailable(row, col)) {
                current_tile_selected.clear();
                current_letter_selected = null;
                return;
            }

            JButton targetButton = boardGridComponent.getButton(row, col);
            
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
            
            tileRackComponent.rearrange(player, tiles_present_player1, tiles_present_player2);
            
            int[] rc = {row, col};
            if (!boardGridComponent.isSpecialTile(ref_board, row, col)) {
                targetButton.setBackground(new Color(242, 191, 118));
            }
            temp_positions.add(rc);
            current_letter_selected = null;
            current_tile_selected.clear();
            recomputeWordSpans();

            targetButton.repaint();
            refresh();
        }
    }

    public void AI_tileSetter() {
        boardGridComponent.AI_tileSetter(ref_board);
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

        if (isVisible()) {
            needsRepaint = true;
            coalescedRepaintTimer.restart();
        }
    }

    private void handleTileDrop(int dropPlayer, int tileIndex, int row, int col) {
        if (dropPlayer != this.player) {
            JOptionPane.showMessageDialog(this, "It's not your turn!", "Invalid Move", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!isCellAvailable(row, col)) {
            JOptionPane.showMessageDialog(this, "This square is already occupied!", "Invalid Move", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Allow placement anywhere - no adjacency restriction during drag

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

    private int findTempIndexAt(int r, int c) {
        for (int i = 0; i < temp_positions.size(); i++) {
            if (temp_positions.get(i)[0] == r && temp_positions.get(i)[1] == c) return i;
        }
        return -1;
    }

    private void handleBoardTileMoved(int fromRow, int fromCol, int toRow, int toCol) {
        int idx = findTempIndexAt(fromRow, fromCol);
        if (idx == -1) return;

        if (!isCellAvailable(toRow, toCol)) {
            JOptionPane.showMessageDialog(this, "This square is already occupied!", "Invalid Move", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Allow movement anywhere - no adjacency restriction

        Tile tile = tiles_selected_from_rack.get(idx);

        boardGridComponent.resetSingleTile(fromRow, fromCol, ref_board);

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