package com.entities;
import com.entities.Entity;
import com.system.Ring;
import com.system.save;
/**
 *
 * @author evan.gabb
 * @author dleskow
 */

public class Bomb extends Entity {

    private static final long serialVersionUID = 1L;
    public Bomb (double x, double y, double ort, double spd, String file_path) {
        super(x, y, ort, spd, file_path);
    }

    public Bomb(save.EntityState state, Ring ring) {
        super(state, ring);
    }
}
