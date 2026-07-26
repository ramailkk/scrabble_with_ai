package gui.frames;

import gui.panels.Panel;

import java.awt.*;
import javax.swing.*;

public class Frame extends JFrame {
    public Frame() {
        this.add(new Panel());
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        this.setTitle("Scrabble Sigma v1.69");
        ImageIcon icon = new ImageIcon("resources/imgs/Scrabble.png");
        Image image = icon.getImage();
        this.setIconImage(image);
    }

    public static void main(String[] args) {
        Frame frame = new Frame();
    }
}
