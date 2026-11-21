package nl.saxion.game.eclosion.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import nl.saxion.game.eclosion.audio.Sfx;
import nl.saxion.gameapp.GameApp;

/**
 * THE NIGHT PASSING (Dawon). Shown while the screen is black after Ageha falls asleep
 * (BedCutscene, step sleep()):
 *
 * 1. A big clock comes out of the dark. Its hands race through the night (23:00 -> 7:00),
 *    a tick on every hour, higher and faster, with little ticks while the minute hand spins.
 * 2. "Ding": morning. A ring of light spreads out, the light behind the clock turns warm,
 *    the moon in the clock turns into a sun.
 * 3. The old day slides up and away and the new one slides in: "DAY 2   14 June / 6 days left".
 * 4. Everything fades back to black, and BedCutscene lets him wake up.
 *
 * The week runs 13-19 June; Day 7 (19 June) is his 18th birthday (story bible 2.1).
 * The day number lives here: NightClock.day (1 on a new game, +1 every night).
 * Enter / Space / E / click skips to the end.
 * Sounds (resources/audio, made by Dawon): sfx_tick, sfx_clock_tick_fast, sfx_clock_ding
 */
public class NightClock {

    /** The day it is now (1 = the first day of the week). */
    public static int day = 1;

    /** New game: back to day 1. */
    public static void reset() {
        day = 1;
    }

    // the game's colours (same as the dialogue box)
    private static final Color TEXT = new Color(232 / 255f, 228 / 255f, 220 / 255f, 1f);
    private static final Color DIM = new Color(169 / 255f, 163 / 255f, 181 / 255f, 1f);
    private static final Color LINE = new Color(138 / 255f, 135 / 255f, 144 / 255f, 1f);
    private static final Color INNER = new Color(60 / 255f, 58 / 255f, 66 / 255f, 1f);
    private static final Color RED = new Color(150 / 255f, 60 / 255f, 70 / 255f, 1f);
    private static final Color FACE = new Color(14 / 255f, 13 / 255f, 17 / 255f, 1f);

    // ---------- timeline (seconds) ----------
    private static final float T_SPIN = 0.9f;       // the hands start racing
    private static final float T_DING = 4.9f;       // 7:00 - ding, morning
    private static final float T_DATE = 5.3f;       // old date out, new date in
    private static final float T_FADE = 8.0f;       // everything fades back to black
    private static final float LENGTH = 8.8f;

    private static final float START_HOUR = 23f, END_HOUR = 31f;   // 23:00 -> 7:00 next morning
    private static final float CLOCK_R = 190;

    private static final String BIG_FONT = "clock_big", MID_FONT = "clock_mid";
    private static final String TICK = "clock_tick", FAST = "clock_fast", DING = "clock_ding";

    private boolean running = false, done = false;
    private float t;
    private boolean dinged;
    private int lastHour;
    private float ring;
    private int fromDay, toDay;

    public boolean isRunning() {
        return running;
    }

    public boolean isDone() {
        return done;
    }

    /** Starts the night: loads its fonts and sounds. */
    public void start() {
        GameApp.addFont(BIG_FONT, PixelFont.make(48));
        GameApp.addFont(MID_FONT, PixelFont.make(26));
        GameApp.addSound(TICK, "audio/sfx_tick.wav");
        GameApp.addSound(FAST, "audio/sfx_clock_tick_fast.wav");
        GameApp.addSound(DING, "audio/sfx_clock_ding.wav");
        running = true;
        done = false;
        t = 0;
        dinged = false;
        lastHour = (int) START_HOUR;
        ring = 0;
        fromDay = day;
        toDay = day + 1;
    }

