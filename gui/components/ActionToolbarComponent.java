package gui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class ActionToolbarComponent {
    private JButton[] options_buttons = new JButton[5];
    private String[] buttonLabels = {"Submit", "Skip", "AI", "Swap", "Resign"};
    private Color[] buttonColors = {
        new Color(144, 238, 144),    // Submit - Green
        new Color(238, 215, 161),    // Skip - Gold
        new Color(255, 188, 218),    // AI - Pink
        new Color(65, 105, 225),     // Swap - Blue
        new Color(235, 45, 58)       // Resign - Red
    };
    private String[] iconPaths = {
        "resources/imgs/Submit.png",
        "resources/imgs/Skip.png",
        "resources/imgs/AI.png",
        "resources/imgs/Swap.png",
        "resources/imgs/Resign.png"
    };
    
    private JPanel toolbarPanel;

    public void initToolbar(ActionListener listener, Container container) {
        toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        toolbarPanel.setOpaque(false);
        
        for (int i = 0; i < options_buttons.length; i++) {
            options_buttons[i] = new JButton();
            options_buttons[i].addActionListener(listener);
            
            // Create icon (smaller for horizontal layout)
            int iconSize = 28;
            ImageIcon icon = new ImageIcon(iconPaths[i]);
            Image image = icon.getImage().getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH);
            icon = new ImageIcon(image);
            
            // Set icon and text
            options_buttons[i].setIcon(icon);
            options_buttons[i].setText(buttonLabels[i]);
            
            // Position text to the RIGHT of icon (horizontal)
            options_buttons[i].setHorizontalTextPosition(SwingConstants.RIGHT);
            options_buttons[i].setVerticalTextPosition(SwingConstants.CENTER);
            
            // Add spacing between icon and text
            options_buttons[i].setIconTextGap(8);
            
            // Style the text
            options_buttons[i].setFont(new Font("Segoe UI", Font.BOLD, 12));
            options_buttons[i].setForeground(Color.BLACK);
            
            // Button background
            options_buttons[i].setBackground(buttonColors[i]);
            
            // Rounded border
            options_buttons[i].setBorder(new RoundedButton(10));
            
            // Make sure button background is painted
            options_buttons[i].setOpaque(true);
            options_buttons[i].setContentAreaFilled(true);
            options_buttons[i].setFocusPainted(false);
            
            // Set preferred size
            options_buttons[i].setPreferredSize(new Dimension(100, 40));
            
            toolbarPanel.add(options_buttons[i]);
        }
        
        container.add(toolbarPanel);
    }

    /** Sets bounds for the toolbar panel. */
    public void layoutToolbarOnce(int x_p, int y_p, int width, int height) {
        if (toolbarPanel != null) {
            toolbarPanel.setBounds(x_p, y_p, width, height);
        }
    }

    public JButton[] getOptionsButtons() {
        return options_buttons;
    }

    public JButton getButton(int index) {
        return options_buttons[index];
    }
}