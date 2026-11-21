package nl.saxion.game.eclosion.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import nl.saxion.game.eclosion.audio.Sfx;
import nl.saxion.gameapp.GameApp;

/**
 * THE NIGHT PASSING (Dawon). Shown while the screen is black after Ageha falls asleep
 * (BedCutscene, step sleep()):
 *
 * 1. A hazy night sky, a big pixel moon on the right with a dashed red ring of hours (23 .. 06),
 *    the time racing in huge glowing digits top left (23:00 -> 7:00, a tick on every hour, higher and faster),
 *    and a slanted band sliding in at the bottom with "DAY", the red day number, the date and the countdown.
 *    The moon grows a little fuller every night: full moon on Day 7, his birthday.
 * 2. 06:00: a deep grandfather-clock chime strikes four times, slowly ("dong ... dong ..."). With every
 *    strike a red shock ring goes out from the moon (no full-screen flash) and the dawn grows: the moon turns blood red with a red glow,
 *    red mist rises behind the band, the last hours of the ring burn bright red, the time gets a red edge.
 * 3. On the band the old day number rolls up and away and the new one rolls in ("DAY 2  14 JUNE").
 * 4. Everything fades back to black, and BedCutscene lets him wake up.
 *
 * The week runs 13-19 June; Day 7 (19 June) is his 18th birthday (story bible 2.1).
 * The day number lives here: NightClock.day (1 on a new game, +1 every night).
 * Enter / Space / E / click skips to the end.
 * Sounds (resources/audio, made by Dawon): sfx_tick, sfx_clock_tick_fast, sfx_clock_chime
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
    private static final Color RED_LIGHT = new Color(200 / 255f, 90 / 255f, 100 / 255f, 1f);
    private static final Color BAND = new Color(18 / 255f, 18 / 255f, 26 / 255f, 1f);
    private static final Color NIGHT_SKY = new Color(0.09f, 0.12f, 0.26f, 1f);
    private static final Color DAWN_SKY = new Color(0.20f, 0.28f, 0.46f, 1f);     // a bit lighter, greyer blue
    private static final Color DAWN_RED = new Color(0.95f, 0.22f, 0.25f, 1f);     // the burning red of the dawn
    private static final Color DAWN_MIST = new Color(0.55f, 0.16f, 0.20f, 1f);
    private static final Color DAWN_TEXT = new Color(0.98f, 0.55f, 0.45f, 1f);
    private static final Color HAZE = new Color(0.30f, 0.34f, 0.48f, 1f);
    private static final Color CLOUD = new Color(0.02f, 0.03f, 0.06f, 1f);

    // ---------- timeline (seconds) ----------
    private static final float T_SPIN = 0.9f;       // the hands start racing
    private static final float T_DING = 4.9f;       // 06:00 - the first strike of the chime
    private static final int CHIMES = 4;            // how many times the clock strikes
    private static final float CHIME_GAP = 1.25f;   // seconds between the strikes (slow, heavy)
    private static final float T_DATE = 6.2f;       // old date out, new date in (after the second strike)
    private static final float T_FADE = 10.4f;      // everything fades back to black
    private static final float LENGTH = 11.4f;

    private static final float START_HOUR = 23f, END_HOUR = 30f;   // 23:00 -> 06:00 next morning (the end of the ring)

    private static final String HUGE_FONT = "clock_huge", DAY_FONT = "clock_day", BIG_FONT = "clock_big",
            MID_FONT = "clock_mid", SMALL_FONT = "clock_small";
    private final GlyphLayout layout = new GlyphLayout();
    private static final String TICK = "clock_tick", FAST = "clock_fast", CHIME = "clock_chime";

    private boolean running = false, done = false;
    private float t;
    private boolean dinged;
    private int chimesPlayed;
    private int lastHour;
    private int fromDay, toDay;

    public boolean isRunning() {
        return running;
    }

    public boolean isDone() {
        return done;
    }

    /** Starts the night: loads its fonts and sounds. */
    public void start() {
        GameApp.addFont(HUGE_FONT, PixelFont.make(136));
        GameApp.addFont(DAY_FONT, PixelFont.make(120));
        GameApp.addFont(BIG_FONT, PixelFont.make(46));
        GameApp.addFont(MID_FONT, PixelFont.make(22));
        GameApp.addFont(SMALL_FONT, PixelFont.make(20));
        GameApp.addSound(TICK, "audio/sfx_tick.wav");
        GameApp.addSound(FAST, "audio/sfx_clock_tick_fast.wav");
        GameApp.addSound(CHIME, "audio/sfx_clock_chime.wav");
        running = true;
        done = false;
        t = 0;
        dinged = false;
        chimesPlayed = 0;
        lastHour = (int) START_HOUR;
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
        // 06:00: the grandfather clock strikes, slowly; the last strike a little lower
        if (t > T_DING) {
            dinged = true;
            int due = Math.min(CHIMES, (int) Math.floor((t - T_DING) / CHIME_GAP) + 1);
            while (chimesPlayed < due) {
                chimesPlayed++;
                play(CHIME, 0.9f, chimesPlayed == CHIMES ? 0.94f : 1f);
            }
        }

        boolean skip = GameApp.isKeyJustPressed(Input.Keys.ENTER) || GameApp.isKeyJustPressed(Input.Keys.SPACE)
                || GameApp.isKeyJustPressed(Input.Keys.E) || GameApp.isButtonJustPressed(Input.Buttons.LEFT);
        if (skip && t > 1f && t < T_FADE) {
            t = T_FADE;
            dinged = true;
            chimesPlayed = CHIMES;   // skipped: no more strikes
        }
        if (t >= LENGTH) {
            finish();
        }
    }

    private void finish() {
        running = false;
        done = true;
        day = toDay;
        GameApp.disposeFont(HUGE_FONT);
        GameApp.disposeFont(DAY_FONT);
        GameApp.disposeFont(BIG_FONT);
        GameApp.disposeFont(MID_FONT);
        GameApp.disposeFont(SMALL_FONT);
        if (moonTexture != null) {
            moonTexture.dispose();
            moonTexture = null;
        }
        if (moonPixmap != null) {
            moonPixmap.dispose();
            moonPixmap = null;
        }
        if (bloodTexture != null) {
            bloodTexture.dispose();
            bloodTexture = null;
        }
        moonPhase = -1;
        GameApp.disposeSound(TICK);
        GameApp.disposeSound(FAST);
        GameApp.disposeSound(CHIME);
    }

    private void play(String sound, float volume, float pitch) {
        GameApp.getSound(sound).play(volume * Sfx.volume, pitch, 0f);
    }

    private static float clamp01(float v) { return Math.max(0, Math.min(1, v)); }

    private static float smooth(float v) {
        v = clamp01(v);
        return v * v * (3 - 2 * v);
    }

    /** The time the clock shows, in hours (23 .. 30 = 06:00 the next morning). */
    private float clockHour() {
        if (t < T_SPIN) return START_HOUR;
        return START_HOUR + (END_HOUR - START_HOUR) * smooth((t - T_SPIN) / (T_DING - T_SPIN));
    }

    // =====================================================================================
    // DRAWING (after the team's concept picture): a dark, hazy night sky with scan lines,
    // a big pixel moon on the right with a dashed red ring of hours (23 .. 06) around it,
    // the racing time in huge glowing digits top left, and a slanted dark band at the bottom
    // with "DAY", the red day number, the date and the days until his birthday.
    // The moon grows a little fuller every night: full moon on Day 7, his birthday.
    // All positions are for the 1280 x 720 world.
    // =====================================================================================

    private static final float MOON_X = 944, MOON_Y = 450, MOON_R = 158;
    private static final float RING_R = 218, RING_W = 12;
    private static final float RING_START = 104, RING_END = -108;   // degrees: from just left of the top, clockwise
    private static final float DEG_PER_HOUR = 28.5f;                 // 23:00 at 100 degrees, 06:00 at -99.5
    private static final int DASHES = 20;
    private static final float BAND_LEFT_Y = 191, BAND_RIGHT_Y = 282; // the red line on top of the band

    /** Draw on a black screen (over the fader's black). */
    public void render(float w, float h) {
        if (!running) {
            return;
        }
        float a = smooth(t / 0.8f) * (1 - smooth((t - T_FADE) / (LENGTH - T_FADE)));
        float dawn = dawn();

        GameApp.enableTransparency();
        drawSky(w, h, a, dawn);
        drawBand(w, a, dawn);
        drawMoon(a, dawn);
        drawRing(a, dawn);
        drawTime(a, dawn);
        drawDay(a);
        drawScanLines(w, h, a);
        GameApp.disableTransparency();
    }

    /** 0 = night, 1 = the red dawn is complete. Grows in a step with every strike of the chime. */
    private float dawn() {
        if (t < T_DING) return 0;
        float steps = 0;
        for (int i = 0; i < CHIMES; i++) {
            steps += smooth((t - T_DING - i * CHIME_GAP) / 0.9f);
        }
        return steps / CHIMES;
    }

    /** 1 right at a strike, fading to 0 before the next one. */
    private float strikePulse() {
        if (t < T_DING) return 0;
        float since = (t - T_DING) % CHIME_GAP;
        int strike = (int) Math.floor((t - T_DING) / CHIME_GAP);
        if (strike >= CHIMES) since = t - T_DING - (CHIMES - 1) * CHIME_GAP;
        return 1 - smooth(since / 0.9f);
    }

    /** 0..1 how far the shock ring of the latest strike has spread (or -1 if there is none). */
    private float strikeWave() {
        if (t < T_DING) return -1;
        int strike = Math.min(CHIMES - 1, (int) Math.floor((t - T_DING) / CHIME_GAP));
        float since = t - T_DING - strike * CHIME_GAP;
        return since < 1.4f ? since / 1.4f : -1;
    }

    // ---------- sky: navy gradient, soft haze, a few stars; red mist at dawn ----------
    private void drawSky(float w, float h, float a, float morning) {
        GameApp.startShapeRenderingFilled();
        GameApp.drawRect(-10, -10, w + 20, h + 20, Color.BLACK);
        int rows = 72;
        for (int i = 0; i < rows; i++) {
            float k = i / (float) rows;                         // 0 = bottom, 1 = top
            float glow = (0.25f + 0.75f * k) * 0.55f * a;
            float r = lerp(NIGHT_SKY.r, DAWN_SKY.r, morning);
            float g = lerp(NIGHT_SKY.g, DAWN_SKY.g, morning);
            float b = lerp(NIGHT_SKY.b, DAWN_SKY.b, morning);
            GameApp.drawRect(-10, h * k, w + 20, h / rows + 1, new Color(r, g, b, glow));
        }
        // haze: soft lighter fog on the left, darker clouds at the top (slowly drifting)
        float drift = t * 6;
        blob(230 + drift, 330, 230, HAZE, 0.10f * a);
        blob(120 + drift, 520, 160, HAZE, 0.07f * a);
        blob(420 + drift * 0.7f, 250, 180, HAZE, 0.06f * a);
        blob(640 - drift, 650, 150, CLOUD, 0.25f * a);
        blob(560 - drift, 600, 110, CLOUD, 0.20f * a);
        blob(1180 - drift * 0.5f, 120, 200, HAZE, 0.05f * a);
        // dawn: red mist rising from behind the band, strongest under the moon
        if (morning > 0) {
            float rise = 20 * morning;
            blob(560, 300 + rise, 200, HAZE, 0.16f * morning * a);   // grey smoke over the horizon
            blob(260, 280 + rise, 220, HAZE, 0.14f * morning * a);
            blob(860, 290 + rise, 160, HAZE, 0.10f * morning * a);
            blob(900, 270 + rise, 230, DAWN_MIST, 0.13f * morning * a);   // red light low behind it
            blob(620, 250 + rise, 200, DAWN_MIST, 0.08f * morning * a);
            blob(300, 230 + rise, 200, DAWN_MIST, 0.06f * morning * a);
            blob(1180, 290 + rise, 160, DAWN_MIST, 0.08f * morning * a);
        }
        // stars, fading out in the morning
        java.util.Random stars = new java.util.Random(13);
        for (int i = 0; i < 46; i++) {
            float sx = stars.nextFloat() * w, sy = h * 0.3f + stars.nextFloat() * h * 0.7f;
            float twinkle = 0.5f + 0.5f * (float) Math.sin(t * 2 + i);
            float size = stars.nextFloat() < 0.2f ? 3 : 2;
            GameApp.drawRect(sx, sy, size, size, new Color(1, 1, 1, (0.2f + 0.35f * twinkle) * a * (1 - morning * 0.6f)));
        }
        GameApp.endShapeRendering();
    }

    /** A soft round blob: circles from big and faint to small. */
    private void blob(float x, float y, float r, Color c, float alpha) {
        for (int i = 0; i < 7; i++) {
            float k = 1 - i / 7f;
            GameApp.drawCircle(x, y, r * (0.35f + 0.65f * k), new Color(c.r, c.g, c.b, alpha / 3.5f));
        }
    }

    // ---------- the band at the bottom ----------
    private void drawBand(float w, float a, float dawn) {
        float in = smooth((t - 0.2f) / 0.7f);
        float off = -(1 - in) * (w + 200);
        ShapeRenderer shapes = GameApp.getShapeRenderer();
        float lx = -20 + off, rx = w + 20 + off;
        float ly = bandY(lx - off), ry = bandY(rx - off);

        GameApp.startShapeRenderingFilled();
        shapes.setColor(BAND.r, BAND.g, BAND.b, 0.95f * a);
        shapes.triangle(lx, -10, rx, -10, rx, ry);
        shapes.triangle(lx, -10, rx, ry, lx, ly);
        // darker towards the bottom
        for (int i = 0; i < 12; i++) {
            GameApp.drawRect(lx, i * 8 - 10, rx - lx, 8, new Color(0, 0, 0, (12 - i) / 12f * 0.35f * a));
        }
        // the thin red line on top (glowing at dawn)
        if (dawn > 0) {
            shapes.setColor(DAWN_RED.r, DAWN_RED.g, DAWN_RED.b, 0.10f * dawn * a);
            shapes.rectLine(lx, ly, rx, ry, 16);
            shapes.setColor(DAWN_RED.r, DAWN_RED.g, DAWN_RED.b, 0.22f * dawn * a);
            shapes.rectLine(lx, ly, rx, ry, 8);
        }
        shapes.setColor(lerp(RED_LIGHT.r, DAWN_RED.r, dawn), lerp(RED_LIGHT.g, DAWN_RED.g, dawn),
                lerp(RED_LIGHT.b, DAWN_RED.b, dawn), 0.9f * a);
        shapes.rectLine(lx, ly, rx, ry, 3.5f);
        GameApp.endShapeRendering();

        // faint big letters along the bottom of the band, sliding slowly to the left
        GameApp.startSpriteRendering();
        String ghost = "ECLOSION  AGEHA  13-19 JUNE  WAKE UP  ECLOSION  AGEHA  13-19 JUNE  WAKE UP";
        drawLeft(BIG_FONT, ghost, off + 40 - t * 18, 14, TEXT, 0.08f * a);
        GameApp.endSpriteRendering();
    }

    /** y of the band's red line at x (it climbs to the right). */
    private static float bandY(float x) {
        return BAND_LEFT_Y + (BAND_RIGHT_Y - BAND_LEFT_Y) * (x / 1280f);
    }

    // ---------- the moon ----------
    private void drawMoon(float a, float morning) {
        float phaseDay = lerp(fromDay, toDay, smooth((t - T_SPIN) / (T_DING - T_SPIN)));
        updateMoonTexture(clamp01(phaseDay / 7f));
        float appear = 0.94f + 0.06f * smooth(t / 0.8f);
        float r = MOON_R * appear;

        // glow around it: faint and cold at night, a wide red halo at dawn
        GameApp.startShapeRenderingFilled();
        for (int i = 0; i < 6; i++) {
            GameApp.drawCircle(MOON_X, MOON_Y, r + 6 + i * 9, new Color(0.55f, 0.65f, 0.9f, 0.008f * a * (1 - morning)));
        }
        if (morning > 0) {
            float pulse = 1 + 0.25f * strikePulse();
            for (int i = 0; i < 7; i++) {
                GameApp.drawCircle(MOON_X, MOON_Y, r + 3 + i * 7 * pulse,
                        new Color(DAWN_RED.r, DAWN_RED.g, DAWN_RED.b, 0.05f * morning * a));
            }
        }
        GameApp.endShapeRendering();

        // the moon of tonight, turning into the blood moon of the dawn
        if (bloodTexture == null) {
            bloodTexture = makeBloodMoon();
        }
        GameApp.startSpriteRendering();
        GameApp.getSpriteBatch().setColor(1, 1, 1, a * (1 - morning));
        GameApp.getSpriteBatch().draw(moonTexture, MOON_X - r, MOON_Y - r, r * 2, r * 2);
        if (morning > 0) {
            GameApp.getSpriteBatch().setColor(1, 1, 1, a * morning);
            GameApp.getSpriteBatch().draw(bloodTexture, MOON_X - r, MOON_Y - r, r * 2, r * 2);
        }
        GameApp.getSpriteBatch().setColor(1, 1, 1, 1);
        GameApp.endSpriteRendering();

        // dawn: a thin burning rim on the lower right of the moon
        if (morning > 0) {
            ShapeRenderer shapes = GameApp.getShapeRenderer();
            GameApp.startShapeRenderingFilled();
            shapes.setColor(DAWN_RED.r, DAWN_RED.g, DAWN_RED.b, 0.55f * morning * a);
            arc(MOON_X, MOON_Y, 0, 360, r, 2.5f);                 // thin red outline all around
            shapes.setColor(DAWN_RED.r, DAWN_RED.g, DAWN_RED.b, 0.25f * morning * a);
            arc(MOON_X, MOON_Y, 30, -120, r + 2, 10);
            arc(MOON_X, MOON_Y, 150, 100, r + 2, 7);
            shapes.setColor(1f, 0.45f, 0.45f, 0.75f * morning * a);
            arc(MOON_X, MOON_Y, 15, -100, r, 3);
            GameApp.endShapeRendering();
        }
    }

    // pixel moon: made in code once (craters, seas, noise), then lit for the phase of the night
    private static final int MOON_PX = 112;               // texture size; drawn ~3x bigger, so chunky pixels
    private static float[] moonAlbedo;                    // brightness of the ground per pixel (-1 = not on the moon)
    private Pixmap moonPixmap;
    private Texture moonTexture;
    private Texture bloodTexture;                         // the same moon, full and blood red (the dawn)
    private float moonPhase = -1;

    private void updateMoonTexture(float fullness) {
        if (moonAlbedo == null) {
            moonAlbedo = makeMoonGround();
        }
        if (moonPixmap == null) {
            moonPixmap = new Pixmap(MOON_PX, MOON_PX, Pixmap.Format.RGBA8888);
        }
        if (moonTexture != null && Math.abs(fullness - moonPhase) < 0.004f) {
            return;
        }
        moonPhase = fullness;
        double theta = Math.PI * (0.08 + 0.92 * fullness); // 0 = new moon, PI = full; waxing = lit on the right
        double lx = Math.sin(theta), lz = -Math.cos(theta);
        for (int j = 0; j < MOON_PX; j++) {
            for (int i = 0; i < MOON_PX; i++) {
                float ground = moonAlbedo[j * MOON_PX + i];
                if (ground < 0) {
                    moonPixmap.drawPixel(i, j, 0);
                    continue;
                }
                double nx = (i + 0.5) / MOON_PX * 2 - 1, ny = 1 - (j + 0.5) / MOON_PX * 2;
                double d2 = nx * nx + ny * ny;
                double nz = Math.sqrt(Math.max(0, 1 - d2));
                double light = nx * lx + nz * lz;         // > 0: the sun shines here
                // seas: the darker patches of the ground, in a few flat steps (pixel art)
                float sea = Math.round(clamp01((0.80f - ground) * 2.6f) * 3) / 3f;
                float r, g, b;
                if (light > 0.05) {                       // the lit side: almost white, blue-grey seas
                    r = lerp(0.86f, 0.50f, sea);
                    g = lerp(0.92f, 0.60f, sea);
                    b = lerp(1.00f, 0.78f, sea);
                } else if (light > -0.02) {               // the edge between light and dark: a mid tone
                    r = 0.45f; g = 0.52f; b = 0.66f;
                } else {                                  // the dark side, still faintly visible
                    r = lerp(0.15f, 0.07f, sea);
                    g = lerp(0.17f, 0.08f, sea);
                    b = lerp(0.21f, 0.11f, sea);
                    if (d2 > 0.955) {                     // a faint cold rim around the dark side
                        r = 0.30f; g = 0.35f; b = 0.46f;
                    }
                }
                if (d2 > 0.978) {                         // a dark outline all around
                    r *= 0.45f; g *= 0.5f; b *= 0.6f;
                }
                moonPixmap.drawPixel(i, j, Color.rgba8888(posterize(r), posterize(g), posterize(b), 1f));
            }
        }
        if (moonTexture == null) {
            moonTexture = new Texture(moonPixmap);
            moonTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        } else {
            moonTexture.draw(moonPixmap, 0, 0);
        }
    }

    /** The blood moon: the whole disc dark red, its seas almost black, darker towards the edge. */
    private Texture makeBloodMoon() {
        if (moonAlbedo == null) {
            moonAlbedo = makeMoonGround();
        }
        Pixmap pix = new Pixmap(MOON_PX, MOON_PX, Pixmap.Format.RGBA8888);
        for (int j = 0; j < MOON_PX; j++) {
            for (int i = 0; i < MOON_PX; i++) {
                float ground = moonAlbedo[j * MOON_PX + i];
                if (ground < 0) {
                    pix.drawPixel(i, j, 0);
                    continue;
                }
                double nx = (i + 0.5) / MOON_PX * 2 - 1, ny = 1 - (j + 0.5) / MOON_PX * 2;
                double d2 = nx * nx + ny * ny;
                float edge = (float) (0.6 + 0.4 * Math.sqrt(Math.max(0, 1 - d2)));   // darker towards the edge
                float sea = Math.round(clamp01((0.80f - ground) * 2.6f) * 3) / 3f;
                float r = lerp(0.32f, 0.13f, sea) * edge;
                float g = lerp(0.09f, 0.04f, sea) * edge;
                float b = lerp(0.10f, 0.05f, sea) * edge;
                if (d2 > 0.978) {
                    r *= 0.5f; g *= 0.5f; b *= 0.5f;
                }
                pix.drawPixel(i, j, Color.rgba8888(posterize(r), posterize(g), posterize(b), 1f));
            }
        }
        Texture texture = new Texture(pix);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pix.dispose();
        return texture;
    }

    /** A few brightness steps only, so it looks like pixel art. */
    private static float posterize(float v) {
        return Math.round(clamp01(v) * 32) / 32f;
    }

    /** The moon's ground: bright highlands, dark seas, craters with light rims, a bit of noise. Same every time. */
    private static float[] makeMoonGround() {
        float[] ground = new float[MOON_PX * MOON_PX];
        java.util.Random rnd = new java.util.Random(7);
        int craters = 110;
        float[][] c = new float[craters][];
        for (int k = 0; k < craters; k++) {
            float cr = 0.025f + (float) Math.pow(rnd.nextFloat(), 3) * 0.13f;
            float cx, cy;
            do {
                cx = rnd.nextFloat() * 2 - 1;
                cy = rnd.nextFloat() * 2 - 1;
            } while (cx * cx + cy * cy > 0.9f);
            c[k] = new float[]{cx, cy, cr, 0.05f + rnd.nextFloat() * 0.09f};
        }
        float[][] seas = {{-0.25f, 0.30f, 0.38f}, {0.18f, 0.05f, 0.30f}, {-0.05f, -0.40f, 0.28f}, {0.42f, 0.42f, 0.22f}};
        for (int j = 0; j < MOON_PX; j++) {
            for (int i = 0; i < MOON_PX; i++) {
                float nx = (i + 0.5f) / MOON_PX * 2 - 1, ny = 1 - (j + 0.5f) / MOON_PX * 2;
                if (nx * nx + ny * ny > 1) {
                    ground[j * MOON_PX + i] = -1;
                    continue;
                }
                float v = 0.80f + 0.16f * noise(nx * 3, ny * 3) + 0.08f * noise(nx * 8, ny * 8);
                for (float[] s : seas) {
                    float d = dist(nx, ny, s[0], s[1]) / s[2];
                    if (d < 1) v -= 0.16f * (1 - d * d);
                }
                for (float[] k : c) {
                    float d = dist(nx, ny, k[0], k[1]) / k[2];
                    if (d < 0.85f) v -= k[3] * (1 - d * d / 0.72f);
                    else if (d < 1.15f) v += k[3] * 0.45f * (1 - Math.abs(d - 1) / 0.15f);
                }
                ground[j * MOON_PX + i] = Math.max(0.25f, Math.min(1.1f, v));
            }
        }
        return ground;
    }

    private static float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /** Smooth value noise between -1 and 1. */
    private static float noise(float x, float y) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
        float fx = x - x0, fy = y - y0;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        float a = hash(x0, y0), b = hash(x0 + 1, y0), c = hash(x0, y0 + 1), d = hash(x0 + 1, y0 + 1);
        return lerp(lerp(a, b, fx), lerp(c, d, fx), fy);
    }

    private static float hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xffff) / 32767.5f - 1;
    }

    // ---------- the dashed ring of hours ----------
    private void drawRing(float a, float morning) {
        float hour = clockHour();
        float grow = smooth((t - 0.3f) / 0.8f);             // the ring draws itself in at the start
        ShapeRenderer shapes = GameApp.getShapeRenderer();
        float span = (RING_START - RING_END) / DASHES;

        GameApp.startShapeRenderingFilled();
        for (int i = 0; i < DASHES; i++) {
            if (i / (float) DASHES > grow) break;
            float a0 = RING_START - i * span, a1 = a0 - span * 0.74f;
            float dashHour = START_HOUR + (100 - (a0 + a1) / 2) / DEG_PER_HOUR;
            boolean passed = dashHour <= hour || morning > 0;
            float alpha = (passed ? 0.95f : 0.38f) * a;
            shapes.setColor(RED_LIGHT.r, RED_LIGHT.g, RED_LIGHT.b, alpha);
            arc(MOON_X, MOON_Y, a0, a1, RING_R, passed ? RING_W : RING_W * 0.8f);
        }
        // dawn: the last hours (03:30 -> 06:00) burn as one bright line, growing with every strike
        if (morning > 0) {
            float from = 100 - (3.5f + 24 - START_HOUR) * DEG_PER_HOUR;       // 03:30
            float to = 100 - (6 + 24 - START_HOUR) * DEG_PER_HOUR;            // 06:00
            float end = lerp(from, to, morning);
            float flicker = 0.85f + 0.15f * (float) Math.sin(t * 23);
            shapes.setColor(DAWN_RED.r, DAWN_RED.g, DAWN_RED.b, 0.10f * a * flicker);
            arc(MOON_X, MOON_Y, from, end, RING_R, 34);
            shapes.setColor(DAWN_RED.r, DAWN_RED.g, DAWN_RED.b, 0.30f * a * flicker);
            arc(MOON_X, MOON_Y, from, end, RING_R, 18);
            shapes.setColor(DAWN_RED.r, DAWN_RED.g, DAWN_RED.b, a);
            arc(MOON_X, MOON_Y, from, end, RING_R, 9);
            shapes.setColor(1f, 0.82f, 0.82f, a);
            arc(MOON_X, MOON_Y, from, end, RING_R, 3);
            // sparks flying off the burning part
            java.util.Random sparks = new java.util.Random(31);
            for (int i = 0; i < 26; i++) {
                float k = sparks.nextFloat();
                float life = ((t * (0.6f + sparks.nextFloat())) + sparks.nextFloat()) % 1f;
                double ang = Math.toRadians(lerp(from, end, k));
                float dist = RING_R + (sparks.nextFloat() - 0.3f) * 30 + life * 26;
                float sx = MOON_X + (float) Math.cos(ang) * dist, sy = MOON_Y + (float) Math.sin(ang) * dist + life * 14;
                float size = sparks.nextFloat() < 0.3f ? 4 : 2;
                GameApp.drawRect(sx, sy, size, size, new Color(1f, 0.55f, 0.5f, (1 - life) * 0.9f * a * morning));
            }
        }
        // the hand: a short bright mark on the ring where the time is now
        float nowAng = 100 - (hour - START_HOUR) * DEG_PER_HOUR;
        if (nowAng > RING_END) {
            double ang = Math.toRadians(nowAng);
            shapes.setColor(TEXT.r, TEXT.g, TEXT.b, a * (1 - morning));
            shapes.rectLine(MOON_X + (float) Math.cos(ang) * (RING_R - 14), MOON_Y + (float) Math.sin(ang) * (RING_R - 14),
                    MOON_X + (float) Math.cos(ang) * (RING_R + 14), MOON_Y + (float) Math.sin(ang) * (RING_R + 14), 4);
        }
        GameApp.endShapeRendering();

        // the hours 23 .. 06 just outside the ring
        GameApp.startSpriteRendering();
        for (int i = 0; i < 8; i++) {
            double ang = Math.toRadians(100 - i * DEG_PER_HOUR);
            float lx = MOON_X + (float) Math.cos(ang) * (RING_R + 34);
            float ly = MOON_Y + (float) Math.sin(ang) * (RING_R + 34);
            boolean now = ((int) Math.floor(hour) % 24) == (23 + i) % 24;
            boolean burning = morning > 0 && i >= 6;   // 05 and 06 turn red at dawn
            Color c = burning ? DAWN_TEXT : now ? TEXT : DIM;
            drawCentered(SMALL_FONT, String.format("%02d", (23 + i) % 24), lx, ly, c, a * (now || burning ? 1f : 0.8f));
        }
        GameApp.endSpriteRendering();

        // every strike: a red shock ring spreading out from the moon
        float wave = strikeWave();
        if (wave >= 0) {
            float k = smooth(wave);
            GameApp.setLineWidth(3f);
            GameApp.startShapeRenderingOutlined();
            GameApp.drawCircle(MOON_X, MOON_Y, MOON_R + k * 420, new Color(DAWN_RED.r, DAWN_RED.g, DAWN_RED.b, 0.55f * (1 - wave) * a));
            GameApp.drawCircle(MOON_X, MOON_Y, MOON_R + k * 260, new Color(DAWN_RED.r, DAWN_RED.g, DAWN_RED.b, 0.30f * (1 - wave) * a));
            GameApp.endShapeRendering();
            GameApp.setLineWidth(1f);
        }
    }

    /** A thick piece of a circle around (cx, cy) between two angles (degrees). */
    private void arc(float cx, float cy, float fromDeg, float toDeg, float r, float width) {
        ShapeRenderer shapes = GameApp.getShapeRenderer();
        int steps = Math.max(2, (int) (Math.abs(toDeg - fromDeg) / 3));
        for (int s = 0; s < steps; s++) {
            double p = Math.toRadians(fromDeg + (toDeg - fromDeg) * s / steps);
            double q = Math.toRadians(fromDeg + (toDeg - fromDeg) * (s + 1) / steps);
            shapes.rectLine(cx + (float) Math.cos(p) * r, cy + (float) Math.sin(p) * r,
                    cx + (float) Math.cos(q) * r, cy + (float) Math.sin(q) * r, width);
        }
    }

    // ---------- top left: NIGHT / MORNING and the time ----------
    private void drawTime(float a, float morning) {
        float hour = clockHour();
        int hh = ((int) Math.floor(hour)) % 24;
        int mm = (int) Math.floor((hour % 1) * 60);
        String time = String.format("%02d:%02d", hh, mm);
        float x = 100, y = 500;
        GameApp.startSpriteRendering();
        boolean isMorning = morning > 0.3f;
        if (isMorning) {
            drawLeft(MID_FONT, "DAY", x + 4, 594, DAWN_TEXT, a);
            drawLeft(MID_FONT, "MORNING", x + 4 + 54, 594, DIM, a);
        } else {
            drawLeft(MID_FONT, "NIGHT", x + 4, 594, DIM, a);
        }
        // a soft glow behind the digits
        for (int i = 0; i < 12; i++) {
            double ang = Math.PI * 2 * i / 12;
            drawLeft(HUGE_FONT, time, x + (float) Math.cos(ang) * 9, y + (float) Math.sin(ang) * 9, TEXT, 0.025f * a);
            drawLeft(HUGE_FONT, time, x + (float) Math.cos(ang) * 4, y + (float) Math.sin(ang) * 4, TEXT, 0.06f * a);
        }
        // dawn: a red edge on the digits, shaking a little with every strike
        if (morning > 0) {
            float shake = 3 + 5 * strikePulse();
            drawLeft(HUGE_FONT, time, x + shake, y, DAWN_RED, 0.55f * morning * a);
            drawLeft(HUGE_FONT, time, x - shake * 0.5f, y, new Color(0.5f, 0.8f, 1f, 1f), 0.12f * morning * a);
        }
        drawLeft(HUGE_FONT, time, x, y, TEXT, a);
        GameApp.endSpriteRendering();
    }

    // ---------- on the band: DAY, the red number, the date, the countdown ----------
    private void drawDay(float a) {
        float in = smooth((t - 0.2f) / 0.7f);
        float off = -(1 - in) * (1280 + 200);
        float roll = smooth((t - T_DATE) / 0.6f);   // 0 = old day, 1 = new day

        GameApp.startSpriteRendering();
        drawLeft(MID_FONT, "DAY", 98 + off, 153, DIM, a);
        float numX = 166 + off, numY = 118;
        if (roll < 1) {
            drawLeft(DAY_FONT, String.valueOf(fromDay), numX, numY + roll * 60, RED_LIGHT, a * (1 - roll));
        }
        if (roll > 0) {
            drawLeft(DAY_FONT, String.valueOf(toDay), numX, numY - (1 - roll) * 60, RED_LIGHT, a * roll);
        }
        int shownDay = roll < 0.5f ? fromDay : toDay;
        float info = roll < 0.5f ? 1 - roll * 2 : (roll - 0.5f) * 2;   // blinks out and in with the roll
        drawLeft(BIG_FONT, Hud.date(shownDay).toUpperCase(), 344 + off, 160, TEXT, a * info);
        drawLeft(SMALL_FONT, countdown(shownDay), 346 + off, 108, RED_LIGHT, a * info);
        GameApp.endSpriteRendering();
    }

    /** Thin dark lines over everything, like an old screen. */
    private void drawScanLines(float w, float h, float a) {
        GameApp.startShapeRenderingFilled();
        Color line = new Color(0, 0, 0, 0.22f * a);
        for (float y = 0; y < h; y += 3) {
            GameApp.drawRect(0, y, w, 1, line);
        }
        GameApp.endShapeRendering();
    }

    /** "5 DAYS UNTIL HIS 18TH BIRTHDAY" ... "TODAY IS THE DAY." */
    private static String countdown(int d) {
        int left = 7 - d;
        if (left <= 0) return "TODAY IS THE DAY.";
        return left + (left == 1 ? " DAY" : " DAYS") + " UNTIL HIS 18TH BIRTHDAY";
    }

    private static float lerp(float from, float to, float k) {
        return from + (to - from) * k;
    }

    /** Text with its left edge on x and its middle on y. */
    private void drawLeft(String fontName, String text, float x, float y, Color c, float a) {
        if (a <= 0.005f) return;
        BitmapFont font = GameApp.getFont(fontName);
        font.setColor(c.r, c.g, c.b, a);
        layout.setText(font, text);
        font.draw(GameApp.getSpriteBatch(), layout, Math.round(x), Math.round(y + layout.height / 2));
        font.setColor(Color.WHITE);
    }

    /** Text centred on x and y. */
    private void drawCentered(String fontName, String text, float x, float y, Color c, float a) {
        if (a <= 0.005f) return;
        BitmapFont font = GameApp.getFont(fontName);
        font.setColor(c.r, c.g, c.b, a);
        layout.setText(font, text);
        font.draw(GameApp.getSpriteBatch(), layout, Math.round(x - layout.width / 2), Math.round(y + layout.height / 2));
        font.setColor(Color.WHITE);
    }
}