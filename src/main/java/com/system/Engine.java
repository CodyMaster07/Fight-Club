package com.system;

/**
 *
 * @author evan.gabb
 */

import com.entities.Boxer;
import com.entities.Entity;
import com.entities.Fighter;
import com.entities.Ball;
import com.game.Window;
import com.system.Ring;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
 
public class Engine implements Runnable {
    
    Random random = new Random();
    public static final double fps = 60.0;
    public static final double ns = 1_000_000_000 / fps;
    
    private boolean isRunning = false;
    private Thread gameThread;
    private Window window;
    private Ring ring;
    
    private List<Entity> entities;
    
    public Engine (int[] res, int[] ring, int frameScale, int[] players, int[] initialPos) {
        entities = new ArrayList<>();
        window = new Window("Fight Club Game", res, frameScale);
        
        double[][] posPos = { {100.0, -100.0}, {-100.0, -100.0}, {-100.0, 100.0}, {100.0, 100.0} };
        
        int ringWidth = ring[0];
        int ringHeight = ring[1];
        
        double ringX = (window.getNatWidth() - ringWidth) / 2.0;
        double ringY = (window.getNatHeight() - ringHeight) / 2.0;
        
        double centerX = window.getNatWidth() / 2.0;
        double centerY = window.getNatHeight() / 2.0;
        
        this.ring = Ring.init(ringX, ringY, ringWidth, ringHeight);
        
        for (int i = 0; i < initialPos.length; i++) {
            if (initialPos[i] == 0)
                initialPos[i] = random.nextInt(1, 5);
        }
        
        int n_entity = 0;
        for (int i = 0; i < players[0]; i++) {
            String sprite_path = (Math.random() > 0.5) ? "/res/ballRed.png" : "/res/ballBlue.png";
            entities.add(new Ball(centerX + posPos[(initialPos[i] - 1)][0], centerY + posPos[(initialPos[i] - 1)][1],  Math.PI / 4.0, 5.0 , sprite_path ));
        }
        for (int i = 0; i < players[1]; i++) {
            entities.add(new Boxer(centerX + posPos[(initialPos[i] - 1)][0], centerY + posPos[(initialPos[i] - 1)][1], Math.PI / 4.0));
        }
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
    
    // Chamada somente se já houver sobreposição
    private void physicalCollision (Entity a, Entity b) {
        double distX = b.getX() + (b.getMaskWidth() /2.0) - (a.getX() + (a.getMaskWidth() /2.0));
        double distY = b.getY() + (b.getMaskHeight()/2.0) - (a.getY() + (a.getMaskHeight()/2.0));
        
        double overlapX = (a.getMaskWidth()  + b.getMaskWidth())  / 2.0 - Math.abs(distX);
        double overlapY = (a.getMaskHeight() + b.getMaskHeight()) / 2.0 - Math.abs(distY);
        double aReflect = 0.0, adAngle = 0.0, bReflect = 0.0, bdAngle = 0.0;
        
        double vax, vay, vbx, vby;

        if (overlapX < overlapY) { // Está colidindo no eixo X, pois o outro eixo deve estar alinhado
            double dirA2B = (distX >= 0) ? 1.0 : -1.0;
            double correction = overlapX / 2.0 + 0.001;
            a.setX(a.getX() - dirA2B * correction);
            b.setX(b.getX() + dirA2B * correction);
            vay = a.getSpeed() * Math.sin(a.getOrientation());
            vby = b.getSpeed() * Math.sin(b.getOrientation());
            // Swaps = collision of equal mass
            vbx = a.getSpeed() * Math.cos(a.getOrientation());
            vax = b.getSpeed() * Math.cos(b.getOrientation());
        } else {
            double dirA2B = (distY >= 0) ? 1.0 : -1.0;
            double correction = overlapY / 2.0 + 0.001;
            a.setY(a.getY() - dirA2B * correction);
            b.setY(b.getY() + dirA2B * correction);
            vax = a.getSpeed() * Math.cos(a.getOrientation());
            vbx = b.getSpeed() * Math.cos(b.getOrientation());
            // Swaps = collision of equal mass
            vby = a.getSpeed() * Math.sin(a.getOrientation());
            vay = b.getSpeed() * Math.sin(b.getOrientation());
        }
        a.setSpeed(Math.hypot(vax, vay));
        b.setSpeed(Math.hypot(vbx, vby));
        aReflect = Math.atan2(vay, vax);
        bReflect = Math.atan2(vby, vbx);
        adAngle = ((Math.random() > 0.5) ? Math.random() * Entity.maxAngleVar : -Math.random() * Entity.maxAngleVar);
        bdAngle = ((Math.random() > 0.5) ? Math.random() * Entity.maxAngleVar : -Math.random() * Entity.maxAngleVar);
        a.setOrientation(Entity.normalizeAng(aReflect + adAngle));
        b.setOrientation(Entity.normalizeAng(bReflect + bdAngle));
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

    
