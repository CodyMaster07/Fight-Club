package com.entities;
import com.entities.Entity;
import com.system.Ring;
import com.system.save;
/**
 * @author evan.gabb
 * @author dleskow
 */

public class Ball extends Entity {
    public Ball (double x, double y, double ort, double spd, String file_path) {
        super(x, y, ort, spd, file_path);
    }

    public Ball(save.EntityState state, Ring ring) {
        super(state, ring);
    }
}
