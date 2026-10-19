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
import java.util.HashSet;
import javax.imageio.ImageIO;

public abstract class Entity {

    public static final double maxAngleVar = Math.PI / 12.0;
    
    // Basic attributes
    protected double x;
    protected double y;
    protected double orientation;
    protected final int maskWidth;
    protected final int maskHeight;
    protected double speed;
    private int spriteScale;
    
    private int durability;
    
    private Ring ring;
    
    private int counter;
    private double rand1;
    private double rand2;
    
    protected BufferedImage sprite;

    public Entity(double x, double y, double ort, int maskW, int maskH, double spd, int dur, int spriteScale) {
        if (spriteScale < 1) {
            throw new IllegalArgumentException("A escala deve ser maior ou igual a 1.");
        }
        
        this.spriteScale = spriteScale;
        
        this.x = x;
        this.y = y;
        this.orientation = ort;
        this.maskWidth = maskW * spriteScale;
        this.maskHeight = maskH * spriteScale;
        
        this.speed = spd;
        this.durability = dur;
        
        this.counter = 0;
        this.rand1 = Math.random();
        this.rand2 = Math.random();
        this.ring = Ring.instance();
    }
    
    public Entity(double x, double y, double ort, int maskW, int maskH, double spd, int dur) {
        this(x, y, ort, maskW, maskH, spd, dur, 1);
    }
    
    public Entity(double x, double y, double ort, double spd, String file_path) { // Construtor usado em ball que não carrega spritesheet
        this.spriteScale = 1;
        
        this.x = x;
        this.y = y;
        this.orientation = ort;      
        this.speed = spd;
        this.durability = 1;
        
        this.counter = 0;
        this.rand1 = Math.random();
        this.rand2 = Math.random();
        this.ring = Ring.instance();
        
        loadSprite(file_path);
        this.maskWidth = getRenderWidth();
        this.maskHeight = getRenderHeight();
    }



    protected void loadSprite(String file_path) {
        try {
            URL url = getClass().getResource(file_path);
            if (url == null) {
                System.err.println("Error: sprite not found");
                return;
            }
            
            sprite = ImageIO.read(url);
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

    public int getRenderWidth() {
        return sprite != null ? sprite.getWidth() * spriteScale : maskWidth;
    }

    public int getRenderHeight() {
        return sprite != null ? sprite.getHeight() * spriteScale : maskHeight;
    }
    
    // Setters
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setOrientation(double orientation) { this.orientation = orientation; }
    public void setSpeed(double speed) { this.speed = speed; }
    public void setDurability(int durability) { this.durability = durability; }

    public static double normalizeAng(double angle) {
        return Math.atan2(Math.sin(angle), Math.cos(angle));
//        return angle;
    }

    public void move() {
        double dx = this.speed * Math.cos(this.orientation);
        double dy = this.speed * Math.sin(this.orientation);

        this.x += dx;
        this.y += dy;
    }

    private void checkRingCollision() {
        counter++;
        if (counter >= 20) {
            counter = 0;
            rand1 = Math.random();
            rand2 = Math.random();
        }
        
        double maxX = ring.getX() + ring.getWidth() - this.maskWidth;
        double maxY = ring.getY() + ring.getHeight() - this.maskHeight;

        boolean left = x <= ring.getX();
        boolean right = x >= maxX;
        boolean top = y <= ring.getY();
        boolean bottom = y >= maxY;

        
        double reflect = 0.0;
        double dAngle = ((rand1 > 0.5) ? rand2 * maxAngleVar : -rand2 * maxAngleVar);

        if (right) {
            reflect = (Math.PI*0.5 - this.orientation)*2.0;
            this.x = maxX;
            ring.setCollided(2);
            
        } else if (bottom) {
            reflect = (-Math.PI - this.orientation)*2.0;
            this.y = maxY;
            ring.setCollided(3);
        } else if (left) {
            reflect = (Math.PI*1.5 - this.orientation)*2.0;
            this.x = ring.getX();
            ring.setCollided(4);
        } else if (top) {
            reflect = (Math.PI - this.orientation)*2.0;
            this.y = ring.getY();
            ring.setCollided(1);
        }
        if (right || left || top || bottom)
            setOrientation(normalizeAng(this.orientation + reflect + dAngle));
        
    }
    
    public boolean intersects(Entity other) {
        if (other == null || other == this)
            return false;
        
        double maxX = this.x + this.maskWidth;
        double maxY = this.y + this.maskHeight;
        
        double oMaxX = other.x + other.maskWidth;
        double oMaxY = other.y + other.maskHeight;
        
        return this.x < oMaxX && maxX > other.x && this.y < oMaxY && maxY > other.y; // Script padrão de colisão AABB
    }
    
    public void onCollision(Entity other) {
        // Must be overwritten para cada Fighter
    }
 
    public void tick() {
        move();
        checkRingCollision();
    }
    
    public void render(Graphics g) {
        if (sprite != null) {
            g.drawImage(sprite, (int) x, (int) y, getRenderWidth(), getRenderHeight(), null);
        } else {
            // Se não tiver sprite, renderiza retângulo vermelho
            g.setColor(Color.RED);
            g.fillRect((int) x, (int) y, getRenderWidth(), getRenderHeight());
        }
    }
}
