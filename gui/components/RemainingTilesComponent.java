package gui.components;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class RemainingTilesComponent {

    private static final int COLUMNS = 7;
    private static final int TILE_ICON_SIZE = 28;
    private static final int COLUMN_WIDTH = 78;
    private static final int ROW_HEIGHT = 48;
    private static final int PADDING = 20;
    private static final int TITLE_HEIGHT = 55;

    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 20);

    private static final Font COUNT_FONT = new Font("Calibri", Font.BOLD, 16);

    private static final Color BOX_BG = new Color(250, 248, 241);
    private static final Color BOX_BORDER = new Color(196, 180, 150);
    private static final Color SEPARATOR_COLOR = new Color(210, 198, 170);
    private static final Color COUNT_COLOR = new Color(80, 60, 30);

    // Cache fully-loaded, pre-scaled icons so each letter is only read/scaled once.
    private final Map<Character, ImageIcon> tileIconCache = new HashMap<>();

    private ImageIcon getTileIcon(char letter) {
        return tileIconCache.computeIfAbsent(letter, l -> {
            ImageIcon icon = new ImageIcon("resources/imgs/" + l + ".png");
            Image scaled = icon.getImage().getScaledInstance(TILE_ICON_SIZE, TILE_ICON_SIZE, Image.SCALE_SMOOTH);
            // Re-wrapping in a new ImageIcon forces it to block (via MediaTracker)
            // until the scaled image is fully loaded, so it's guaranteed ready to paint.
            return new ImageIcon(scaled);
        });
    }

   public void drawRemaining(Graphics2D g, int x, int y, int[] remainingCounts) {
    int rows = (int) Math.ceil(remainingCounts.length / (double) COLUMNS);
    int boxWidth = COLUMNS * COLUMN_WIDTH + PADDING;
    int boxHeight = TITLE_HEIGHT + rows * ROW_HEIGHT + 10;
    int boxX = x - 10;
    int boxY = y - 80;

    Font originalFont = g.getFont();
    Color originalColor = g.getColor();

    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Shadow offset
    int shadowOffset = 5;

    // Draw shadow with gradient
    for (int i = shadowOffset; i > 0; i--) {
        int alpha = 30 + (shadowOffset - i) * 15;
        g.setColor(new Color(0, 0, 0, Math.min(alpha, 90)));
        g.fillRoundRect(
            boxX + i + 2, 
            boxY + i + 2, 
            boxWidth, 
            boxHeight, 
            25, 25
        );
    }

    // Draw main box
    g.setColor(BOX_BG);
    g.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 25, 25);
    
    // Draw border with thicker right and bottom edges
    g.setStroke(new BasicStroke(1.5f));
    g.setColor(BOX_BORDER);
    g.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 25, 25);

    // Thicker right border (shadow effect)
    g.setStroke(new BasicStroke(4.0f));
    g.setColor(new Color(0, 0, 0));
    g.drawLine(
        boxX + boxWidth - 1, 
        boxY + 8, 
        boxX + boxWidth - 1, 
        boxY + boxHeight - 8
    );

    // Thicker bottom border (shadow effect)
    g.drawLine(
        boxX + 8, 
        boxY + boxHeight - 1, 
        boxX + boxWidth - 8, 
        boxY + boxHeight - 1
    );


        // Title
        // --- TITLE - CENTERED ---
        g.setStroke(new BasicStroke(1.0f));
        g.setFont(TITLE_FONT);
        g.setColor(Color.BLACK);
        
        // Calculate centered position for the title
        FontMetrics fm = g.getFontMetrics();
        String title = "Remaining Tiles";
        int titleWidth = fm.stringWidth(title);
        int titleX = boxX + (boxWidth - titleWidth) / 2; // Center horizontally
        int titleY = boxY + 32;
        g.drawString(title, titleX, titleY);

        // // Separator under the title
        // g.setColor(SEPARATOR_COLOR);
        // g.drawLine(boxX + PADDING, boxY + 42, boxX + boxWidth - PADDING, boxY + 42);

        // Grid of tile icons paired tightly with their counts
        int startX = boxX + PADDING;
        int startY = boxY + TITLE_HEIGHT;
        int x_p = startX;
        int y_p = startY;

        for (int i = 1; i <= remainingCounts.length; i++) {
            char letter = (char) (64 + i);
            ImageIcon icon = getTileIcon(letter);
            icon.paintIcon(null, g, x_p, y_p);

            g.setFont(COUNT_FONT);
            g.setColor(COUNT_COLOR);
            g.drawString("x" + remainingCounts[i - 1], x_p + TILE_ICON_SIZE + 4, y_p + TILE_ICON_SIZE - 7);

            x_p += COLUMN_WIDTH;
            if (i % COLUMNS == 0) {
                x_p = startX;
                y_p += ROW_HEIGHT;
            }
        }

        g.setFont(originalFont);
        g.setColor(originalColor);
    }
}