package nl.saxion.game.eclosion.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import nl.saxion.gameapp.GameApp;
import nl.saxion.game.eclosion.audio.Sfx;

/**
 * Keypad for a code lock (the safe in Ageha's room).
 *
 * Keyboard: number keys (top row or numpad), Backspace = delete, Enter = try the code, Esc = close.
 * Mouse: click the buttons; click outside the panel to close.
 * Uses the dialogue fonts, so DialogueBox.loadAssets() must have been called.
 */
public class SafeKeypad {
    public enum Result { NOTHING, CORRECT, CLOSED }

    private static final Color PANEL_COLOR = new Color(8 / 255f, 8 / 255f, 10 / 255f, 0.94f);
    private static final Color LINE_COLOR = new Color(138 / 255f, 135 / 255f, 144 / 255f, 1f);
    private static final Color INNER_LINE_COLOR = new Color(60 / 255f, 58 / 255f, 66 / 255f, 1f);
    private static final Color BUTTON_COLOR = new Color(28 / 255f, 26 / 255f, 32 / 255f, 1f);
    private static final Color BUTTON_HOVER = new Color(48 / 255f, 44 / 255f, 54 / 255f, 1f);
    private static final Color TEXT_COLOR = new Color(232 / 255f, 228 / 255f, 220 / 255f, 1f);
    private static final Color HINT_COLOR = new Color(138 / 255f, 135 / 255f, 144 / 255f, 1f);
    private static final Color WRONG_COLOR = new Color(190 / 255f, 60 / 255f, 70 / 255f, 1f);
    private static final Color DIM = new Color(0, 0, 0, 0.45f);

