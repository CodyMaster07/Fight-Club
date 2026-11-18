package com.game;

import java.awt.Rectangle;

public class Button {
    public final Rectangle bounds;
    public final String label;
    public final Runnable action;

    public Button(int x, int y, int width, int height, String label, Runnable action) {
        this.bounds = new Rectangle(x, y, width, height);
        this.label = label;
        this.action = action;
    }

    public void execute() {
        if (action != null) {
            action.run();
        }
    }
}
