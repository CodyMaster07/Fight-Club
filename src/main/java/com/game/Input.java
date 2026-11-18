package com.game;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashSet;
import java.util.Set;

public class Input implements KeyListener, MouseListener, MouseMotionListener {

    private final int frameScale;

    // Keyboard states
    private final Set<Integer> keysDown = new HashSet<>();
    private final Set<Integer> keysJustPressed = new HashSet<>();
    private final Set<Integer> keysJustReleased = new HashSet<>();

    // Mouse states (stored in unscaled/native coordinates)
    private int mouseX;
    private int mouseY;
    private boolean mouseDown;
    private boolean mouseJustPressed;
    private boolean mouseJustReleased;

    public Input(int frameScale) {
        // Guard against 0 or negative scale
        this.frameScale = Math.max(1, frameScale);
    }

    /**
     * Call this at the VERY END of every update tick in your loop
     * so single-frame "just pressed" / "just released" events reset.
     */
    public void update() {
        keysJustPressed.clear();
        keysJustReleased.clear();
        mouseJustPressed = false;
        mouseJustReleased = false;
    }

    // ==========================================
    // KEYBOARD QUERIES
    // ==========================================

    /** True on every frame the key is held down (great for fighter movement). */
    public boolean isKeyDown(int keyCode) {
        return keysDown.contains(keyCode);
    }

    /** True ONLY on the single frame the key was pressed (great for menu navigation). */
    public boolean isKeyJustPressed(int keyCode) {
        return keysJustPressed.contains(keyCode);
    }

    /** True ONLY on the single frame the key was let go. */
    public boolean isKeyJustReleased(int keyCode) {
        return keysJustReleased.contains(keyCode);
    }

    // ==========================================
    // MOUSE & HOVER QUERIES (Native Resolution)
    // ==========================================

    /** Mouse X adjusted by frameScale to match NAT_WIDTH (0 to res[0]). */
    public int getMouseX() {
        return mouseX;
    }

    /** Mouse Y adjusted by frameScale to match NAT_HEIGHT (0 to res[1]). */
    public int getMouseY() {
        return mouseY;
    }

    public boolean isMouseDown() {
        return mouseDown;
    }

    public boolean isMouseJustPressed() {
        return mouseJustPressed;
    }

    public boolean isMouseJustReleased() {
        return mouseJustReleased;
    }

    /** Checks if the scaled mouse cursor is hovering inside a native-resolution Rectangle. */
    public boolean isHovering(Rectangle bounds) {
        return bounds != null && bounds.contains(mouseX, mouseY);
    }

    /** Overload to check hover without creating a Rectangle object. */
    public boolean isHovering(int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width &&
               mouseY >= y && mouseY <= y + height;
    }

    // ==========================================
    // AWT LISTENER CALLBACKS
    // ==========================================

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        // Only register "justPressed" if it wasn't already held down (ignores OS key-repeat)
        if (!keysDown.contains(code)) {
            keysJustPressed.add(code);
        }
        keysDown.add(code);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        keysDown.remove(code);
        keysJustReleased.add(code);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // Divide by frameScale so coordinates match your NAT_WIDTH x NAT_HEIGHT canvas
        this.mouseX = e.getX() / frameScale;
        this.mouseY = e.getY() / frameScale;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (!mouseDown) {
                mouseJustPressed = true;
            }
            mouseDown = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            mouseDown = false;
            mouseJustReleased = true;
        }
    }

    // Unused interface methods
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}
