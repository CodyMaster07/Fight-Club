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
 *
 * @author evan.gabb
 */
public abstract class Fighter extends Entity {
    
    private static final int TICKS_OF_FLASHING = 12;
    
    
    private int totalFrames = 6;
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
    private static final int SPRITE_SCALE = 2;
    
    private static final Font HEALTH_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 10);
    
    private BufferedImage[] sprites;
    
    public Fighter(double x, double y, double ort, int maskWidth, int maskHeight, double speed, int durability) {
        super(x, y, ort, maskWidth, maskHeight, speed, durability, 2);
        this.maxDurability = durability;
    }
    
    protected void loadSpriteSheet(String sprite_path, int frameCount) {
        if (frameCount <= 0) {
            throw new IllegalArgumentException("Quantidade de Frames não pode ser negativa");
        }
        
        try {
            URL url = getClass().getResource(sprite_path);
            if (url == null) {
                System.err.println("Error: sprite not found");
                return;
            }
            
            BufferedImage sheet = ImageIO.read(url);
            
            this.sprites = new BufferedImage[totalFrames];
            
            for (int i = 0; i < totalFrames; i++) {
                this.sprites[i] = sheet.getSubimage(i * F_WIDTH, 0, F_WIDTH, F_HEIGHT);
            }
            
        this.normalFrameCount = frameCount;
        setAnimationFrame(0);
        
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível carregar: "+ sprite_path, e);
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
        
        int spriteOffset = damageFlashTicks > 0 ? normalFrameCount : 0; // Se estiver sofrendo o flash do dano, vai carregar a sprite de flash equivalente ao quadro atual;
        sprite = sprites[animationFrame + spriteOffset];
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
