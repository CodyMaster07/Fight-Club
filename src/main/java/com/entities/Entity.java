package com.entities;

/**
 *
 * @author evan.gabb
 */
import com.system.Ring;

public class Entity {

    // Basic attributes
    private double x;
    private double y;
    private double orientation;
    private double maskWidth;
    private double maskHeight;
    private double speed;
    private int durability;

    public Entity(double x, double y) {
        this(x, y, 0.0, 10.0, 10.0, 0.0, 1);
    }

    public Entity(double x, double y, double ort, double maskW, double maskH, double spd, int dur) {
        this.x = x;
        this.y = y;
        this.orientation = ort;
        this.maskWidth = maskW;
        this.maskHeight = maskH;
        this.speed = spd;
        this.durability = dur;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getOrientation() {
        return orientation;
    }

    public double getMaskWidth() {
        return maskWidth;
    }

    public double getMaskHeight() {
        return maskHeight;
    }

    public double getSpeed() {
        return speed;
    }

    public int getDurability() {
        return durability;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setOrientation(double orientation) {
        this.orientation = orientation;
    }

    public void setMaskWidth(double maskWidth) {
        this.maskWidth = maskWidth;
    }

    public void setMaskHeight(double maskHeight) {
        this.maskHeight = maskHeight;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void setDurability(int durability) {
        this.durability = durability;
    }

    public static double normalizeAng(double angle) {
        return Math.atan2(Math.sin(angle), Math.cos(angle));
    }

    public void tick() {
        checkRingCollision();
        move();
    }

    protected void move() {
        double dx = this.speed * Math.cos(this.orientation);
        double dy = this.speed * Math.sin(this.orientation);

        this.x += dx;
        this.y += dy;
    }

    private void checkRingCollision() {
        Ring ring = Ring.instance();

        if (this.x + this.maskWidth >= ring.getWidth()
                && this.y + this.maskHeight >= ring.getHeight()
                && this.x <= 0.0
                && this.y <= 0.0) {
            double maxAngle = Math.PI / 12.0;
            double dAngle = Math.PI + ((Math.random() > 0.5) ? Math.random() * maxAngle : -Math.random() * maxAngle);
            setOrientation(normalizeAng(this.orientation + dAngle));
        }
    }
}