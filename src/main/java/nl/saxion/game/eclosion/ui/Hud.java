package nl.saxion.game.eclosion.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import nl.saxion.game.eclosion.explore.TimeOfDay;
import nl.saxion.gameapp.GameApp;

/**
 * The little things always on screen while Ageha walks around (Dawon):
 *
 *   top left     - the day: a slanted dark band with "DAY 2", the date and how many days are left,
 *                  a small moon (night) or sun (day) next to it, a thin red stripe under it.
 *   bottom right - "[I] Collection", so the player knows the memory items can be looked at.
 *
 * Both fade out while something else is on screen (dialogue, a menu, a cutscene) and come back after.
 * ExploreScreen: loadAssets() in show(), disposeAssets() in hide(), render(...) after the night is drawn.
 * The day number comes from NightClock.day; the week runs 13-19 June, Day 7 is the birthday.
 */
public final class Hud {
    private static final String DAY_FONT = "hud_day", SMALL_FONT = "hud_small";

    private static final Color BAND = new Color(10 / 255f, 10 / 255f, 14 / 255f, 0.82f);
    private static final Color TEXT = new Color(232 / 255f, 228 / 255f, 220 / 255f, 1f);
    private static final Color DIM = new Color(169 / 255f, 163 / 255f, 181 / 255f, 1f);
    private static final Color LINE = new Color(138 / 255f, 135 / 255f, 144 / 255f, 1f);
    private static final Color RED = new Color(150 / 255f, 60 / 255f, 70 / 255f, 1f);
    private static final Color RED_LIGHT = new Color(200 / 255f, 90 / 255f, 100 / 255f, 1f);
    private static final Color MOON = new Color(0.78f, 0.8f, 0.92f, 1f);
    private static final Color SUN = new Color(1f, 0.8f, 0.5f, 1f);

    private static final float FADE_SPEED = 4f;   // 1 / seconds to fade in or out

    private static final GlyphLayout layout = new GlyphLayout();
    private static float alpha = 0;

    private Hud() {
    }

    public static void loadAssets() {
        GameApp.addFont(DAY_FONT, PixelFont.make(40));
        GameApp.addFont(SMALL_FONT, PixelFont.make(18));
        alpha = 0;   // fades in on a new screen
    }

    public static void disposeAssets() {
        GameApp.disposeFont(DAY_FONT);
        GameApp.disposeFont(SMALL_FONT);
    }

    /** "13 June" for day 1 ... "19 June" for day 7. */
    public static String date(int day) {
        return (12 + day) + " June";
    }

    /** "6 days left" ... "Birthday" (counting the birthday itself, like the night clock). */
    public static String daysLeft(int day) {
        int left = 7 - day;
        if (left <= 0) return "Birthday";
        return left == 1 ? "1 day left" : left + " days left";
    }

    /**
     * Draw the HUD. visible = nothing else is on screen right now (no dialogue, menu or cutscene).
     */
    public static void render(float delta, float worldW, float worldH, boolean visible) {
        alpha = visible ? Math.min(1f, alpha + delta * FADE_SPEED) : Math.max(0f, alpha - delta * FADE_SPEED);
        if (alpha <= 0) {
            return;
        }
        GameApp.enableTransparency();
        drawDay(worldH, alpha);
        drawCollectionHint(worldW, alpha);
        GameApp.disableTransparency();
    }