    public void update(float delta) {
        if (!running) {
            return;
        }
        t += delta;

        int hour = (int) Math.floor(clockHour());
        if (t > T_SPIN && t < T_DING && hour != lastHour) {
            lastHour = hour;
            float p = (hour - START_HOUR) / (END_HOUR - START_HOUR);
            play(TICK, 0.5f, 0.9f + 0.4f * p);
        }
        if (t > T_SPIN + 0.3f && t < T_DING - 0.3f && ((int) (t * 14)) != ((int) ((t - delta) * 14))) {
            play(FAST, 0.12f, 1f + (float) Math.random() * 0.1f);
        }
        if (!dinged && t > T_DING) {
            play(DING, 0.8f, 1f);
            dinged = true;
        }
        if (dinged) ring = Math.min(1f, ring + delta / 1.4f);

        boolean skip = GameApp.isKeyJustPressed(Input.Keys.ENTER) || GameApp.isKeyJustPressed(Input.Keys.SPACE)
                || GameApp.isKeyJustPressed(Input.Keys.E) || GameApp.isButtonJustPressed(Input.Buttons.LEFT);
        if (skip && t > 1f && t < T_FADE) {
            t = T_FADE;
            dinged = true;
        }
        if (t >= LENGTH) {
            finish();
        }
    }

    private void finish() {
        running = false;
        done = true;
        day = toDay;
        GameApp.disposeFont(BIG_FONT);
        GameApp.disposeFont(MID_FONT);
        GameApp.disposeSound(TICK);
        GameApp.disposeSound(FAST);
        GameApp.disposeSound(DING);
    }

    private void play(String sound, float volume, float pitch) {
        GameApp.getSound(sound).play(volume * Sfx.volume, pitch, 0f);
    }

    private static float clamp01(float v) { return Math.max(0, Math.min(1, v)); }

    private static float smooth(float v) {
        v = clamp01(v);
        return v * v * (3 - 2 * v);
    }

    /** The time the clock shows, in hours (23 .. 31 = 7:00 the next morning). */
    private float clockHour() {
        if (t < T_SPIN) return START_HOUR;
        return START_HOUR + (END_HOUR - START_HOUR) * smooth((t - T_SPIN) / (T_DING - T_SPIN));
    }

    /** Draw on a black screen (over the fader's black). */
    public void render(float w, float h) {
        if (!running) {
            return;
        }
        float a = smooth(t / 0.8f) * (1 - smooth((t - T_FADE) / (LENGTH - T_FADE)));
        float morning = smooth((t - T_DING) / 1.2f);
        float cx = w / 2, cy = h / 2 + 50;

        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        GameApp.drawRect(-10, -10, w + 20, h + 20, Color.BLACK);
        if (morning > 0) {
            for (int i = 0; i < 8; i++) {
                GameApp.drawCircle(cx, cy, CLOCK_R + 30 + i * 45, new Color(1f, 0.78f, 0.5f, 0.025f * morning * a));
            }
        }
        GameApp.endShapeRendering();

        drawClock(cx, cy, a, morning);
        if (t > T_DATE) {
            drawDate(w, a);
        }
        GameApp.disableTransparency();
    }

