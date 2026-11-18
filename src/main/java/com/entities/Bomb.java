package com.entities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class Bomb extends Entity {

    private static final int MASK_WIDTH = 18;
    private static final int MASK_HEIGHT = 18;
    private static final int ARM_TICKS = 30; // 0.5s at 60 FPS
    public static final int EXPLOSION_DAMAGE = 75;

    private int armTicksRemaining = ARM_TICKS;
    private boolean exploded = false;

    public Bomb(double x, double y) {
        super(x, y, 0.0, MASK_WIDTH, MASK_HEIGHT, 0.0, 1);
    }

    public boolean isArmed() {
        return armTicksRemaining <= 0;
    }

    public boolean isExploded() {
        return exploded;
    }

    @Override
    public void tick() {
        if (armTicksRemaining > 0) {
            armTicksRemaining--;
        }
    }

    @Override
    public void onCollision(Entity other) {
        if (exploded || !isArmed()) {
            return;
        }

        if (other instanceof Fighter target && !target.isKO()) {
            target.takeDamage(EXPLOSION_DAMAGE);
            exploded = true;
        }
    }

    @Override
    public void render(Graphics g) {
        if (exploded) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = (int) Math.round(getX());
            int y = (int) Math.round(getY());
            int w = getMaskWidth();
            int h = getMaskHeight();

            boolean armed = isArmed();

            g2.setColor(armed ? new Color(30, 30, 30) : new Color(90, 90, 90, 160));
            g2.fillOval(x, y, w, h);

            g2.setColor(armed ? new Color(255, 60, 40) : new Color(255, 200, 60, 180));
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawOval(x + 1, y + 1, w - 2, h - 2);

            if (armed) {
                g2.fillOval(x + w / 2 - 3, y + h / 2 - 3, 6, 6);
            }
        } finally {
            g2.dispose();
        }
    }
}
