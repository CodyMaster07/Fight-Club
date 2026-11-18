package com.game;

import java.awt.Canvas;
import java.awt.Dimension;
import javax.swing.JFrame;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;
import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;
/**
 * @author evan.gabb
 * @author dleskow
 */
public class Window {

    private final Input input;
    
    private Canvas canvas;
    private JFrame frame;
    
    private volatile int NAT_WIDTH;
    private volatile int NAT_HEIGHT;
    
    private volatile int scale;

    public Window(String title, int[] res, int scale) {
        NAT_WIDTH = res[0];
        NAT_HEIGHT = res[1];
        this.scale = scale;
        this.input = new Input(scale);
        
        // O Canvas é a tela em branco onde a Engine vai desenhar as entidades
        onEventThread(() -> {
            canvas = new Canvas();
            canvas.setPreferredSize(new Dimension(NAT_WIDTH*scale, NAT_HEIGHT*scale));

            frame = new JFrame(title);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(canvas);
            frame.pack(); // Ajusta as bordas
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            canvas.addKeyListener(input);
            canvas.addMouseListener(input);
            canvas.addMouseMotionListener(input);
            canvas.setFocusable(true);
            canvas.requestFocusInWindow();
        });
    }

    public void configure(int width, int height, int scale) {
        if (width <= 0 || height <= 0 || scale <= 0) {
            throw new IllegalArgumentException("Resolução ou escala inválida");
        }
        onEventThread(() -> {
            NAT_WIDTH = width;
            NAT_HEIGHT = height;
            this.scale = scale;
            input.setFrameScale(scale);
            BufferStrategy buffers = canvas.getBufferStrategy();
            if (buffers != null) {
                buffers.dispose();
            }
            canvas.setPreferredSize(new Dimension(width * scale, height * scale));
            frame.pack();
            frame.setLocationRelativeTo(null);
            canvas.requestFocusInWindow();
        });
    }

    public int getScale() {
        return scale;
    }

    public void close() {
        onEventThread(frame::dispose);
    }

    private static void onEventThread(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(action);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação da janela interrompida", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Não foi possível atualizar a janela", e.getCause());
        }
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
