package com.entities;

/**
 *
 * @author evan.gabb
 */
public class Fighter extends Entity {
    public Fighter(double x, double y, double spd, double ort, String sprite_path) {
        super(x, y);
        
        loadSprite(sprite_path);
        
        this.setSpeed(spd);
        this.setOrientation(ort);
    }
}
