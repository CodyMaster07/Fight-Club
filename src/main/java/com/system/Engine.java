package com.system;

/**
 * @author evan.gabb
 * @author dleskow
 */

import com.entities.Boxer;
import com.entities.Entity;
import com.entities.Fighter;
import com.game.Window;
import com.game.Input;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import com.entities.Bomber;
import com.entities.Team;
 
public class Engine implements Runnable {
    
    private Window window;
    private GameRandom random = new GameRandom();
    public static final double fps = 60.0;
    public static final double ns = 1_000_000_000 / fps;
    
    private volatile boolean isRunning = false;
    private volatile boolean exitRequested;
    private long quitAtNanos;
    private String statusMessage;
    private long statusUntilNanos;
    private Thread gameThread;
    private Ring ring;
    
    private List<Entity> entities;
    private Fighter redFighter;
    private Fighter blueFighter;

    public Engine (Window window, int[] res, int[] ring, int frameScale, int[] players, int[] initialPos) {
	this.window = window;
    
    
        entities = new ArrayList<>();
        
        double[][] posPos = { {100.0, -100.0}, {-100.0, -100.0}, {-100.0, 100.0}, {100.0, 100.0} };
        
        int ringWidth = ring[0];
        int ringHeight = ring[1];
        
        double ringX = (window.getNatWidth() - ringWidth) / 2.0;
        double ringY = (window.getNatHeight() - ringHeight) / 2.0;
        
        double centerX = window.getNatWidth() / 2.0;
        double centerY = window.getNatHeight() / 2.0;
        
        double fighterWidth = 34 * 2;
        double fighterHeight = 34 * 2;
        double y = ringY + (ringHeight - fighterHeight) / 2.0;
        double leftX = ringX + (ringWidth - fighterWidth) * 0.25;
        double rightX = ringX + (ringWidth - fighterWidth) * 0.75;
        
        this.ring = Ring.newGame(ringX, ringY, ringWidth, ringHeight);
        
        for (int i = 0; i < initialPos.length; i++) {
            if (initialPos[i] == 0)
                initialPos[i] = random.nextInt(1, 5);
        }
        
//        int n_entity = 0;
//        for (int i = 0; i < players[0]; i++) {
//            String sprite_path = (Math.random() > 0.5) ? "/res/ballRed.png" : "/res/ballBlue.png";
//            entities.add(new Ball(centerX + posPos[(initialPos[i] - 1)][0], centerY + posPos[(initialPos[i] - 1)][1],  Math.PI / 4.0, 5.0 , sprite_path ));
//        }
//        for (int i = 0; i < players[1]; i++) {
//            entities.add(new Boxer(centerX + posPos[(initialPos[i] - 1)][0], centerY + posPos[(initialPos[i] - 1)][1], Math.PI / 4.0));
//        }
        
        setActiveFighter(new Boxer(leftX, y, Math.PI / 4.0, Team.RED));
        setActiveFighter(new Bomber(rightX, y, -3.0 * Math.PI / 4.0, Team.BLUE));
    }

    public Engine(Window window, save state) {
        Ring restoredRing = Ring.fromState(state.ring());
        List<Entity> restoredEntities = state.restoreEntities(restoredRing);
        window.configure(state.screenWidth(), state.screenHeight(), state.frameScale());

        this.window = window;
        this.ring = restoredRing;
        this.entities = restoredEntities;
        this.redFighter = state.redFighterIndex() < 0 ? null
                : (Fighter) entities.get(state.redFighterIndex());
        this.blueFighter = state.blueFighterIndex() < 0 ? null
                : (Fighter) entities.get(state.blueFighterIndex());
        this.random = GameRandom.fromState(state.randomState());
        Ring.restoreInstance(restoredRing);
    }

    /** Capture somente na thread do jogo ou enquanto a Engine estiver parada. */
    public save captureState() {
        return save.capture(window.getNatWidth(), window.getNatHeight(), window.getScale(),
                ring, entities, redFighter, blueFighter, random);
    }

    public boolean isExitRequested() {
        return exitRequested;
    }

    private void saveAndQuit() {
        try {
            captureState().write(save.DEFAULT_FILE);
            statusMessage = "Jogo salvo";
            quitAtNanos = System.nanoTime() + 1_000_000_000L;
            exitRequested = true;
        } catch (IOException | RuntimeException e) {
            statusMessage = "Não foi possível salvar. Tente Q novamente.";
            statusUntilNanos = System.nanoTime() + 4_000_000_000L;
            e.printStackTrace();
        }
    }
    
    public synchronized void start() {
        if (gameThread != null && gameThread.isAlive())
            return;
        if (exitRequested) {
            throw new IllegalStateException("Esta execução já foi encerrada");
        }
        window.getInput().reset();
        isRunning = true;
        gameThread = new Thread(this, "game-loop");
        gameThread.start();
    }
    public void stop() {
        isRunning = false;
        awaitFinish();
    }
    // Let's mainMenu waiting for game to finish
	public void awaitFinish() {
	    if (gameThread != null && Thread.currentThread() != gameThread) {
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
        adAngle = ((random.nextDouble() > 0.5) ? random.nextDouble() * Entity.maxAngleVar : -random.nextDouble() * Entity.maxAngleVar);
        bdAngle = ((random.nextDouble() > 0.5) ? random.nextDouble() * Entity.maxAngleVar : -random.nextDouble() * Entity.maxAngleVar);
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
    
    @Override 
    public void run() {
        // Configuração para ter 60 ticks por segundo (60 FPS)
        long lastTime = System.nanoTime();
        
        double delta = 0;
        
        try {
            while (isRunning) {
                long now = System.nanoTime();
                delta += (now - lastTime) / ns;
                lastTime = now;
                while (delta >= 1 && isRunning) {
                    tick();
                    render();
                    delta--;
                }
                LockSupport.parkNanos(1_000_000L);
            }
        } finally {
            isRunning = false;
        }
    }
    

    public void tick() {
        Input input = window.getInput();
        boolean quit;
        boolean backToMenu;
        synchronized (input) {
            quit = input.isKeyJustPressed(KeyEvent.VK_Q);
            backToMenu = input.isKeyJustPressed(KeyEvent.VK_ESCAPE);
            input.update();
        }

        // Depois do save, mantém exatamente a posição salva durante a mensagem.
        if (exitRequested) {
            if (System.nanoTime() >= quitAtNanos) {
                isRunning = false;
            }
            return;
        }
        if (quit) {
            saveAndQuit();
            return;
        }
        if (backToMenu) {
            isRunning = false;
            return;
        }
        
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
        
        if (redFighter != null && redFighter.isKO()) {
            redFighter = null;
        }
        if (blueFighter != null && blueFighter.isKO()) {
            blueFighter = null;
        }
        
        updateFacing();
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
            if (statusMessage != null
                    && (exitRequested || System.nanoTime() < statusUntilNanos)) {
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
                FontMetrics metrics = g.getFontMetrics();
                int textWidth = metrics.stringWidth(statusMessage);
                int textX = (window.getNatWidth() - textWidth) / 2;
                int textY = window.getNatHeight() / 2;
                g.setColor(new Color(0, 0, 0, 190));
                g.fillRect(textX - 12, textY - metrics.getAscent() - 8,
                        textWidth + 24, metrics.getHeight() + 16);
                g.setColor(Color.WHITE);
                g.drawString(statusMessage, textX, textY);
            }
        } finally {
            window.showFrame(g);
        }
    }
}

    
