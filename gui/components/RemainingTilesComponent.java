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
    private static final int TITLE_HEIGHT = 45;

    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font COUNT_FONT = new Font("Calibri", Font.BOLD, 16);
    private static final Font STATS_FONT = new Font("Calibri", Font.PLAIN, 13);

    private static final Color PANEL_BG = new Color(245, 242, 235);
    private static final Color PANEL_BORDER = new Color(200, 190, 180);
    private static final Color COUNT_COLOR = new Color(80, 60, 30);
    private static final Color STATS_COLOR = new Color(100, 100, 100);

    // Cache fully-loaded, pre-scaled icons so each letter is only read/scaled once.
    private final Map<Character, ImageIcon> tileIconCache = new HashMap<>();

    private ImageIcon getTileIcon(char letter) {
        return tileIconCache.computeIfAbsent(letter, l -> {
            ImageIcon icon = new ImageIcon("resources/imgs/" + l + ".png");
            Image scaled = icon.getImage().getScaledInstance(TILE_ICON_SIZE, TILE_ICON_SIZE, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        });
    }

    /**
     * Counts vowels and consonants in the remaining tile counts.
     * Vowels: A, E, I, O, U
     * Consonants: all other letters
     */
    private int[] countVowelsAndConsonants(int[] remainingCounts) {
        int vowels = 0;
        int consonants = 0;
        String vowelLetters = "AEIOU";
        
        for (int i = 0; i < remainingCounts.length; i++) {
            char letter = (char) (65 + i); // 'A' = 65
            if (vowelLetters.indexOf(letter) >= 0) {
                vowels += remainingCounts[i];
            } else {
                consonants += remainingCounts[i];
            }
        }
        return new int[]{vowels, consonants};
    }

    public void drawRemaining(Graphics2D g, int x, int y, int[] remainingCounts) {
        int rows = (int) Math.ceil(remainingCounts.length / (double) COLUMNS);
        int boxWidth = COLUMNS * COLUMN_WIDTH + PADDING + 10;
        int boxHeight = TITLE_HEIGHT + rows * ROW_HEIGHT + 15;
        int boxX = x - 10;
        int boxY = y - 80;

        Font originalFont = g.getFont();
        Color originalColor = g.getColor();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Shadow
        g.setColor(new Color(0, 0, 0, 25));
        g.fillRoundRect(boxX + 3, boxY + 3, boxWidth, boxHeight, 12, 12);

        // Main panel background
        g.setColor(PANEL_BG);
        g.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 12, 12);

        // Border
        g.setColor(PANEL_BORDER);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 12, 12);

        // --- TITLE ON THE LEFT ---
        g.setStroke(new BasicStroke(1.0f));
        g.setFont(TITLE_FONT);
        g.setColor(Color.BLACK);
        
        int titleX = boxX + 15;
        int titleY = boxY + 30;
        g.drawString("Tile Bag", titleX, titleY);

        // --- VOWEL/CONSONANT COUNTERS ON THE RIGHT ---
        int[] counts = countVowelsAndConsonants(remainingCounts);
        g.setFont(STATS_FONT);
        g.setColor(STATS_COLOR);
        
        String statsText = "Vowels: " + counts[0] + "  Consonants: " + counts[1];
        FontMetrics statsFm = g.getFontMetrics();
        int statsWidth = statsFm.stringWidth(statsText);
        int statsX = boxX + boxWidth - statsWidth - 15;
        int statsY = boxY + 30;
        g.drawString(statsText, statsX, statsY);

        // Grid of tile icons paired tightly with their counts
        int startX = boxX + PADDING;
        int startY = boxY + TITLE_HEIGHT + 5;
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