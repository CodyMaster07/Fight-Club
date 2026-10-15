package com.entities;

/**
 *
 * @author evan.gabb
 */
import com.system.Ring;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

public abstract class Entity {

    // Basic attributes
    private double x;
    private double y;
    private double orientation;
    private int maskWidth;
    private int maskHeight;
    private double speed;
    private int durability;
    
    protected BufferedImage sprite;

    public Entity(double x, double y) {
        this(x, y, 0.0, 10, 10, 0.0, 1);
    }

    public Entity(double x, double y, double ort, int maskW, int maskH, double spd, int dur) {
        this.x = x;
        this.y = y;
        this.orientation = ort;
        this.maskWidth = maskW;
        this.maskHeight = maskH;
        this.speed = spd;
        this.durability = dur;
    }

    protected void loadSprite(String file_path) {
        try {
            URL url = getClass().getResource(file_path);
            if (url == null) {
                System.err.println("Error: sprite not found");
                return;
            }
            
            sprite = ImageIO.read(url);
            this.maskWidth = sprite.getWidth();
            this.maskHeight = sprite.getHeight();
        } catch (IOException e) {
            System.err.println("Fail to read sprite file.");
            e.printStackTrace();
        }
    }
    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getOrientation() { return orientation; }
    public int getMaskWidth() { return maskWidth; }
    public int getMaskHeight() { return maskHeight; }
    public double getSpeed() { return speed; }
    public int getDurability() { return durability; }

    // Setters
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setOrientation(double orientation) { this.orientation = orientation; }
    public void setMaskWidth(int maskWidth) { this.maskWidth = maskWidth; }
    public void setMaskHeight(int maskHeight) { this.maskHeight = maskHeight; }
    public void setSpeed(double speed) { this.speed = speed; }
    public void setDurability(int durability) { this.durability = durability; }

    public static double normalizeAng(double angle) {
        return Math.atan2(Math.sin(angle), Math.cos(angle));
    }

    public void move() {
        double dx = this.speed * Math.cos(this.orientation);
        double dy = this.speed * Math.sin(this.orientation);

        this.x += dx;
        this.y += dy;
    }

    private void checkRingCollision() {
        Ring ring = Ring.instance();

        if (this.x + this.maskWidth >= ring.getWidth()
                || this.y + this.maskHeight >= ring.getHeight()
                || this.x <= 0.0
                || this.y <= 0.0) {
            double maxAngle = Math.PI / 12.0;
            double dAngle = Math.PI + ((Math.random() > 0.5) ? Math.random() * maxAngle : -Math.random() * maxAngle);
            setOrientation(normalizeAng(this.orientation + dAngle));
        }
    }
    
    public void tick() {
        checkRingCollision();
        move();
    }
    
    public void render(Graphics g) {
        if (sprite != null) {
            g.drawImage(sprite, (int) x, (int) y, null);
        } else {
            // Se não tiver sprite, renderiza retângulo vermelho
            g.setColor(Color.RED);
            g.fillRect((int) x, (int) y, maskWidth, maskHeight);
        }
    }
}