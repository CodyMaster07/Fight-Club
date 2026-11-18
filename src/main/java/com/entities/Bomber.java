package com.entities;

/**
 *
 * @author evan.gabb
 */
public class Bomber extends Fighter {
    private static final int DEFAULT_DURABILITY = 80;
    private static final int MASK_WIDTH = 24;
    private static final int MASK_HEIGHT = 34;
    private static final double DEFAULT_SPEED = 4.0;

    private static final int PUNCH_DAMAGE = 20;
    private static final int TICKS_PER_FRAME = 4;
    private static final int[] PUNCH_ANIMATION_FRAMES = {0, 1, 2, 1, 0};
    
    private static final int PUNCH_DURATION = PUNCH_ANIMATION_FRAMES.length * TICKS_PER_FRAME;
    
    private int punchTicksRemaining;
    
    public Bomber(double x, double y, double ort, Team team) {
        super(x, y, ort, MASK_WIDTH, MASK_HEIGHT,
                DEFAULT_SPEED, DEFAULT_DURABILITY, team);

    loadSpriteSheet("/res/fightersheet.png", 4, 3);
}

    private boolean tryStartPunch() {
        if (punchTicksRemaining > 0)
            return false;

        punchTicksRemaining = PUNCH_DURATION;
        setAnimationFrame(PUNCH_ANIMATION_FRAMES[0]);

        return true;
    }
    
    @Override
    public void tick() {
        super.tick();

        if (punchTicksRemaining > 0) {
            punchTicksRemaining--;

            int ticksPassados = PUNCH_DURATION - punchTicksRemaining;

            int step = Math.min( ticksPassados / TICKS_PER_FRAME, PUNCH_ANIMATION_FRAMES.length - 1);

            setAnimationFrame(PUNCH_ANIMATION_FRAMES[step]);
        }
    }
    
    @Override
    public void onCollision(Entity other) {
        if (other == null || other == this || isKO())
            return;

        if (!tryStartPunch())
            return;

        if (other instanceof Fighter target)
            target.takeDamage(PUNCH_DAMAGE);
    }
}