    private void drawClock(float cx, float cy, float a, float morning) {
        float r = CLOCK_R * (0.85f + 0.15f * smooth(t / 0.8f));
        float hour = clockHour();
        ShapeRenderer shapes = GameApp.getShapeRenderer();

        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        GameApp.drawCircle(cx, cy, r + 10, new Color(LINE.r, LINE.g, LINE.b, a));
        GameApp.drawCircle(cx, cy, r + 6, new Color(FACE.r, FACE.g, FACE.b, a));
        GameApp.drawCircle(cx, cy, r - 14, new Color(INNER.r * 0.5f, INNER.g * 0.5f, INNER.b * 0.5f, a));
        GameApp.drawCircle(cx, cy, r - 17, new Color(FACE.r, FACE.g, FACE.b, a));
        // hour marks: the quarters longer and red
        for (int i = 0; i < 12; i++) {
            double ang = Math.toRadians(90 - i * 30);
            boolean quarter = i % 3 == 0;
            float in = r - (quarter ? 44 : 34), out = r - 22;
            Color c = quarter ? RED : LINE;
            shapes.setColor(c.r, c.g, c.b, a);
            shapes.rectLine(cx + (float) Math.cos(ang) * in, cy + (float) Math.sin(ang) * in,
                    cx + (float) Math.cos(ang) * out, cy + (float) Math.sin(ang) * out, quarter ? 6 : 3);
        }
        // the moon (night) turns into a sun (morning)
        float my = cy - r * 0.42f;
        if (morning < 1) {
            GameApp.drawCircle(cx, my, 18, new Color(0.75f, 0.78f, 0.9f, a * (1 - morning)));
            GameApp.drawCircle(cx + 8, my + 5, 16, new Color(FACE.r, FACE.g, FACE.b, a * (1 - morning)));
        }
        if (morning > 0) {
            GameApp.drawCircle(cx, my, 15, new Color(1f, 0.8f, 0.5f, a * morning));
            for (int i = 0; i < 8; i++) {
                double ang = Math.toRadians(i * 45 + t * 20);
                shapes.setColor(1f, 0.8f, 0.5f, a * morning);
                shapes.rectLine(cx + (float) Math.cos(ang) * 21, my + (float) Math.sin(ang) * 21,
                        cx + (float) Math.cos(ang) * 28, my + (float) Math.sin(ang) * 28, 3);
            }
        }
        // hands
        double hourAng = Math.toRadians(90 - (hour % 12) * 30);
        double minAng = Math.toRadians(90 - (hour % 1) * 360);
        shapes.setColor(TEXT.r, TEXT.g, TEXT.b, a);
        shapes.rectLine(cx, cy, cx + (float) Math.cos(hourAng) * r * 0.5f, cy + (float) Math.sin(hourAng) * r * 0.5f, 10);
        shapes.setColor(DIM.r, DIM.g, DIM.b, a);
        shapes.rectLine(cx, cy, cx + (float) Math.cos(minAng) * r * 0.78f, cy + (float) Math.sin(minAng) * r * 0.78f, 5);
        GameApp.drawCircle(cx, cy, 12, new Color(RED.r, RED.g, RED.b, a));
        GameApp.drawCircle(cx, cy, 5, new Color(TEXT.r, TEXT.g, TEXT.b, a));
        GameApp.endShapeRendering();

        // the ding: a ring spreading out
        if (ring > 0 && ring < 1) {
            GameApp.setLineWidth(3f);
            GameApp.startShapeRenderingOutlined();
            GameApp.drawCircle(cx, cy, r + 10 + ring * 220, new Color(1f, 0.9f, 0.75f, 0.7f * (1 - ring) * a));
            GameApp.drawCircle(cx, cy, r + 10 + ring * 120, new Color(1f, 0.9f, 0.75f, 0.4f * (1 - ring) * a));
            GameApp.endShapeRendering();
            GameApp.setLineWidth(1f);
        }

        int hh = ((int) Math.floor(hour)) % 24;
        int mm = (int) Math.floor((hour % 1) * 60);
        GameApp.startSpriteRendering();
        GameApp.drawTextCentered(MID_FONT, String.format("%02d:%02d", hh, mm), cx, cy - r - 40,
                new Color(DIM.r, DIM.g, DIM.b, a));
        GameApp.endSpriteRendering();
    }

    /** The old day slides up and away, the new day slides in from below. */
    private void drawDate(float w, float a) {
        float p = smooth((t - T_DATE) / 0.7f);
        float cx = w / 2, baseY = 90;
        GameApp.startSpriteRendering();
        if (p < 1) {
            GameApp.drawTextCentered(BIG_FONT, dateLine(fromDay), cx, baseY + 40 * p, new Color(DIM.r, DIM.g, DIM.b, (1 - p) * a));
        }
        GameApp.drawTextCentered(BIG_FONT, dateLine(toDay), cx, baseY - 30 * (1 - p), new Color(TEXT.r, TEXT.g, TEXT.b, p * a));
        float q = smooth((t - T_DATE - 0.6f) / 0.6f);
        if (q > 0) {
            int left = 8 - toDay;   // counting the birthday itself
            String countdown = left <= 1 ? "Today is the day." : left + " days left";
            GameApp.drawTextCentered(MID_FONT, countdown, cx, baseY - 50, new Color(RED.r, RED.g, RED.b, q * a));
        }
        GameApp.endSpriteRendering();
    }

    private static String dateLine(int d) {
        return "DAY " + d + "   " + (12 + d) + " June";
    }
}
