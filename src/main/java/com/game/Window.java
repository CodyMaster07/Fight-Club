package com.game;

import java.awt.Canvas;
import java.awt.Dimension;
import javax.swing.JFrame;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;
/**
 * @author evan.gabb
 * @author dleskow
 */
public class Window {

	private Input input;
    
    private Canvas canvas;
    private JFrame frame;
    
    private final int NAT_WIDTH;
    private final int NAT_HEIGHT;
    
    private final int scale;

    public Window(String title, int[] res, int scale) {
        NAT_WIDTH = res[0];
        NAT_HEIGHT = res[1];
        this.scale = scale;
        
        // O Canvas é a tela em branco onde a Engine vai desenhar as entidades
        canvas = new Canvas();
        canvas.setPreferredSize(new Dimension(NAT_WIDTH*scale, NAT_HEIGHT*scale));

        frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(canvas);
        frame.pack(); // Ajusta as bordas
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

		this.input = new Input(scale);
		canvas.addKeyListener(input);
		canvas.addMouseListener(input);
		canvas.addMouseMotionListener(input);
		canvas.setFocusable(true);
		canvas.requestFocus();
    }

    public Canvas getCanvas() {
        return canvas;
    }

	public Input getInput() {
	    return input;
	}

    public int getNatWidth() { return NAT_WIDTH; }
    public int getNatHeight() { return NAT_HEIGHT; }
   
    public Graphics2D beginFrame() {
        BufferStrategy buffers = canvas.getBufferStrategy();
        
        if (buffers == null) {
            canvas.createBufferStrategy(3);
            return null;
        }
        
        Graphics2D g = (Graphics2D) buffers.getDrawGraphics();
        
        g.scale(scale, scale);
        g.setColor(new Color(30, 30, 30));
        g.fillRect(0, 0, NAT_WIDTH, NAT_HEIGHT);
        
        return g;
    }
    
    public void showFrame(Graphics2D g) {
        g.dispose();
        canvas.getBufferStrategy().show();
    }
}
