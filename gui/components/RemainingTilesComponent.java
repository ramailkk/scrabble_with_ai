package gui.components;

import java.awt.*;
import java.util.Locale;

public class RemainingTilesComponent {
    public void drawRemaining(Graphics2D graphics2D, int x, int y, int[] remainingCounts) {
        int x_p = x;
        int y_p = y;
        graphics2D.drawRoundRect(x - 10, y - 80, 550, 250, 25, 25);
        graphics2D.setFont(new Font("Herona", Font.PLAIN, 25));
        graphics2D.drawString("Remaining Tiles:", x, y - 50);

        graphics2D.setFont(new Font("Herona", Font.PLAIN, 20));
        for (int i = 1; i <= remainingCounts.length; i++) {
            graphics2D.drawString(String.valueOf((char) (65 + (i - 1))).toUpperCase(Locale.ROOT) + " x " + remainingCounts[i - 1], x_p, y_p);
            x_p = x_p + 80;
            if (i % 7 == 0) {
                x_p = x;
                y_p = y_p + 50;
            }
        }
        graphics2D.setFont(null);
    }
}
