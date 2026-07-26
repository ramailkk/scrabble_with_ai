package gui.components;

import application.model.Tile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class TileRackComponent {
    private JButton[] tile_rack_player_1 = new JButton[7];
    private JButton[] tile_rack_player_2 = new JButton[7];
    private JButton[] shuffle = new JButton[2];

    private Container container;
    private BoardGridComponent boardGridComponent;
    private TileDropListener dropListener;

    // state for the tile currently being dragged (only one drag happens at a time)
    private JLabel dragLabel;

    /** Callback fired when a rack tile is dropped onto a board cell. */
    public interface TileDropListener {
        void onTileDropped(int player, int index, int row, int col);
    }

    public void setBoardGridComponent(BoardGridComponent boardGridComponent) {
        this.boardGridComponent = boardGridComponent;
    }

    public void setDropListener(TileDropListener dropListener) {
        this.dropListener = dropListener;
    }

    public void initRacks(ActionListener listener, Container container, ArrayList<Tile> p1Tiles, ArrayList<Tile> p2Tiles) {
        this.container = container;

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
            makeDraggable(tile_rack_player_1[i], i, 1, p1Tiles);
            container.add(tile_rack_player_1[i]);

            icon = new ImageIcon("resources/imgs/" + String.valueOf(p2Tiles.get(i).letter).toUpperCase() + ".png");
            image = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            icon = new ImageIcon(image);
            tile_rack_player_2[i] = new JButton(icon);
            tile_rack_player_2[i].addActionListener(listener);
            tile_rack_player_2[i].setBorder(new RoundedButton(10));
            tile_rack_player_2[i].setBackground(new Color(242, 191, 118));
            makeDraggable(tile_rack_player_2[i], i, 2, p2Tiles);
            container.add(tile_rack_player_2[i]);
        }
    }

    /**
     * Manual drag implementation: on press, a floating copy of the tile's
     * icon is added to the window's glass pane and tracked with the mouse.
     * On release, if the pointer is over a free board cell, the drop
     * listener is invoked; otherwise the floating tile is simply removed
     * and the rack button (which never lost its icon) is left exactly as
     * it was -- i.e. the tile "snaps back" to the rack for free.
     */
    private void makeDraggable(JButton button, int index, int player, ArrayList<Tile> rack) {
        MouseAdapter dragAdapter = new MouseAdapter() {
            private ImageIcon originalIcon;

            @Override
            public void mousePressed(MouseEvent e) {
                if (index >= rack.size() || button.getIcon() == null) return;

                JRootPane rootPane = SwingUtilities.getRootPane(button);
                if (rootPane == null) return;

                originalIcon = (ImageIcon) button.getIcon();

                JComponent glass = (JComponent) rootPane.getGlassPane();
                glass.setLayout(null);

                dragLabel = new JLabel(originalIcon);
                dragLabel.setSize(button.getWidth(), button.getHeight());
                Point startInGlass = SwingUtilities.convertPoint(button, e.getPoint(), glass);
                dragLabel.setLocation(startInGlass.x - dragLabel.getWidth() / 2, startInGlass.y - dragLabel.getHeight() / 2);
                glass.add(dragLabel);
                glass.setVisible(true);

                // Hide the icon in the rack slot while the tile is "in the air",
                // but keep the button itself (border/background) in place.
                button.setIcon(null);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragLabel == null) return;
                JRootPane rootPane = SwingUtilities.getRootPane(button);
                if (rootPane == null) return;
                JComponent glass = (JComponent) rootPane.getGlassPane();
                Point p = SwingUtilities.convertPoint(button, e.getPoint(), glass);
                dragLabel.setLocation(p.x - dragLabel.getWidth() / 2, p.y - dragLabel.getHeight() / 2);
                glass.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragLabel == null) return;
                JRootPane rootPane = SwingUtilities.getRootPane(button);

                int[] cell = null;
                if (rootPane != null && boardGridComponent != null && container != null) {
                    Point pointInContainer = SwingUtilities.convertPoint(button, e.getPoint(), boardGridComponent.getContainer());
                    cell = boardGridComponent.getCellAt(pointInContainer);
                }

                if (rootPane != null) {
                    JComponent glass = (JComponent) rootPane.getGlassPane();
                    glass.remove(dragLabel);
                    glass.setVisible(false);
                    glass.repaint();
                }
                dragLabel = null;

                // Restore the rack icon by default -- if the drop is accepted,
                // the listener will trigger a full rack rearrange that
                // overwrites this anyway.
                button.setIcon(originalIcon);

                if (cell != null && dropListener != null) {
                    dropListener.onTileDropped(player, index, cell[0], cell[1]);
                }
            }
        };
        button.addMouseListener(dragAdapter);
        button.addMouseMotionListener(dragAdapter);
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

    public void layoutPlayer1(Graphics2D g, int x_p, int y_p, int width, int height, int player) {
        shuffle[0].setBounds(x_p - 50, y_p, 40, 40);
        int x = x_p;
        for (int i = 0; i < tile_rack_player_1.length; i++) {
            tile_rack_player_1[i].setBounds(x, y_p, width, height);
            x = x + width;
        }
    }

    public void layoutPlayer2(Graphics2D g, int x_p, int y_p, int width, int height, int player) {
        shuffle[1].setBounds(x_p - 50, y_p, 40, 40);
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