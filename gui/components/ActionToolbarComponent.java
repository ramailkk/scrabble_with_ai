package gui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class ActionToolbarComponent {
    private JButton[] options_buttons = new JButton[6];

    public void initToolbar(ActionListener listener, Container container) {
        for (int i = 0; i < options_buttons.length; i++) {
            options_buttons[i] = new JButton();
            options_buttons[i].addActionListener(listener);
            container.add(options_buttons[i]);
            if (i == 0) {
                ImageIcon icon = new ImageIcon("resources/imgs/Reset.png");
                Image image = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                icon = new ImageIcon(image);
                options_buttons[i].setIcon(icon);
                options_buttons[i].setBorder(new RoundedButton(10));
                options_buttons[i].setBackground(new Color(204, 204, 204));
            } else if (i == 1) {
                ImageIcon icon = new ImageIcon("resources/imgs/AI.png");
                Image image = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                icon = new ImageIcon(image);
                options_buttons[i].setIcon(icon);
                options_buttons[i].setBorder(new RoundedButton(10));
                options_buttons[i].setBackground(new Color(255, 188, 218));
            } else if (i == 2) {
                ImageIcon icon = new ImageIcon("resources/imgs/Skip.png");
                Image image = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                icon = new ImageIcon(image);
                options_buttons[i].setIcon(icon);
                options_buttons[i].setBorder(new RoundedButton(10));
                options_buttons[i].setBackground(new Color(238, 215, 161));
            } else if (i == 3) {
                ImageIcon icon = new ImageIcon("resources/imgs/Submit.png");
                Image image = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                icon = new ImageIcon(image);
                options_buttons[i].setIcon(icon);
                options_buttons[i].setBorder(new RoundedButton(10));
                options_buttons[i].setBackground(new Color(144, 238, 144));
            } else if (i == 4) {
                ImageIcon icon = new ImageIcon("resources/imgs/Swap.png");
                Image image = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                icon = new ImageIcon(image);
                options_buttons[i].setIcon(icon);
                options_buttons[i].setBorder(new RoundedButton(10));
                options_buttons[i].setBackground(new Color(65, 105, 225));
            } else if (i == 5) {
                ImageIcon icon = new ImageIcon("resources/imgs/Resign.png");
                Image image = icon.getImage().getScaledInstance(90, 90, Image.SCALE_SMOOTH);
                icon = new ImageIcon(image);
                options_buttons[i].setIcon(icon);
                options_buttons[i].setBorder(new RoundedButton(10));
                options_buttons[i].setBackground(new Color(235, 45, 58));
            }
        }
    }

    /** Sets bounds for the toolbar buttons. Call ONCE (e.g. from Panel.initBoard) -- they never move mid-game. */
    public void layoutToolbarOnce(int x_p, int y_p, int width, int height) {
        int x = x_p;
        for (int i = 0; i < options_buttons.length; i++) {
            options_buttons[i].setBounds(x, y_p, width, height);
            x = x + width;
        }
    }

    public JButton[] getOptionsButtons() {
        return options_buttons;
    }

    public JButton getButton(int index) {
        return options_buttons[index];
    }
}