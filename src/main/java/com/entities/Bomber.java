package com.entities;

import com.system.Engine;
import com.system.Ring;

/**
- @author evan.gabb
- @author dleskow
 */
public class Bomber extends Fighter {
    private static final int DEFAULT_DURABILITY = 400;
    private static final int MASK_WIDTH = 24;
    private static final int MASK_HEIGHT = 34;
    private static final double DEFAULT_SPEED = 4.0;

    private static final int PUNCH_DAMAGE = 20;
    private static final double BOMBER_SPEED_SCALING = 0.35;
    private static final double BOMB_SPAWN_DISTANCE = 42.0;

    private static final int TICKS_PER_FRAME = 4;
    private static final int[] PUNCH_ANIMATION_FRAMES = {0, 1, 2, 1, 0};
    private static final int PUNCH_DURATION = PUNCH_ANIMATION_FRAMES.length * TICKS_PER_FRAME;
    
    private int punchTicksRemaining;
    
    public Bomber(double x, double y, double ort, Team team) {
        super(x, y, ort, MASK_WIDTH, MASK_HEIGHT,
                DEFAULT_SPEED, DEFAULT_DURABILITY, team);

        loadSpriteSheet("/res/fightersheet.png", 4, 3);
    }

    @Override
    public boolean usePrimaryAbility() {
        if (!isAbilityReady()) {
            return false;
        }

        Engine engine = Engine.instance();
        if (engine == null) {
            return false;
        }

        double centerX = getX() + getRenderWidth() / 2.0;
        double centerY = getY() + getRenderHeight() / 2.0;
        double angle = getAimAngle();

        double bombSize = 18.0;
        double spawnX = centerX + Math.cos(angle) * BOMB_SPAWN_DISTANCE - bombSize / 2.0;
        double spawnY = centerY + Math.sin(angle) * BOMB_SPAWN_DISTANCE - bombSize / 2.0;

        Ring ring = Ring.instance();
        if (ring != null) {
            spawnX = Math.max(ring.getX(), Math.min(ring.getX() + ring.getWidth() - bombSize, spawnX));
            spawnY = Math.max(ring.getY(), Math.min(ring.getY() + ring.getHeight() - bombSize, spawnY));
        }

        engine.spawnEntity(new Bomb(spawnX, spawnY));
        startAbilityCooldown();
        return true;
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
            int step = Math.min(ticksPassados / TICKS_PER_FRAME, PUNCH_ANIMATION_FRAMES.length - 1);

            setAnimationFrame(PUNCH_ANIMATION_FRAMES[step]);
        }
    }
    
    @Override
    public void onCollision(Entity other) {
        if (other == null || other == this || isKO() || other instanceof Bomb)
            return;

        if (!tryStartPunch())
            return;

        if (other instanceof Fighter target) {
            int finalDamage = calculateImpactDamage(PUNCH_DAMAGE, BOMBER_SPEED_SCALING);
            target.takeDamage(finalDamage);
        }
    }
}