    // ---------- top left: the day ----------
    private static void drawDay(float worldH, float a) {
        int day = Math.max(1, Math.min(7, NightClock.day));
        boolean night = TimeOfDay.isNight();
        float top = worldH - 14, bandH = 74, bandW = 330, slant = 26;
        float slide = (1 - a) * -40;   // slides in a little from the left while fading in
        ShapeRenderer shapes = GameApp.getShapeRenderer();

        GameApp.startShapeRenderingFilled();
        // the dark band: straight on the left (off screen), slanted on the right
        shapes.setColor(BAND.r, BAND.g, BAND.b, BAND.a * a);
        float x0 = -10 + slide, y0 = top - bandH, x1 = bandW + slide;
        shapes.triangle(x0, y0, x1, y0, x1 + slant, top);
        shapes.triangle(x0, y0, x1 + slant, top, x0, top);
        // thin red stripe under it, the same slant
        shapes.setColor(RED.r, RED.g, RED.b, a);
        float sy = y0 - 7, sh = 4, sr = x1 - 8, srTop = sr + slant * sh / bandH;
        shapes.triangle(x0, sy, sr, sy, srTop, sy + sh);
        shapes.triangle(x0, sy, srTop, sy + sh, x0, sy + sh);

        // moon (night) or sun (day)
        float ix = 34 + slide, iy = top - bandH / 2;
        if (night) {
            shapes.setColor(MOON.r, MOON.g, MOON.b, a);
            shapes.circle(ix, iy, 13, 24);
            shapes.setColor(BAND.r, BAND.g, BAND.b, a);   // cut a crescent out of it
            shapes.circle(ix + 6, iy + 4, 11, 24);
        } else {
            shapes.setColor(SUN.r, SUN.g, SUN.b, a);
            shapes.circle(ix, iy, 9, 20);
            for (int i = 0; i < 8; i++) {
                double ang = Math.toRadians(i * 45);
                shapes.rectLine(ix + (float) Math.cos(ang) * 13, iy + (float) Math.sin(ang) * 13,
                        ix + (float) Math.cos(ang) * 18, iy + (float) Math.sin(ang) * 18, 2.5f);
            }
        }
        GameApp.endShapeRendering();

        // "DAY 2" big, the date and the days left small next to it
        GameApp.startSpriteRendering();
        float tx = 62 + slide;
        float dayW = drawLeft(DAY_FONT, "DAY " + day, tx, iy + 2, TEXT, a);
        float sx = tx + dayW + 16;
        drawLeft(SMALL_FONT, date(day).toUpperCase(), sx, iy + 11, DIM, a);
        drawLeft(SMALL_FONT, daysLeft(day).toUpperCase(), sx, iy - 12, RED_LIGHT, a);
        GameApp.endSpriteRendering();
    }

    // ---------- bottom right: "[I] Collection" ----------
    private static void drawCollectionHint(float worldW, float a) {
        float a2 = a * 0.85f;
        String label = "Collection";
        float labelW = textWidth(SMALL_FONT, label);
        float key = 26, gap = 10, margin = 20;
        float right = worldW - margin, cy = margin + key / 2;
        float keyX = right - labelW - gap - key;

        // the key cap: dark square with a light border
        GameApp.startShapeRenderingFilled();
        GameApp.drawRect(keyX, cy - key / 2, key, key, new Color(BAND.r, BAND.g, BAND.b, BAND.a * a2));
        GameApp.endShapeRendering();
        GameApp.setLineWidth(1.5f);
        GameApp.startShapeRenderingOutlined();
        GameApp.drawRect(keyX, cy - key / 2, key, key, new Color(LINE.r, LINE.g, LINE.b, a2));
        GameApp.endShapeRendering();
        GameApp.setLineWidth(1f);

        GameApp.startSpriteRendering();
        drawCentered(SMALL_FONT, "I", keyX + key / 2, cy, TEXT, a2);
        drawLeft(SMALL_FONT, label, keyX + key + gap, cy, TEXT, a2);
        GameApp.endSpriteRendering();
    }

    // ---------- text helpers (y = the middle of the text) ----------
    private static float drawLeft(String fontName, String text, float x, float y, Color c, float a) {
        BitmapFont font = GameApp.getFont(fontName);
        font.setColor(c.r, c.g, c.b, a);
        layout.setText(font, text);
        font.draw(GameApp.getSpriteBatch(), layout, Math.round(x), Math.round(y + layout.height / 2));
        font.setColor(Color.WHITE);
        return layout.width;
    }

    private static void drawCentered(String fontName, String text, float x, float y, Color c, float a) {
        BitmapFont font = GameApp.getFont(fontName);
        font.setColor(c.r, c.g, c.b, a);
        layout.setText(font, text);
        font.draw(GameApp.getSpriteBatch(), layout, Math.round(x - layout.width / 2), Math.round(y + layout.height / 2));
        font.setColor(Color.WHITE);
    }

    private static float textWidth(String fontName, String text) {
        layout.setText(GameApp.getFont(fontName), text);
        return layout.width;
    }
}
