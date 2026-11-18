package com.entities;

import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

/**
 * @author evan.gabb
 * @author dleskow
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
    
    private BufferedImage[][] sprites;
    private final Team team;
    private boolean isLeft;
    
    public Fighter(double x, double y, double ort, int maskWidth, int maskHeight, double speed, int durability, Team team) {
        super(x, y, ort, maskWidth, maskHeight, speed, durability, 2);
        this.maxDurability = durability;
        this.team = java.util.Objects.requireNonNull(team, "Time obrigatório");
    }
    
    public Team getTeam() {
        return team;
    }

    public boolean isLeft() {
        return isLeft;
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
    
    protected final void setAnimationFrame (int frame) {
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
                    ? normalFrameCount - 1       // 2: dano começa pelo último quadro
                    : normalFrameCount * 2 - 1; // 5: normal começa pelo último quadro
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
        
        // O dano fica pendente, podendo ser acumulado. Também permite que um Fighter possa fazer sua ação antes de morrer em um tick
        pendingDamage = Math.min(getDurability(), pendingDamage + damage); // Faz com que o dano pendente nunca possa ser maior que a vida que sobrou
        
    }
    
    public void applyPendingDamage() {
        if (pendingDamage == 0)
            return;
        
        setDurability(Math.max(0, getDurability() - pendingDamage)); // Só uma garantia
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

        // Na mesma coordenada horizontal, preserva a direção anterior.
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
            super.tick();
        }
        
        if (damageFlashTicks > 0) {
            damageFlashTicks--;
            updateSprite();
        }
    }
    
    @Override
    public void render(Graphics g) {
        // Desenha a sprite conforme entity
        super.render(g);

        // Cria uma cópia para não alterar o Graphics original
        Graphics barGraphics = g.create();

        try {
            int currentHealth = getDurability();

            int spriteWidth = sprite != null
                    ? sprite.getWidth()
                    : getMaskWidth();

            // Centralizando a barra acima da sprite
            int barX = (int) getX() + (getRenderWidth() - HEALTH_BAR_WIDTH) / 2;

            int barY = (int) getY() - HEALTH_BAR_HEIGHT - HEALTH_BAR_GAP;

            double healthProportion = maxDurability > 0 ? (double) currentHealth / maxDurability : 0.0;

            int filledWidth = (int) Math.round(HEALTH_BAR_WIDTH * healthProportion);

            // Fundo na parte correspondente à vida perdida
            barGraphics.setColor(Color.DARK_GRAY);
            barGraphics.fillRect(barX, barY, HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT);

            // Parte vermelha proporcional a vida
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
    
    @Override
    public abstract void onCollision(Entity other);
}
