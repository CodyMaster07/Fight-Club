package com.system;

/**
 *
 * @author evan.gabb
 */

import com.entities.Entity;
import com.entities.Fighter;
import com.game.Window;
import com.system.Ring;
import static com.system.Ring.init;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;
 
public class Engine implements Runnable {
    
    private boolean isRunning = false;
    public static int scale;
    private Thread gameThread;
    private Window window;
    private Ring ring;
    
    private List<Entity> entities;
    
    public Engine(int frameScale) {
        entities = new ArrayList<>();
        
        scale = frameScale;
        window = new Window("Fight Club Game", scale);
        
        ring = init(window.getWidth(), window.getHeight());
        
        entities.add(new Fighter(10.0,10.0, 2, -1.5, "/res/ganesh.png"));
        entities.add(new Fighter(20.0,10.0, 1, 1.2, "/res/ganeh.png"));
    }
    
    public synchronized void start() {
        if (isRunning)
            return;
        isRunning = true;
        gameThread = new Thread(this);
        gameThread.start();
    }
    
    public synchronized void stop() {
        isRunning = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    @Override 
    public void run() {
        // Configuração para ter 60 ticks por segundo (60 FPS)
        long lastTime = System.nanoTime();
        double tps = 60.0;
        double ns = 1000000000 / tps;
        double delta = 0;
        
        while (isRunning) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            while (delta >= 1) {
                tick();
                delta--;
            }
            
            render();
        }
        stop();
    }
    
    public void tick() {
        for (int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            e.tick();
        }
    }
    
    public void render() {
        BufferStrategy bs = window.getCanvas().getBufferStrategy();
        if (bs == null) {
            window.getCanvas().createBufferStrategy(3); // BufferStartegy com 3 buffers, para ficar liso
            return;
        }       
        
        Graphics g = bs.getDrawGraphics();
        
        g.setColor(new Color(30, 30, 30));
        g.fillRect(0, 0, window.getWidth(), window.getHeight());
        
        for (int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            e.render(g);
        }

        g.dispose();
        bs.show();
    }
}

    