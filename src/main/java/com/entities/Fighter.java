package com.entities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

/**
- @author evan.gabb
- @author dleskow
 */
public abstract class Fighter extends Entity {
    
    private static final int TICKS_OF_FLASHING = 12;
    
    private static final int F_WIDTH = 34;
    private static final int F_HEIGHT = 34;
    private int normalFrameCount;
    private int animationFrame;
    private int damageFlashTicks;
    private int pendingDamage;
    private final int maxDurability;
    private static final int HEALTH_BAR_WIDTH = 36;
    private static final int HEALTH_BAR_HEIGHT = 12;
    private static final int HEALTH_BAR_GAP = 4;
    
    private static final Font HEALTH_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 10);

    // --- AIM, ABILITY & SPEED-DAMAGE CONSTANTS ---
    public static final double AIM_ROTATION_SPEED = Math.toRadians(4.0);
    public static final double DASH_SPEED_BOOST = 4.0;
    private static final int ABILITY_COOLDOWN_TICKS = 120; // 2.0s at 60 FPS
    private static final double REFERENCE_SPEED = 4.0;
    private static final double DEFAULT_SPEED_DAMAGE_SCALING = 1.0;

    private double aimAngle;
    private int abilityCooldownRemaining;
    
    private BufferedImage[][] sprites;
    private final Team team;
    private boolean isLeft;
    
    public Fighter(double x, double y, double ort, int maskWidth, int maskHeight, double speed, int durability, Team team) {
        super(x, y, ort, maskWidth, maskHeight, speed, durability, 2);
        this.maxDurability = durability;
        this.team = java.util.Objects.requireNonNull(team, "Time obrigatório");
        this.aimAngle = ort;
    }
    
    public Team getTeam() {
        return team;
    }

    public boolean isLeft() {
        return isLeft;
    }

    public double getAimAngle() {
        return aimAngle;
    }

    public void rotateAim(double deltaAngle) {
        this.aimAngle = normalizeAng(this.aimAngle + deltaAngle);
    }

    public boolean isAbilityReady() {
        return !isKO() && abilityCooldownRemaining <= 0;
    }

    protected void startAbilityCooldown() {
        abilityCooldownRemaining = ABILITY_COOLDOWN_TICKS;
    }

    /** Default primary ability is Dash (used by Boxer). Subclasses like Bomber can override. */
    public boolean usePrimaryAbility() {
        return tryDash();
    }

    public boolean tryDash() {
        if (!isAbilityReady()) {
            return false;
        }

        setOrientation(aimAngle);
        setSpeed(getSpeed() + DASH_SPEED_BOOST);
        startAbilityCooldown();
        return true;
    }

    protected int calculateImpactDamage(int baseDamage) {
        return calculateImpactDamage(baseDamage, DEFAULT_SPEED_DAMAGE_SCALING);
    }

    protected int calculateImpactDamage(int baseDamage, double speedScaling) {
        double speedRatio = getImpactSpeed() / REFERENCE_SPEED;
        double multiplier = 1.0 + speedScaling * (speedRatio - 1.0);
        return Math.max(1, (int) Math.round(baseDamage * multiplier));
    }
    
    protected void loadSpriteSheet(String spritePath, int firstRow, int frameCount) {
        if (firstRow < 0 || frameCount <= 0) {
            throw new IllegalArgumentException("Linha ou quantidade de quadros inválida");
        }

        URL url = getClass().getResource(spritePath);
        if (url == null) {
            throw new IllegalStateException("Sprite não encontrada: " + spritePath);
        }

        try {
            BufferedImage sheet = ImageIO.read(url);
            int columns = frameCount * 2;

            if (sheet == null
                    || sheet.getWidth() < columns * F_WIDTH
                    || sheet.getHeight() < (firstRow + 4) * F_HEIGHT) {
                throw new IllegalStateException("Spritesheet inválida: " + spritePath);
            }

            sprites = new BufferedImage[4][columns];

            for (int row = 0; row < 4; row++) {
                for (int column = 0; column < columns; column++) {
                    sprites[row][column] = sheet.getSubimage(
                            column * F_WIDTH,
                            (firstRow + row) * F_HEIGHT,
                            F_WIDTH,
                            F_HEIGHT);
                }
            }

            normalFrameCount = frameCount;
            setAnimationFrame(0);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível carregar: " + spritePath, e);
        }
    }
    
    public boolean isKO() {
        return getDurability() == 0;
    }
    
    protected final void setAnimationFrame(int frame) {
        if (frame < 0 || frame >= normalFrameCount) {
            throw new IllegalArgumentException("Frame inválido: " + frame);
        }
        
        animationFrame = frame;
        updateSprite();
    }
    
    private void updateSprite() {
        if (sprites == null) {
            return;
        }

        int row = team.getRowOffset() + (isLeft ? 1 : 0);
        boolean damaged = damageFlashTicks > 0;
        int column;

        if (isLeft) {
            int lastColumn = damaged
                    ? normalFrameCount - 1
                    : normalFrameCount * 2 - 1;
            column = lastColumn - animationFrame;
        } else {
            int offset = damaged ? normalFrameCount : 0;
            column = offset + animationFrame;
        }

        sprite = sprites[row][column];
    }
    
    protected void takeDamage(int damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("O dano não pode ser negativo.");
        }
        
        if (damage == 0 || isKO())
            return;
        
        pendingDamage = Math.min(getDurability(), pendingDamage + damage);
    }
    
    public void applyPendingDamage() {
        if (pendingDamage == 0)
            return;
        
        setDurability(Math.max(0, getDurability() - pendingDamage));
        pendingDamage = 0;
        damageFlashTicks = TICKS_OF_FLASHING;
        updateSprite();
    }   
    
    public void faceOpponent(Fighter opponent) {
        if (opponent == null || opponent == this || opponent.getTeam() == team) {
            return;
        }

        double myCenterX = getX() + getRenderWidth() / 2.0;
        double opponentCenterX = opponent.getX() + opponent.getRenderWidth() / 2.0;

        if (myCenterX == opponentCenterX) {
            return;
        }

        boolean nextIsLeft = opponentCenterX < myCenterX;
        if (isLeft != nextIsLeft) {
            isLeft = nextIsLeft;
            updateSprite();
        }
    }
    
    @Override
    public void tick() {
        if (!isKO()) {
            if (abilityCooldownRemaining > 0) {
                abilityCooldownRemaining--;
            }

            super.tick();
        }
        
        if (damageFlashTicks > 0) {
            damageFlashTicks--;
            updateSprite();
        }
    }
    
    @Override
    public void render(Graphics g) {
        if (!isKO()) {
            renderAimArrow(g);
        }

        super.render(g);

        Graphics barGraphics = g.create();

        try {
            int currentHealth = getDurability();

            int barX = (int) getX() + (getRenderWidth() - HEALTH_BAR_WIDTH) / 2;
            int barY = (int) getY() - HEALTH_BAR_HEIGHT - HEALTH_BAR_GAP;

            double healthProportion = maxDurability > 0 ? (double) currentHealth / maxDurability : 0.0;
            int filledWidth = (int) Math.round(HEALTH_BAR_WIDTH * healthProportion);

            barGraphics.setColor(Color.DARK_GRAY);
            barGraphics.fillRect(barX, barY, HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT);

            barGraphics.setColor(Color.RED);
            barGraphics.fillRect(barX, barY, filledWidth, HEALTH_BAR_HEIGHT);

            barGraphics.setFont(HEALTH_FONT);
            FontMetrics metrics = barGraphics.getFontMetrics();

            String text = Integer.toString(currentHealth);
            int textX = barX + (HEALTH_BAR_WIDTH - metrics.stringWidth(text)) / 2;
            int textY = barY + (HEALTH_BAR_HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent();

            barGraphics.setColor(Color.WHITE);
            barGraphics.drawString(text, textX, textY);

        } finally {
            barGraphics.dispose();
        }
    }

    private void renderAimArrow(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            double centerX = getX() + getRenderWidth() / 2.0;
            double centerY = getY() + getRenderHeight() / 2.0;

            double startDist = 26.0;
            double endDist = 46.0;
            double headSize = 7.0;

            double cos = Math.cos(aimAngle);
            double sin = Math.sin(aimAngle);

            double x1 = centerX + cos * startDist;
            double y1 = centerY + sin * startDist;
            double x2 = centerX + cos * endDist;
            double y2 = centerY + sin * endDist;

            boolean ready = isAbilityReady();
            Color arrowColor = (team == Team.BLUE)
                    ? (ready ? new Color(70, 170, 255) : new Color(70, 170, 255, 90))
                    : (ready ? new Color(255, 85, 85) : new Color(255, 85, 85, 90));

            g2.setColor(arrowColor);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine((int) Math.round(x1), (int) Math.round(y1), (int) Math.round(x2), (int) Math.round(y2));

            double angle1 = aimAngle + Math.toRadians(145);
            double angle2 = aimAngle - Math.toRadians(145);

            Path2D.Double head = new Path2D.Double();
            head.moveTo(x2, y2);
            head.lineTo(x2 + Math.cos(angle1) * headSize, y2 + Math.sin(angle1) * headSize);
            head.lineTo(x2 + Math.cos(angle2) * headSize, y2 + Math.sin(angle2) * headSize);
            head.closePath();

            g2.fill(head);
        } finally {
            g2.dispose();
        }
    }
    
    @Override
    public abstract void onCollision(Entity other);
}