    private static final float PANEL_W = 340, PANEL_H = 440;
    private static final float BUTTON_W = 84, BUTTON_H = 52, BUTTON_GAP = 12;
    private static final float SLOT_W = 36, SLOT_H = 46, SLOT_GAP = 8;
    private static final String[] BUTTONS = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "<", "0", "OK"};

    private final String code;
    private boolean open = false;
    private String entered = "";
    private float wrongTimer = 0;   // > 0 while "Wrong code" is shown

    public SafeKeypad(String code) {
        this.code = code;
    }

    public void open() {
        open = true;
        entered = "";
        wrongTimer = 0;
    }

    public boolean isOpen() {
        return open;
    }

    /** Handles keys and clicks. Call every frame while open. */
    public Result update(float delta, float mouseX, float mouseY, float worldW, float worldH) {
        if (!open) {
            return Result.NOTHING;
        }
        if (wrongTimer > 0) {
            wrongTimer -= delta;
        }

        if (GameApp.isKeyJustPressed(Input.Keys.ESCAPE)) {
            open = false;
            return Result.CLOSED;
        }
        for (int digit = 0; digit <= 9; digit++) {
            if (GameApp.isKeyJustPressed(Input.Keys.NUM_0 + digit) || GameApp.isKeyJustPressed(Input.Keys.NUMPAD_0 + digit)) {
                press(String.valueOf(digit));
            }
        }
        if (GameApp.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            press("<");
        }
        if (GameApp.isKeyJustPressed(Input.Keys.ENTER) || GameApp.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)) {
            if (press("OK")) return Result.CORRECT;
        }

        if (GameApp.isButtonJustPressed(Input.Buttons.LEFT)) {
            float panelX = worldW / 2 - PANEL_W / 2, panelY = worldH / 2 - PANEL_H / 2;
            if (!GameApp.pointInRect(mouseX, mouseY, panelX, panelY, PANEL_W, PANEL_H)) {
                open = false;
                return Result.CLOSED;
            }
            int button = buttonAt(mouseX, mouseY, worldW, worldH);
            if (button >= 0 && press(BUTTONS[button])) {
                return Result.CORRECT;
            }
        }
        return Result.NOTHING;
    }

    /** Returns true when the right code was confirmed. */
    private boolean press(String key) {
        if (key.equals("<")) {
            Sfx.play(Sfx.KEY, 0.4f, 0.03f);
            if (!entered.isEmpty()) entered = entered.substring(0, entered.length() - 1);
        } else if (key.equals("OK")) {
            if (entered.length() < code.length()) {
                Sfx.play(Sfx.KEY, 0.4f, 0.03f);
                return false;
            }
            if (entered.equals(code)) {
                Sfx.play(Sfx.KEY_RIGHT);    // click-clack: the lock opens
                open = false;
                return true;
            }
            Sfx.play(Sfx.KEY_WRONG);
            wrongTimer = 1.2f;
            entered = "";
        } else if (entered.length() < code.length()) {
            Sfx.play(Sfx.KEY, 0.4f, 0.03f);
            entered += key;
        }
        return false;
    }

    private float gridX(float worldW) {
        return worldW / 2 - (3 * BUTTON_W + 2 * BUTTON_GAP) / 2;
    }

    private float gridY(float worldH) {
        return worldH / 2 - PANEL_H / 2 + 26;
    }

    /** Index in BUTTONS under the mouse, or -1. */
    private int buttonAt(float mx, float my, float worldW, float worldH) {
        for (int i = 0; i < BUTTONS.length; i++) {
            float bx = gridX(worldW) + (i % 3) * (BUTTON_W + BUTTON_GAP);
            float by = gridY(worldH) + (3 - i / 3) * (BUTTON_H + BUTTON_GAP);
            if (GameApp.pointInRect(mx, my, bx, by, BUTTON_W, BUTTON_H)) return i;
        }
        return -1;
    }

    public void render(float mouseX, float mouseY, float worldW, float worldH) {
        if (!open) {
            return;
        }
        float panelX = worldW / 2 - PANEL_W / 2, panelY = worldH / 2 - PANEL_H / 2;
        int hover = buttonAt(mouseX, mouseY, worldW, worldH);

        float slotsW = code.length() * SLOT_W + (code.length() - 1) * SLOT_GAP;
        float slotsX = worldW / 2 - slotsW / 2;
        float slotsY = gridY(worldH) + 4 * BUTTON_H + 3 * BUTTON_GAP + 26;

        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        GameApp.drawRect(0, 0, worldW, worldH, DIM);
        GameApp.drawRect(panelX, panelY, PANEL_W, PANEL_H, PANEL_COLOR);
        for (int i = 0; i < BUTTONS.length; i++) {
            float bx = gridX(worldW) + (i % 3) * (BUTTON_W + BUTTON_GAP);
            float by = gridY(worldH) + (3 - i / 3) * (BUTTON_H + BUTTON_GAP);
            GameApp.drawRect(bx, by, BUTTON_W, BUTTON_H, i == hover ? BUTTON_HOVER : BUTTON_COLOR);
        }
        GameApp.endShapeRendering();

        GameApp.setLineWidth(1.5f);
        GameApp.startShapeRenderingOutlined();
        GameApp.drawRect(panelX, panelY, PANEL_W, PANEL_H, LINE_COLOR);
        GameApp.drawRect(panelX + 5, panelY + 5, PANEL_W - 10, PANEL_H - 10, INNER_LINE_COLOR);
        for (int i = 0; i < BUTTONS.length; i++) {
            float bx = gridX(worldW) + (i % 3) * (BUTTON_W + BUTTON_GAP);
            float by = gridY(worldH) + (3 - i / 3) * (BUTTON_H + BUTTON_GAP);
            GameApp.drawRect(bx, by, BUTTON_W, BUTTON_H, INNER_LINE_COLOR);
        }
        for (int i = 0; i < code.length(); i++) {
            GameApp.drawRect(slotsX + i * (SLOT_W + SLOT_GAP), slotsY, SLOT_W, SLOT_H,
                    wrongTimer > 0 ? WRONG_COLOR : LINE_COLOR);
        }
        GameApp.endShapeRendering();
        GameApp.setLineWidth(1f);

        GameApp.startSpriteRendering();
        BitmapFont font = GameApp.getFont(DialogueBox.TEXT_FONT);
        String title = wrongTimer > 0 ? "Wrong code" : "Enter the code";
        drawCentered(font, title, worldW / 2, panelY + PANEL_H - 22, wrongTimer > 0 ? WRONG_COLOR : TEXT_COLOR);
        for (int i = 0; i < code.length(); i++) {
            String c = i < entered.length() ? String.valueOf(entered.charAt(i)) : "_";
            drawCentered(font, c, slotsX + i * (SLOT_W + SLOT_GAP) + SLOT_W / 2, slotsY + SLOT_H - 10,
                    i < entered.length() ? TEXT_COLOR : HINT_COLOR);
        }
        for (int i = 0; i < BUTTONS.length; i++) {
            float bx = gridX(worldW) + (i % 3) * (BUTTON_W + BUTTON_GAP);
            float by = gridY(worldH) + (3 - i / 3) * (BUTTON_H + BUTTON_GAP);
            drawCentered(font, BUTTONS[i], bx + BUTTON_W / 2, by + BUTTON_H - 13, TEXT_COLOR);
        }
        drawCentered(font, "Esc: close", worldW / 2, panelY - 14, HINT_COLOR);
        GameApp.endSpriteRendering();
        GameApp.disableTransparency();
    }

    /** y is the top of the text. */
    private void drawCentered(BitmapFont font, String text, float centerX, float top, Color color) {
        font.setColor(color);
        float w = GameApp.getTextWidth(DialogueBox.TEXT_FONT, text);
        font.draw(GameApp.getSpriteBatch(), text, centerX - w / 2, top);
    }
}
