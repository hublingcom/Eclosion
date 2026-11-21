package nl.saxion.game.eclosion.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import nl.saxion.game.eclosion.audio.Sfx;
import nl.saxion.game.eclosion.items.Items;
import nl.saxion.gameapp.GameApp;

/**
 * "You found something" (Dawon): the screen darkens, the item's picture rises into a framed panel
 * with its name and a short text. Enter / Space / E / click closes it.
 * Same colours and fonts as the dialogue box (DialogueBox.loadAssets() must have been called).
 *
 *   popup.open(Items.HAIR_TIE);
 *   render(): popup.update(delta); ... popup.render(worldW, worldH);   while popup.isOpen()
 */
public class ItemPopup {
    private static final Color DARKEN = new Color(0, 0, 0, 0.7f);
    private static final Color PANEL = new Color(8 / 255f, 8 / 255f, 10 / 255f, 0.94f);
    private static final Color LINE = new Color(138 / 255f, 135 / 255f, 144 / 255f, 1f);
    private static final Color INNER = new Color(60 / 255f, 58 / 255f, 66 / 255f, 1f);
    private static final Color TEXT = new Color(232 / 255f, 228 / 255f, 220 / 255f, 1f);
    private static final Color DIM = new Color(169 / 255f, 163 / 255f, 181 / 255f, 1f);
    private static final Color RED = new Color(150 / 255f, 60 / 255f, 70 / 255f, 1f);

    private static final float PANEL_W = 520, PANEL_H = 470, IMAGE = 200;

    private Items.Item item = null;
    private float time = 0;

    public void open(Items.Item newItem) {
        item = newItem;
        item.load();
        time = 0;
        Sfx.play(Sfx.KEY_RIGHT, 0.4f, 0f);    // a small bright "found it" sound
    }

    public boolean isOpen() {
        return item != null;
    }

    public void update(float delta) {
        if (item == null) {
            return;
        }
        time += delta;
        boolean close = GameApp.isKeyJustPressed(Input.Keys.ENTER) || GameApp.isKeyJustPressed(Input.Keys.SPACE)
                || GameApp.isKeyJustPressed(Input.Keys.E) || GameApp.isButtonJustPressed(Input.Buttons.LEFT);
        if (close && time > 0.6f) {
            Sfx.play(Sfx.CONFIRM);
            item = null;
        }
    }

    public void render(float worldW, float worldH) {
        if (item == null) {
            return;
        }
        float t = Math.min(1f, time / 0.5f);
        float ease = 1 - (1 - t) * (1 - t);
        float px = worldW / 2 - PANEL_W / 2;
        float py = worldH / 2 - PANEL_H / 2 - 20 * (1 - ease);

        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        GameApp.drawRect(0, 0, worldW, worldH, new Color(0, 0, 0, DARKEN.a * ease));
        GameApp.drawRect(px, py, PANEL_W, PANEL_H, new Color(PANEL.r, PANEL.g, PANEL.b, PANEL.a * ease));
        // soft glow behind the picture
        float cx = worldW / 2, cy = py + PANEL_H - 70 - IMAGE / 2;
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 2.2f);
        for (int i = 0; i < 5; i++) {
            GameApp.drawCircle(cx, cy, IMAGE * 0.35f + i * 18, new Color(1f, 0.9f, 0.85f, 0.025f * ease * (0.7f + 0.3f * pulse)));
        }
        GameApp.endShapeRendering();

        GameApp.setLineWidth(1.5f);
        GameApp.startShapeRenderingOutlined();
        GameApp.drawRect(px, py, PANEL_W, PANEL_H, new Color(LINE.r, LINE.g, LINE.b, ease));
        GameApp.drawRect(px + 5, py + 5, PANEL_W - 10, PANEL_H - 10, new Color(INNER.r, INNER.g, INNER.b, ease));
        GameApp.endShapeRendering();
        GameApp.setLineWidth(1f);

        GameApp.startSpriteRendering();
        GameApp.drawTextCentered(DialogueBox.NAME_FONT, "- Memory item -", cx, py + PANEL_H - 34, withAlpha(RED, ease));
        float rise = 14 * (1 - ease);
        GameApp.getSpriteBatch().setColor(1, 1, 1, ease);
        GameApp.drawTexture(item.texture(), cx - IMAGE / 2, cy - IMAGE / 2 - rise, IMAGE, IMAGE);
        GameApp.getSpriteBatch().setColor(Color.WHITE);
        GameApp.drawTextCentered(DialogueBox.NAME_FONT, item.name, cx, py + 150, withAlpha(TEXT, ease));
        GameApp.drawTextCentered(DialogueBox.TEXT_FONT, item.floor, cx, py + 116, withAlpha(DIM, ease));
        drawWrapped(item.description, cx, py + 80, PANEL_W - 80, withAlpha(DIM, ease));
        if (time > 0.6f && (time % 1f) < 0.6f) {
            GameApp.drawTextCentered(DialogueBox.TEXT_FONT, "Added to your collection  [ I ]", cx, py + 26,
                    withAlpha(LINE, ease));
        }
        GameApp.endSpriteRendering();
    }

    /** Centred text in up to two lines (splits at the space nearest to the middle if it is too wide). */
    private void drawWrapped(String text, float cx, float y, float maxW, Color color) {
        com.badlogic.gdx.graphics.g2d.GlyphLayout layout =
                new com.badlogic.gdx.graphics.g2d.GlyphLayout(GameApp.getFont(DialogueBox.TEXT_FONT), text);
        if (layout.width <= maxW) {
            GameApp.drawTextCentered(DialogueBox.TEXT_FONT, text, cx, y, color);
            return;
        }
        int mid = text.length() / 2, split = text.lastIndexOf(' ', mid);
        if (split < 0) split = text.indexOf(' ', mid);
        if (split < 0) {
            GameApp.drawTextCentered(DialogueBox.TEXT_FONT, text, cx, y, color);
            return;
        }
        GameApp.drawTextCentered(DialogueBox.TEXT_FONT, text.substring(0, split), cx, y + 14, color);
        GameApp.drawTextCentered(DialogueBox.TEXT_FONT, text.substring(split + 1), cx, y - 16, color);
    }

    private static Color withAlpha(Color c, float a) {
        return new Color(c.r, c.g, c.b, c.a * a);
    }
}
