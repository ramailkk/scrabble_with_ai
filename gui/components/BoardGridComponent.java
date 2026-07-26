package gui.components;

import application.model.BoardCell;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class BoardGridComponent {
    private JButton[][] buttons = new JButton[15][15];
    private boolean[][] tempBoard = new boolean[15][15];
    private Container container;

    public void initGrid(ActionListener listener, Container container, BoardCell[][] refBoard) {
        this.container = container;

        for (int i = 0; i < buttons.length; i++) {
            for (int j = 0; j < buttons[0].length; j++) {
                buttons[i][j] = new JButton();
                buttons[i][j].addActionListener(listener);

                if (i == 7 && j == 7) {
                    ImageIcon icon = new ImageIcon("resources/imgs/Star.png");
                    Image image = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(image);
                    buttons[i][j].setIcon(icon);
                    buttons[i][j].setBorder(new RoundedButton(10));
                    buttons[i][j].setBackground(new Color(255, 255, 255));
                    tempBoard[i][j] = true;
                } else if (refBoard[i][j].speciality == 1) {
                    ImageIcon icon = new ImageIcon("resources/imgs/DL.png");
                    Image image = icon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(image);
                    buttons[i][j].setIcon(icon);
                    buttons[i][j].setBorder(new RoundedButton(10));
                    buttons[i][j].setBackground(new Color(0, 104, 102));
                    tempBoard[i][j] = true;
                } else if (refBoard[i][j].speciality == 2) {
                    ImageIcon icon = new ImageIcon("resources/imgs/TL.png");
                    Image image = icon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(image);
                    buttons[i][j].setIcon(icon);
                    buttons[i][j].setBorder(new RoundedButton(10));
                    buttons[i][j].setBackground(new Color(238, 215, 161));
                    tempBoard[i][j] = true;
                } else if (refBoard[i][j].speciality == 3) {
                    ImageIcon icon = new ImageIcon("resources/imgs/DW.png");
                    Image image = icon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(image);
                    buttons[i][j].setIcon(icon);
                    buttons[i][j].setBorder(new RoundedButton(10));
                    buttons[i][j].setBackground(new Color(161, 207, 203));
                    tempBoard[i][j] = true;
                } else if (refBoard[i][j].speciality == 4) {
                    ImageIcon icon = new ImageIcon("resources/imgs/TW.png");
                    Image image = icon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(image);
                    buttons[i][j].setIcon(icon);
                    buttons[i][j].setBorder(new RoundedButton(10));
                    buttons[i][j].setBackground(new Color(255, 155, 155));
                    tempBoard[i][j] = true;
                } else {
                    buttons[i][j].setBorder(new RoundedButton(10));
                    buttons[i][j].setBackground(new Color(204, 204, 204));
                }
                container.add(buttons[i][j]);
            }
        }
    }

    /**
     * Returns the container the board buttons live in. Rack tiles are added
     * to the same container (see Panel.initBoard), so a point can be
     * converted into this coordinate space and passed to getCellAt.
     */
    public Container getContainer() {
        return container;
    }

    /**
     * Hit-tests a point (already expressed in this component's container's
     * coordinate space) against the 15x15 grid of board buttons.
     *
     * @return {row, col} of the cell under the point, or null if the point
     *         isn't over any board cell.
     */
    public int[] getCellAt(Point pointInContainerCoords) {
        if (pointInContainerCoords == null) return null;
        for (int r = 0; r < buttons.length; r++) {
            for (int c = 0; c < buttons[0].length; c++) {
                if (buttons[r][c].isShowing() && buttons[r][c].getBounds().contains(pointInContainerCoords)) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
    }

    public void layoutGrid(Graphics2D g, int x_p, int y_p, int width, int height) {
    int x = x_p;
    int y = y_p;
    for (int i = 1; i <= 15; i++) {
        for (int j = 1; j <= 15; j++) {
            buttons[i - 1][j - 1].setBounds(x, y, width, height);
            x = x + width;
        }
        x = x_p;
        y = y + height;
    }
    
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setColor(Color.BLACK);
    
    // Smaller font for coordinates to fit closer to board
    Font font = new Font("Consolas", Font.BOLD, 17);
    g.setFont(font);
    FontMetrics fm = g.getFontMetrics();
    
    // Row numbers (left side) - positioned just outside the board
    x = x_p - 6; // Reduced gap
    y = y_p + height / 2 + fm.getAscent() / 2 - 2;
    for (int i = 1; i <= 15; i++) {
        String num = String.valueOf(i);
        int numWidth = fm.stringWidth(num);
        g.drawString(num, x - numWidth, y);
        y = y + height;
    }
    
    // Column letters (top) - positioned just above the board
    x = x_p + width / 2;
    y = y_p - 6; // Reduced gap
    for (int i = 1; i <= 15; i++) {
        String letter = String.valueOf((char) (64 + i));
        int letterWidth = fm.stringWidth(letter);
        g.drawString(letter, x - letterWidth / 2, y);
        x = x + width;
    }
}

    public void AI_tileSetter(BoardCell[][] refBoard) {
        for (int r = 0; r < 15; r++) {
            for (int c = 0; c < 15; c++) {
                if (refBoard[r][c].isOccupied) {
                    int h = 40;
                    int w = 40;
                    if (!tempBoard[r][c]) buttons[r][c].setBackground(new Color(242, 191, 118));

                    ImageIcon icon = new ImageIcon("resources/imgs/" + String.valueOf(refBoard[r][c].tile.letter) + ".png");
                    Image image = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(image);
                    buttons[r][c].setIcon(icon);
                }
            }
        }
    }

    public void resetTiles(ArrayList<int[]> tempPositions, BoardCell[][] refBoard) {
        for (int i = 0; i < tempPositions.size(); i++) {
            int x = tempPositions.get(i)[0];
            int y = tempPositions.get(i)[1];
            if (x == 7 && y == 7) {
                ImageIcon icon = new ImageIcon("resources/imgs/Star.png");
                Image image = icon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
                icon = new ImageIcon(image);
                buttons[x][y].setIcon(icon);
                buttons[x][y].setBorder(new RoundedButton(10));
                buttons[x][y].setBackground(new Color(255, 255, 255));
            } else if (refBoard[x][y].speciality == 1) {
                ImageIcon icon = new ImageIcon("resources/imgs/DL.png");
                Image image = icon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
                icon = new ImageIcon(image);
                buttons[x][y].setIcon(icon);
                buttons[x][y].setBorder(new RoundedButton(10));
                buttons[x][y].setBackground(new Color(0, 104, 102));
            } else if (refBoard[x][y].speciality == 2) {
                ImageIcon icon = new ImageIcon("resources/imgs/TL.png");
                Image image = icon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
                icon = new ImageIcon(image);
                buttons[x][y].setIcon(icon);
                buttons[x][y].setBorder(new RoundedButton(10));
                buttons[x][y].setBackground(new Color(238, 215, 161));
            } else if (refBoard[x][y].speciality == 3) {
                ImageIcon icon = new ImageIcon("resources/imgs/DW.png");
                Image image = icon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
                icon = new ImageIcon(image);
                buttons[x][y].setIcon(icon);
                buttons[x][y].setBorder(new RoundedButton(10));
                buttons[x][y].setBackground(new Color(161, 207, 203));
            } else if (refBoard[x][y].speciality == 4) {
                ImageIcon icon = new ImageIcon("resources/imgs/TW.png");
                Image image = icon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
                icon = new ImageIcon(image);
                buttons[x][y].setIcon(icon);
                buttons[x][y].setBorder(new RoundedButton(10));
                buttons[x][y].setBackground(new Color(255, 155, 155));
            } else {
                buttons[x][y].setIcon(null);
                buttons[x][y].setBorder(new RoundedButton(10));
                buttons[x][y].setBackground(new Color(204, 204, 204));
            }
        }
    }

    public boolean isFreeTile(BoardCell[][] refBoard, int i, int j) {
        return !refBoard[i][j].isOccupied;
    }

    public boolean isSpecialTile(BoardCell[][] refBoard, int i, int j) {
        return refBoard[i][j].speciality == 1 || refBoard[i][j].speciality == 2 || refBoard[i][j].speciality == 3 || refBoard[i][j].speciality == 4;
    }

    public JButton[][] getButtons() {
        return buttons;
    }

    public JButton getButton(int r, int c) {
        return buttons[r][c];
    }
}