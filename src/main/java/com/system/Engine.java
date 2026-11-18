package com.system;

/**
- @author evan.gabb
- @author dleskow
 */

import com.entities.Bomb;
import com.entities.Bomber;
import com.entities.Boxer;
import com.entities.Entity;
import com.entities.Fighter;
import com.entities.Team;
import com.game.Input;
import com.game.Window;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
 
public class Engine implements Runnable {
    
    private static Engine currentInstance;
    
    private Window window;
    Random random = new Random();
    public static final double fps = 60.0;
    public static final double ns = 1_000_000_000 / fps;
    
    private boolean isRunning = false;
    private Thread gameThread;
    private Ring ring;
    
    private List<Entity> entities;
    private final List<Entity> pendingEntities = new ArrayList<>();
    private Fighter redFighter;
    private Fighter blueFighter;

    public Engine(Window window, int[] res, int[] ring, int frameScale, int[] players, int[] initialPos) {
        currentInstance = this;
        this.window = window;
    
        entities = new ArrayList<>();
        
        int ringWidth = ring[0];
        int ringHeight = ring[1];
        
        double ringX = (window.getNatWidth() - ringWidth) / 2.0;
        double ringY = (window.getNatHeight() - ringHeight) / 2.0;
        
        double fighterWidth = 34 * 2;
        double fighterHeight = 34 * 2;
        double y = ringY + (ringHeight - fighterHeight) / 2.0;
        double leftX = ringX + (ringWidth - fighterWidth) * 0.25;
        double rightX = ringX + (ringWidth - fighterWidth) * 0.75;
        
        this.ring = Ring.init(ringX, ringY, ringWidth, ringHeight);
        
        for (int i = 0; i < initialPos.length; i++) {
            if (initialPos[i] == 0)
                initialPos[i] = random.nextInt(1, 5);
        }
        
        setActiveFighter(new Boxer(leftX, y, Math.PI / 4.0, Team.RED));
        setActiveFighter(new Bomber(rightX, y, -3.0 * Math.PI / 4.0, Team.BLUE));
    }

    public static Engine instance() {
        return currentInstance;
    }

    public void spawnEntity(Entity entity) {
        if (entity != null) {
            pendingEntities.add(entity);
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
        if (!isRunning) return;
        isRunning = false;
        if (Thread.currentThread() != gameThread && gameThread != null) {
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void awaitFinish() {
        if (gameThread != null) {
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void checkEntitiesCollision() {
        for (int i = 0; i < entities.size(); i++) {
            Entity a = entities.get(i);
            for (int j = i + 1; j < entities.size(); j++) {
                Entity b = entities.get(j);
                if (a.intersects(b)) {
                    if (!(a instanceof Bomb) && !(b instanceof Bomb)) {
                        a.setImpactSpeed(a.getSpeed());
                        b.setImpactSpeed(b.getSpeed());
                        physicalCollision(a, b);
                    }
                    a.onCollision(b);
                    b.onCollision(a);
                }
            }
        }
    }
    
    // Chamada somente se já houver sobreposição
    private void physicalCollision(Entity a, Entity b) {
        double distX = b.getX() + (b.getMaskWidth() / 2.0) - (a.getX() + (a.getMaskWidth() / 2.0));
        double distY = b.getY() + (b.getMaskHeight() / 2.0) - (a.getY() + (a.getMaskHeight() / 2.0));
        
        double overlapX = (a.getMaskWidth() + b.getMaskWidth()) / 2.0 - Math.abs(distX);
        double overlapY = (a.getMaskHeight() + b.getMaskHeight()) / 2.0 - Math.abs(distY);
        double aReflect = 0.0, adAngle = 0.0, bReflect = 0.0, bdAngle = 0.0;
        
        double vax, vay, vbx, vby;

        if (overlapX < overlapY) {
            double dirA2B = (distX >= 0) ? 1.0 : -1.0;
            double correction = overlapX / 2.0 + 0.001;
            a.setX(a.getX() - dirA2B * correction);
            b.setX(b.getX() + dirA2B * correction);
            vay = a.getSpeed() * Math.sin(a.getOrientation());
            vby = b.getSpeed() * Math.sin(b.getOrientation());
            vbx = a.getSpeed() * Math.cos(a.getOrientation());
            vax = b.getSpeed() * Math.cos(b.getOrientation());
        } else {
            double dirA2B = (distY >= 0) ? 1.0 : -1.0;
            double correction = overlapY / 2.0 + 0.001;
            a.setY(a.getY() - dirA2B * correction);
            b.setY(b.getY() + dirA2B * correction);
            vax = a.getSpeed() * Math.cos(a.getOrientation());
            vbx = b.getSpeed() * Math.cos(b.getOrientation());
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
    
    private void setActiveFighter(Fighter next) {
        if (next == null || next.isKO()) {
            throw new IllegalArgumentException("O lutador deve estar vivo");
        }

        if (next.getTeam() == Team.RED) {
            entities.remove(redFighter);
            redFighter = next;
        } else {
            entities.remove(blueFighter);
            blueFighter = next;
        }

        entities.add(next);
        updateFacing();
    }

    private void updateFacing() {
        if (redFighter == null || blueFighter == null) {
            return;
        }

        redFighter.faceOpponent(blueFighter);
        blueFighter.faceOpponent(redFighter);
    }

    private void handlePlayerControls(Input input) {
        // Blue Fighter: WASD
        if (blueFighter != null && !blueFighter.isKO()) {
            if (input.isKeyDown(KeyEvent.VK_A)) {
                blueFighter.rotateAim(-Fighter.AIM_ROTATION_SPEED);
            }
            if (input.isKeyDown(KeyEvent.VK_D)) {
                blueFighter.rotateAim(Fighter.AIM_ROTATION_SPEED);
            }
            if (input.isKeyJustPressed(KeyEvent.VK_W)) {
                blueFighter.usePrimaryAbility();
            }
        }

        // Red Fighter: Arrow Keys
        if (redFighter != null && !redFighter.isKO()) {
            if (input.isKeyDown(KeyEvent.VK_LEFT)) {
                redFighter.rotateAim(-Fighter.AIM_ROTATION_SPEED);
            }
            if (input.isKeyDown(KeyEvent.VK_RIGHT)) {
                redFighter.rotateAim(Fighter.AIM_ROTATION_SPEED);
            }
            if (input.isKeyJustPressed(KeyEvent.VK_UP)) {
                redFighter.usePrimaryAbility();
            }
        }
    }
    
    @Override 
    public void run() {
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
        Input input = window.getInput();
    
        if (input.isKeyJustPressed(KeyEvent.VK_ESCAPE)) {
            isRunning = false;
            return;
        }

        handlePlayerControls(input);
        
        for (int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            e.tick();
        }
        checkEntitiesCollision();

        for (Entity entity : entities) {
            if (entity instanceof Fighter fighter) {
                fighter.applyPendingDamage();
            }
        }

        entities.removeIf(entity -> entity instanceof Fighter fighter && fighter.isKO());
        entities.removeIf(entity -> entity instanceof Bomb bomb && bomb.isExploded());

        if (!pendingEntities.isEmpty()) {
            entities.addAll(pendingEntities);
            pendingEntities.clear();
        }
        
        if (redFighter != null && redFighter.isKO()) {
            redFighter = null;
        }
        if (blueFighter != null && blueFighter.isKO()) {
            blueFighter = null;
        }
        
        updateFacing();
        ring.tick();
        input.update();
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
