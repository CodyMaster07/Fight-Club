package com.game;

import com.system.Engine;
import com.game.Input;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author evan.gabb
 * @author dleskow
 */
public class MainMenu {
    private Window window;
    public enum Menu { MAIN, GAME, LOAD, CHARACTER_SELECT }
    private Menu menu;

    private int[] res;
    private int[] ring;
    private int frameScale;
    private int[] players;
    private int[] initialPos;

    public MainMenu(int[] res, int[] ring, int frameScale, int[] players, int[] initialPos) {
        this.window = new Window("Fight Club Game", res, frameScale);
        this.menu = Menu.MAIN;
        this.res = res;
        this.ring = ring;
        this.frameScale = frameScale;
        this.players = players;
        this.initialPos = initialPos;
    }

    public void handleMenus() {
        while (true) {
            switch (menu) {
                case MAIN:
                default:
                    menu = showMainMenu();
                    break;

                case GAME:
				    Engine engine = new Engine(window, res, ring, frameScale, players, initialPos);
				    engine.start();
				    engine.awaitFinish(); // Waits here while gameThread is active
				    menu = Menu.MAIN;     // Returns to Main Menu
				    break;

                case LOAD:
                    // Save.load(); // To be implemented later
                    menu = Menu.MAIN;
                    break;

                case CHARACTER_SELECT:
                    // To be implemented later
                    menu = Menu.MAIN;
                    break;
            }
        }
    }

    private Menu showMainMenu() {
        Input input = window.getInput();
        int width = window.getNatWidth();
        int height = window.getNatHeight();

        // Holds the next Menu state once a button is triggered
        final Menu[] nextMenu = { null };

        // Button dimensions centered horizontally
        int btnWidth = 140;
        int btnHeight = 26;
        int btnX = (width - btnWidth) / 2;
        int startY = height / 2 - 20;
        int spacing = 34;

        List<Button> buttons = new ArrayList<>();
        buttons.add(new Button(btnX, startY, btnWidth, btnHeight, "Play", () -> {
            nextMenu[0] = Menu.GAME;
        }));
        buttons.add(new Button(btnX, startY + spacing, btnWidth, btnHeight, "Select Characters", () -> {
            // Empty for now (later: nextMenu[0] = Menu.CHARACTER_SELECT;)
        }));
        buttons.add(new Button(btnX, startY + spacing * 2, btnWidth, btnHeight, "Load", () -> {
            // Empty for now (later: Save.load();)
        }));

        int selectedIndex = -1; // -1 means no button is currently selected
        int lastMouseX = input.getMouseX();
        int lastMouseY = input.getMouseY();

        while (nextMenu[0] == null) {
            // --- 1. UPDATE INPUT ---
            int mx = input.getMouseX();
            int my = input.getMouseY();
            boolean mouseMoved = (mx != lastMouseX || my != lastMouseY);
            lastMouseX = mx;
            lastMouseY = my;

            // Mouse hover detection (only overrides keyboard if mouse actually moved)
            if (mouseMoved) {
                selectedIndex = -1;
                for (int i = 0; i < buttons.size(); i++) {
                    if (input.isHovering(buttons.get(i).bounds)) {
                        selectedIndex = i;
                        break;
                    }
                }
            }

            // Mouse click on hovered button
            if (input.isMouseJustPressed()) {
                for (int i = 0; i < buttons.size(); i++) {
                    if (input.isHovering(buttons.get(i).bounds)) {
                        buttons.get(i).execute();
                        break;
                    }
                }
            }

            // Keyboard arrow navigation
            boolean nextKey = input.isKeyJustPressed(KeyEvent.VK_UP) || input.isKeyJustPressed(KeyEvent.VK_RIGHT);
            boolean prevKey = input.isKeyJustPressed(KeyEvent.VK_DOWN) || input.isKeyJustPressed(KeyEvent.VK_LEFT);

            if (nextKey || prevKey) {
                if (selectedIndex == -1) {
                    selectedIndex = 0; // Select first button if none selected
                } else if (nextKey) {
                    selectedIndex = (selectedIndex + 1) % buttons.size(); // Next (below)
                } else {
                    selectedIndex = (selectedIndex - 1 + buttons.size()) % buttons.size(); // Previous (above)
                }
            }

            // Enter triggers currently selected button
            if (input.isKeyJustPressed(KeyEvent.VK_ENTER) && selectedIndex != -1) {
                buttons.get(selectedIndex).execute();
            }

            input.update(); // Reset single-frame key/mouse events

            // --- 2. RENDER ---
            Graphics2D g = window.beginFrame();
            if (g != null) {
                renderMainMenu(g, width, height, buttons, selectedIndex);
                window.showFrame(g);
            }

            // Cap menu loop to ~60 FPS so it doesn't max out a CPU core
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return nextMenu[0];
    }

    private void renderMainMenu(Graphics2D g, int width, int height, List<Button> buttons, int selectedIndex) {
        // Title
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        String title = "FIGHT CLUB GAME";
        FontMetrics titleFm = g.getFontMetrics();
        g.drawString(title, (width - titleFm.stringWidth(title)) / 2, height / 4);

        // Buttons
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics btnFm = g.getFontMetrics();

        for (int i = 0; i < buttons.size(); i++) {
            Button btn = buttons.get(i);
            boolean isHovered = (i == selectedIndex);

            // Button background
            g.setColor(isHovered ? new Color(180, 45, 45) : new Color(55, 55, 55));
            g.fillRect(btn.bounds.x, btn.bounds.y, btn.bounds.width, btn.bounds.height);

            // Button border
            g.setColor(isHovered ? Color.WHITE : new Color(120, 120, 120));
            g.drawRect(btn.bounds.x, btn.bounds.y, btn.bounds.width, btn.bounds.height);

            // Centered label
            g.setColor(Color.WHITE);
            int textX = btn.bounds.x + (btn.bounds.width - btnFm.stringWidth(btn.label)) / 2;
            int textY = btn.bounds.y + ((btn.bounds.height - btnFm.getHeight()) / 2) + btnFm.getAscent();
            g.drawString(btn.label, textX, textY);
        }
    }
}
