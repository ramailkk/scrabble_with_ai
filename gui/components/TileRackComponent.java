package gui.components;

import application.model.Tile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class TileRackComponent {
    private JButton[] tile_rack_player_1 = new JButton[7];
    private JButton[] tile_rack_player_2 = new JButton[7];
    private JButton[] shuffle = new JButton[2];

    public void initRacks(ActionListener listener, Container container, ArrayList<Tile> p1Tiles, ArrayList<Tile> p2Tiles) {
        for (int i = 0; i < shuffle.length; i++) {
            ImageIcon icon = new ImageIcon("resources/imgs/Shuffle.png");
            Image image = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            icon = new ImageIcon(image);
            shuffle[i] = new JButton(icon);
            shuffle[i].addActionListener(listener);
            shuffle[i].setBorder(new RoundedButton(10));
            shuffle[i].setBackground(new Color(204, 204, 204));
            container.add(shuffle[i]);
        }
        for (int i = 0; i < tile_rack_player_1.length; i++) {
            ImageIcon icon = new ImageIcon("resources/imgs/" + String.valueOf(p1Tiles.get(i).letter).toUpperCase() + ".png");
            Image image = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            icon = new ImageIcon(image);
            tile_rack_player_1[i] = new JButton(icon);
            tile_rack_player_1[i].addActionListener(listener);
            tile_rack_player_1[i].setBorder(new RoundedButton(10));
            tile_rack_player_1[i].setBackground(new Color(242, 191, 118));
            container.add(tile_rack_player_1[i]);

            icon = new ImageIcon("resources/imgs/" + String.valueOf(p2Tiles.get(i).letter).toUpperCase() + ".png");
            image = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            icon = new ImageIcon(image);
            tile_rack_player_2[i] = new JButton(icon);
            tile_rack_player_2[i].addActionListener(listener);
            tile_rack_player_2[i].setBorder(new RoundedButton(10));
            tile_rack_player_2[i].setBackground(new Color(242, 191, 118));
            container.add(tile_rack_player_2[i]);
        }
    }

    public void rearrange(int player, ArrayList<Tile> p1Tiles, ArrayList<Tile> p2Tiles) {
        if (player == 1) {
            for (int i = 0; i < tile_rack_player_1.length; i++) {
                if (i < p1Tiles.size()) {
                    ImageIcon icon = new ImageIcon("resources/imgs/" + String.valueOf(p1Tiles.get(i).letter).toUpperCase() + ".png");
                    Image image = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(image);
                    tile_rack_player_1[i].setIcon(icon);
                } else {
                    ImageIcon icon = new ImageIcon("resources/imgs/Empty.png");
                    Image image = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(image);
                    tile_rack_player_1[i].setIcon(icon);
                }
            }
        } else {
            for (int i = 0; i < tile_rack_player_2.length; i++) {
                if (i < p2Tiles.size()) {
                    ImageIcon icon = new ImageIcon("resources/imgs/" + String.valueOf(p2Tiles.get(i).letter).toUpperCase() + ".png");
                    Image image = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(image);
                    tile_rack_player_2[i].setIcon(icon);
                } else {
                    ImageIcon icon = new ImageIcon("resources/imgs/Empty.png");
                    Image image = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(image);
                    tile_rack_player_2[i].setIcon(icon);
                }
            }
        }
    }

    public void layoutPlayer1(Graphics2D g, int x_p, int y_p, int width, int height, int player, int score1) {
        g.drawString("Player 1", x_p + 100, y_p - 10);
        if (player == 1)
            g.drawString("(Your turn)", x_p + 180, y_p - 10);
        shuffle[0].setBounds(x_p - 50, y_p, 40, 40);
        g.drawString("Score = " + String.valueOf(score1), x_p + 300, y_p + width / 2 + 10);
        int x = x_p;
        for (int i = 0; i < tile_rack_player_1.length; i++) {
            tile_rack_player_1[i].setBounds(x, y_p, width, height);
            x = x + width;
        }
    }

    public void layoutPlayer2(Graphics2D g, int x_p, int y_p, int width, int height, int player, int score2) {
        g.drawString("Player 2", x_p + 100, y_p - 10);
        shuffle[1].setBounds(x_p - 50, y_p, 40, 40);
        if (player == 2)
            g.drawString("(Your turn)", x_p + 180, y_p - 10);
        g.drawString("Score = " + String.valueOf(score2), x_p + 300, y_p + width / 2 + 10);
        int x = x_p;
        for (int i = 0; i < tile_rack_player_2.length; i++) {
            tile_rack_player_2[i].setBounds(x, y_p, width, height);
            x = x + width;
        }
    }

    public JButton[] getTileRackPlayer1() {
        return tile_rack_player_1;
    }

    public JButton[] getTileRackPlayer2() {
        return tile_rack_player_2;
    }

    public JButton[] getShuffleButtons() {
        return shuffle;
    }
}
