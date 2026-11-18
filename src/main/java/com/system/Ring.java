package com.system;

/**
 * @author evan.gabb
 * @author dleskow
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.util.Objects;

public class Ring { // Singleton of the Ring

    private static volatile Ring instance; // Volatile is a thread-safe way to guarantee all threads read the updated data

    private double x;
    private double y;
    
    private final int ogX;
    private final int ogY;
    
    private final int width;
    private final int height;
    
    private int hasCollided;
    
    private int counter;

    private Ring(double x, double y, int wid, int hei) {
        this.x = x;
        this.y = y;
        this.ogX = (int) x;
        this.ogY = (int) y;
        this.width = wid;
        this.height = hei;
        this.counter = 0;
        this.hasCollided = 0;
    }

    private Ring(save.RingState state) {
        this.x = state.x();
        this.y = state.y();
        this.ogX = state.originalX();
        this.ogY = state.originalY();
        this.width = state.width();
        this.height = state.height();
        this.hasCollided = state.collidedSide();
        this.counter = state.counter();
    }

    public static Ring fromState(save.RingState state) {
        return new Ring(state);
    }

    public save.RingState captureState() {
        return new save.RingState(x, y, ogX, ogY, width, height, hasCollided, counter);
    }

    public static synchronized Ring newGame(double x, double y, int width, int height) {
        instance = new Ring(x, y, width, height);
        return instance;
    }

    public static synchronized Ring init(double x, double y, int wid, int hei) {
        if (instance == null) {
            instance = new Ring(x, y, wid, hei);
        }
        return instance;
    }

    public static Ring instance() {
        if (instance == null) {
            throw new IllegalStateException("Ring is not initialized. Call Ring.init(width, height) first.");
        }
        return instance;
    }

    public static synchronized void restoreInstance(Ring restored) {
        instance = Objects.requireNonNull(restored);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
    
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
    
    public int getHasCollided() {
        return hasCollided;
    }

    public void setCollided(int neoCollided) {
        hasCollided = neoCollided;
    }
    
    public void tick() {
        if (hasCollided > 0) {
            if (counter < 2) {
                switch (hasCollided) {
                    case 1 -> this.y -= 1.0;
                    case 2 -> this.x += 1.0;
                    case 3 -> this.y += 1.0;
                    case 4 -> this.x -= 1.0;
                }
            }
            counter++;
            if (counter >= 10) {
                counter = 0;
                hasCollided = 0;
                this.x = ogX;
                this.y = ogY;
            }
        }
    }
    
    public void render(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(10));
        g2d.drawRect((int) x, (int) y, width, height);
    }
}
