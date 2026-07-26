package gui.components;

import application.model.BoardCell;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class BoardGridComponent {
    private JButton[][] buttons = new JButton[15][15];
    private boolean[][] tempBoard = new boolean[15][15];

    public void initGrid(ActionListener listener, Container container, BoardCell[][] refBoard) {
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
        g.setColor(Color.BLACK);
        Font font = new Font("Times New Roman", Font.BOLD, 20);
        g.setFont(font);
        x = x_p - (width / 2 + 10);
        y = y_p + height / 2;
        for (int i = 1; i <= 15; i++) {
            g.drawString(String.valueOf(i), x, y);
            y = y + height;
        }
        x = x_p + (width / 2 - 10);
        y = y_p - height / 2;
        for (int i = 1; i <= 15; i++) {
            g.drawString(String.valueOf((char) (64 + i)), x, y);
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
