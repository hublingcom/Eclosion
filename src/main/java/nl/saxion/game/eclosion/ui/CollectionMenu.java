package nl.saxion.game.eclosion.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import nl.saxion.game.eclosion.audio.Bgm;
import nl.saxion.game.eclosion.audio.Sfx;
import nl.saxion.game.eclosion.items.Items;
import nl.saxion.gameapp.GameApp;

/**
 * The collection of memory items (Dawon). Opens with I (or Tab) while walking around, like the pause menu:
 * the game stops, a panel shows one place per floor. Found items show their picture,
 * the others a "?". Left/Right (A/D) or the mouse picks a place, its name and text show below.
 * I / Tab / Esc closes it. Same style as the pause menu and the dialogue box.
 * Uses the dialogue fonts, so DialogueBox.loadAssets() must have been called.
 */
public class CollectionMenu {
    private static final Color DARKEN = new Color(0, 0, 0, 0.6f);
    private static final Color PANEL = new Color(8 / 255f, 8 / 255f, 10 / 255f, 0.94f);
    private static final Color LINE = new Color(138 / 255f, 135 / 255f, 144 / 255f, 1f);
    private static final Color INNER = new Color(60 / 255f, 58 / 255f, 66 / 255f, 1f);
    private static final Color SLOT = new Color(22 / 255f, 21 / 255f, 26 / 255f, 1f);
    private static final Color TEXT = new Color(232 / 255f, 228 / 255f, 220 / 255f, 1f);
    private static final Color DIM = new Color(140 / 255f, 136 / 255f, 132 / 255f, 1f);
    private static final Color MARKER = new Color(150 / 255f, 60 / 255f, 70 / 255f, 1f);

    private static final float SLOT_SIZE = 120, GAP = 24, PANEL_H = 400, PADDING = 40;

    private boolean open = false;
    private int selected = 0;
    private float lastMouseX = Float.NaN, lastMouseY = Float.NaN;
    private final Rectangle[] slotAreas = new Rectangle[Items.SLOTS];

    public boolean isOpen() {
        return open;
    }

    public void open(float mouseX, float mouseY) {
        open = true;
        selected = 0;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        for (Items.Item item : Items.ALL) {
            if (Items.has(item)) {
                item.load();
            }
        }
        Bgm.setDimmed(true);
        Sfx.play(Sfx.CONFIRM);
    }

    private void close() {
        open = false;
        Bgm.setDimmed(false);
        Sfx.play(Sfx.MOVE);
    }

    /** True for the keys that open / close the collection. */
    public static boolean toggleKey() {
        return GameApp.isKeyJustPressed(Input.Keys.I) || GameApp.isKeyJustPressed(Input.Keys.TAB);
    }

    public void update(float mouseX, float mouseY) {
        if (!open) {
            return;
        }
        if (toggleKey() || GameApp.isKeyJustPressed(Input.Keys.ESCAPE)) {
            close();
            return;
        }
        int before = selected;
        if (GameApp.isKeyJustPressed(Input.Keys.LEFT) || GameApp.isKeyJustPressed(Input.Keys.A)) {
            selected = (selected - 1 + Items.SLOTS) % Items.SLOTS;
        }
        if (GameApp.isKeyJustPressed(Input.Keys.RIGHT) || GameApp.isKeyJustPressed(Input.Keys.D)) {
            selected = (selected + 1) % Items.SLOTS;
        }
        boolean mouseMoved = mouseX != lastMouseX || mouseY != lastMouseY;
        if (mouseMoved) {
            for (int i = 0; i < slotAreas.length; i++) {
                if (slotAreas[i] != null && slotAreas[i].contains(mouseX, mouseY)) {
                    selected = i;
                }
            }
        }
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (selected != before) {
            Sfx.play(Sfx.MOVE);
        }
    }

    public void render(float worldW, float worldH) {
        if (!open) {
            return;
        }
        float panelW = Items.SLOTS * SLOT_SIZE + (Items.SLOTS - 1) * GAP + 2 * PADDING;
        float px = worldW / 2 - panelW / 2, py = worldH / 2 - PANEL_H / 2;
        float slotY = py + PANEL_H - 90 - SLOT_SIZE;

        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        GameApp.drawRect(0, 0, worldW, worldH, DARKEN);
        GameApp.drawRect(px, py, panelW, PANEL_H, PANEL);
        for (int i = 0; i < Items.SLOTS; i++) {
            float sx = px + PADDING + i * (SLOT_SIZE + GAP);
            slotAreas[i] = new Rectangle(sx, slotY, SLOT_SIZE, SLOT_SIZE);
            GameApp.drawRect(sx, slotY, SLOT_SIZE, SLOT_SIZE, SLOT);
        }
        // small marker under the selected place
        float mx = px + PADDING + selected * (SLOT_SIZE + GAP) + SLOT_SIZE / 2;
        GameApp.setColor(MARKER);
        GameApp.getShapeRenderer().triangle(mx - 8, slotY - 10, mx + 8, slotY - 10, mx, slotY - 2);
        GameApp.endShapeRendering();

        GameApp.setLineWidth(1.5f);
        GameApp.startShapeRenderingOutlined();
        GameApp.drawRect(px, py, panelW, PANEL_H, LINE);
        GameApp.drawRect(px + 5, py + 5, panelW - 10, PANEL_H - 10, INNER);
        for (int i = 0; i < Items.SLOTS; i++) {
            Rectangle r = slotAreas[i];
            GameApp.drawRect(r.x, r.y, r.width, r.height, i == selected ? LINE : INNER);
        }
        GameApp.endShapeRendering();
        GameApp.setLineWidth(1f);

        GameApp.startSpriteRendering();
        GameApp.drawTextCentered(DialogueBox.NAME_FONT, "Memories  " + Items.count() + " / " + Items.SLOTS,
                worldW / 2, py + PANEL_H - 40, TEXT);
        for (int i = 0; i < Items.SLOTS; i++) {
            Rectangle r = slotAreas[i];
            Items.Item item = Items.inSlot(i);
            if (item != null && Items.has(item)) {
                float pad = 10;
                GameApp.drawTexture(item.texture(), r.x + pad, r.y + pad, r.width - 2 * pad, r.height - 2 * pad);
            } else {
                GameApp.drawTextCentered(DialogueBox.NAME_FONT, "?", r.x + r.width / 2, r.y + r.height / 2, INNER);
            }
            GameApp.drawTextCentered(DialogueBox.TEXT_FONT, "Floor " + (i + 1), r.x + r.width / 2, r.y - 26,
                    i == selected ? TEXT : DIM);
        }
        Items.Item chosen = Items.inSlot(selected);
        String name, text;
        if (chosen != null && Items.has(chosen)) {
            name = chosen.name;
            text = chosen.description;
        } else {
            name = "???";
            text = "Not found yet.";
        }
        GameApp.drawTextCentered(DialogueBox.NAME_FONT, name, worldW / 2, py + 100, TEXT);
        GameApp.drawTextCentered(DialogueBox.TEXT_FONT, text, worldW / 2, py + 66, DIM);
        GameApp.drawTextCentered(DialogueBox.TEXT_FONT, "I / Esc: close", worldW / 2, py + 28, INNER);
        GameApp.endSpriteRendering();
    }
}
