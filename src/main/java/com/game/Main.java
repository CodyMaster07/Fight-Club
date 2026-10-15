package com.game;

import com.entities.Entity;
import com.system.Ring;
import com.system.Engine;
        
/**
 *
 * @author evan.gabb
 */


        
public class Main {
    public static void main(String[] args) {
        Ring.init(800, 600);

        System.out.println("Ring initialized: " + Ring.instance().getWidth() + "x" + Ring.instance().getHeight());

        Entity player = new Entity(10, 20);
        System.out.println("Player at (" + player.getX() + ", " + player.getY() + ")");
    }
}
