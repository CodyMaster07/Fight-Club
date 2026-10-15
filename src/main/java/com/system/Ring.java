package com.system;

/**
 *
 * @author evan.gabb
 */
public class Ring { // Singleton of the Ring

    private static volatile Ring instance; // Volatile is a thread-safe way to guarantee all threads read the updated data

    private final int width;
    private final int height;

    private Ring(int wid, int hei) {
        this.width = wid;
        this.height = hei;
    }

    public static synchronized Ring init(int wid, int hei) {
        if (instance == null) {
            instance = new Ring(wid, hei);
        }
        return instance;
    }

    public static Ring instance() {
        if (instance == null) {
            throw new IllegalStateException("Ring is not initialized. Call Ring.init(width, height) first.");
        }
        return instance;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}