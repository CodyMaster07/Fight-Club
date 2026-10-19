package com.system;

/**
 *
 * @author evan.gabb
 */

import com.entities.Boxer;
import com.entities.Entity;
import com.entities.Fighter;
import com.game.Window;
import com.system.Ring;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
 
public class Engine implements Runnable {
    
    public static final double fps = 60.0;
    public static final double ns = 1000000000 / fps;
    
    private boolean isRunning = false;
    private Thread gameThread;
    private Window window;
    private Ring ring;
    
    private List<Entity> entities;
    
    public Engine(int frameScale) {
        entities = new ArrayList<>();
        window = new Window("Fight Club Game", frameScale);
        
        int ringWidth = 500;
        int ringHeight = 500;
        
        double ringX = (window.getNatWidth() - ringWidth) / 2.0;
        double ringY = (window.getNatHeight() - ringHeight) / 2.0;
        
        double centerX = window.getNatWidth() / 2.0;
        double centerY = window.getNatHeight() / 2.0;
        
        ring = Ring.init(ringX, ringY, ringWidth, ringHeight);
        
        entities.add(new Boxer(centerX + 50,centerY + 50, Math.PI / 4.0));
        entities.add(new Boxer(centerX - 50,centerY - 50, -Math.PI / 4.0));
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
    
    private void checkEntitiesCollision() {
        for (int i = 0; i < entities.size(); i++) {
            Entity a = entities.get(i);
            for (int j = i + 1; j < entities.size(); j++) {
                Entity b = entities.get(j);
                if (a.intersects(b)) {
                    physicalCollision(a, b); 
                    // System.out.println("Tocou");
                    a.onCollision(b);
                    b.onCollision(a);
                }
            }
        }
    }
    
    private void physicalCollision(Entity a, Entity b) {
        double distX = b.getX() + (b.getMaskWidth()/2.0) - (a.getX() + (a.getMaskWidth()/2.0));
        double distY = b.getY() + (b.getMaskHeight()/2.0) - (a.getY() + (a.getMaskHeight()/2.0));
        
        double overlapX = (a.getMaskWidth() + b.getMaskWidth()) / 2.0 - Math.abs(distX);
        double overlapY = (a.getMaskHeight() + b.getMaskHeight()) / 2.0 - Math.abs(distY);
        double aReflect = 0.0, adAngle = 0.0, bReflect = 0.0, bdAngle = 0.0;
        
        if (overlapX < overlapY) { // Está colidindo no eixo X
            double dirA2B = (distX >= 0) ? 1.0 : -1.0;
            double correction = overlapX / 2.0 + 0.001;

            a.setX(a.getX() - dirA2B * correction);
            b.setX(b.getX() + dirA2B * correction);
            
            double relVelX = b.getSpeed()* Math.cos(b.getOrientation()) - a.getSpeed()* Math.cos(a.getOrientation());
            if (relVelX * dirA2B < 0.0) { // Realmente estão se aproximando e tem que mudar os ângulos        
                if (distX >= 0) { // a fazendo colisão right e b fazendo colisão left
                    aReflect = (Math.PI*0.5 - a.getOrientation())*2.0;
                    bReflect = (Math.PI*1.5 - b.getOrientation())*2.0;
                } else { // a fazendo colisão left e b fazendo colisão right
                    aReflect = (Math.PI*1.5 - a.getOrientation())*2.0;
                    bReflect = (Math.PI*0.5 - b.getOrientation())*2.0;
                }
            }
        } else {
            double dirA2B = (distY >= 0) ? 1.0 : -1.0;
            double correction = overlapY / 2.0 + 0.001;

            a.setY(a.getY() - dirA2B * correction);
            b.setY(b.getY() + dirA2B * correction);
            
            double relVelY = b.getSpeed()* Math.sin(b.getOrientation()) - a.getSpeed()* Math.sin(a.getOrientation());
            if (relVelY * dirA2B < 0.0) { // Realmente estão se aproximando e tem que mudar os ângulos       
                if (distY >= 0) { // a fazendo colisão bottom e b fazendo colisão top
                    aReflect = (-Math.PI - a.getOrientation())*2.0;
                    bReflect = (Math.PI - b.getOrientation())*2.0;
                } else { // a fazendo colisão top e b fazendo colisão bottom
                    aReflect = (Math.PI - a.getOrientation())*2.0;
                    bReflect = (-Math.PI - b.getOrientation())*2.0;
                }
            }
        }
        adAngle = ((Math.random() > 0.5) ? Math.random() * Entity.maxAngleVar : -Math.random() * Entity.maxAngleVar);
        bdAngle = ((Math.random() > 0.5) ? Math.random() * Entity.maxAngleVar : -Math.random() * Entity.maxAngleVar);
        a.setOrientation(Entity.normalizeAng(a.getOrientation() + aReflect + adAngle));
        b.setOrientation(Entity.normalizeAng(b.getOrientation() + bReflect + bdAngle));
    }
    
    @Override 
    public void run() {
        // Configuração para ter 60 ticks por segundo (60 FPS)
        long lastTime = System.nanoTime();
        
        double delta = 0;
        
        while (isRunning) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            while (delta >= 1) {
                tick();
                render();
                delta--;
            }
        }
        stop();
    }
    
    public void tick() {
        for (int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            e.tick();
        }
        checkEntitiesCollision(); // Aqui os danos são registrados mas ficam pendentes


        for (Entity entity : entities) {
            if (entity instanceof Fighter fighter) {
                fighter.applyPendingDamage();
            }
        }

        entities.removeIf(entity -> entity instanceof Fighter fighter && fighter.isKO());
        ring.tick();
    }
    
    public void render() {
        Graphics2D g = window.beginFrame();
        
        if (g == null)
            return;
        
        try {
            ring.render(g);
            
            for (Entity entity : entities)
                entity.render(g);
        } finally {
            window.showFrame(g);
        }
    }
}

    