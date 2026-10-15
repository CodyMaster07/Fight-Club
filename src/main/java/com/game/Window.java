package com.game;

import java.awt.Canvas;
import java.awt.Dimension;
import javax.swing.JFrame;

/**
 *
 * @author evan.gabb
 */
public class Window {
    
    private Canvas canvas;
    private JFrame frame;
    private int width = 400;
    private int height = 300;

    public Window(String title, int scale) {
        this.width = scale*this.width;
        this.height = scale*this.height;

        frame = new JFrame(title);
        frame.setSize(this.width, this.height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        // O Canvas é a tela em branco onde a Engine vai desenhar as entidades
        canvas = new Canvas();
        
        canvas.setPreferredSize(new Dimension(this.width, this.height));
        canvas.setMaximumSize(new Dimension(this.width, this.height));
        canvas.setMinimumSize(new Dimension(this.width, this.height));
        canvas.setFocusable(false); // Deixa o foco do teclado no JFrame

        frame.add(canvas);
        frame.pack(); // Ajusta as bordas
        frame.setVisible(true);
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
   
}
