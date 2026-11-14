package nl.saxion.game.eclosion;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * NIGHT -> MORNING. Reusable for every night of the week.
 *
 *   DayTransitionScreen.prepare(3, "RoomScreen");     // the day that starts, where to go after
 *   GameApp.switchScreen("DayTransitionScreen");
 *
 * Ageha's room: he goes to bed, the desk lamp clicks off, moonlight slides over the floor while the
 * clock ticks, then dawn light fills the room, birds, and a card: "Day 3 / 15 June / 5 days left".
 * ENTER skips.
 *
 * The game runs 13-19 June; Day 7 (19 June) is his 18th birthday (story bible 2.1).
 *
 * Images: images/room.png (1600x902, the one with the black margin), images/ageha_left_idle.png
 * Sounds: audio/tick.wav, audio/lamp_click.wav, audio/birds.wav
 */
public class DayTransitionScreen extends ScalableGameScreen {

    static int day = 2;
    static String nextScreen = "RoomScreen";

    public static void prepare(int newDay, String next) {
        day = newDay;
        nextScreen = next;
    }

    static final float W = 1280, H = 720;
    // room.png is drawn like in RoomScreen: scaled to the screen height and centered
    static final float IMG_W = 1600, IMG_H = 902;
    final float scale = H / IMG_H;
    final float offX = (W - IMG_W * scale) / 2;

    // timeline (seconds)
    static final float T_TO_BED = 1.0f, T_LAMP_OFF = 2.2f, T_DAWN = 7.5f, T_CARD = 10.5f, T_END = 14.5f;

    float t;
    boolean lampOff, birds, card;
    int lastTick;

    public DayTransitionScreen() {
        super(1280, 720);
    }

    /** image pixel (x from left, y from top) -> world x / y */
    float wx(float ix) { return offX + ix * scale; }
    float wy(float iy) { return H - iy * scale; }

    @Override
    public void show() {
        GameApp.addTexture("dt_room", "images/room.png");
        GameApp.addTexture("dt_ageha", "images/ageha_left_idle.png");
        GameApp.addFont("dt_big", "fonts/basic.ttf", 72);
        GameApp.addFont("dt_mid", "fonts/basic.ttf", 30);
        GameApp.addSound("dt_tick", "audio/tick.wav");
        GameApp.addSound("dt_click", "audio/lamp_click.wav");
        GameApp.addSound("dt_birds", "audio/birds.wav");
        t = 0;
        lampOff = birds = card = false;
        lastTick = 0;
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        t += delta;

        if (!lampOff && t > T_LAMP_OFF) { GameApp.playSound("dt_click", 0.9f); lampOff = true; }
        // clock ticks once per second during the night
        int sec = (int) t;
        if (lampOff && t < T_DAWN + 1.5f && sec != lastTick) {
            lastTick = sec;
            GameApp.getSound("dt_tick").play(0.45f, sec % 2 == 0 ? 1f : 0.9f, 0f);
        }
        if (!birds && t > T_DAWN + 0.8f) { GameApp.playSound("dt_birds", 0.6f); birds = true; }

        if (t > T_END || (t > 1f && GameApp.isKeyJustPressed(Input.Keys.ENTER))) {
            GameApp.switchScreen(nextScreen);
            return;
        }
        draw();
    }

    float clamp01(float v) { return Math.max(0, Math.min(1, v)); }

    void draw() {
        GameApp.clearScreen(0, 0, 0);

        GameApp.startSpriteRendering();
        GameApp.drawTexture("dt_room", offX, 0, IMG_W * scale, H);
        // Ageha stands next to his bed, then "gets in" (fades out)
        float agehaAlpha = 1 - clamp01((t - T_TO_BED) / 0.8f);
        if (agehaAlpha > 0) {
            GameApp.getSpriteBatch().setColor(1, 1, 1, agehaAlpha);
            float ah = 200 * scale, aw = ah * 194f / 320f;
            GameApp.drawTexture("dt_ageha", wx(560), wy(520), aw, ah);
            GameApp.getSpriteBatch().setColor(Color.WHITE);
        }
        GameApp.endSpriteRendering();

        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();

        // ---------- light level ----------
        float night = clamp01((t - T_LAMP_OFF) / 0.25f);          // lamp off: quick darkening
        float dawn = clamp01((t - T_DAWN) / 3.0f);                  // slow sunrise
        float dark = night * (1 - dawn);

        // night: deep blue darkness
        GameApp.drawRect(0, 0, W, H, new Color(0.02f, 0.03f, 0.08f, 0.78f * dark));

        // moonlight through the window, slowly sliding across the floor
        if (dark > 0.05f) {
            float slideT = clamp01((t - T_LAMP_OFF) / (T_DAWN - T_LAMP_OFF + 2));
            float beamX = wx(830) - 120 + slideT * 260;
            for (int i = 0; i < 32; i++) {            // many thin slices = smooth soft beam
                float y = wy(270) - i * 10;
                float w = 120 + i * 5.5f;
                GameApp.drawRect(beamX - w / 2 + i * 3.5f, y - 10, w, 10, new Color(0.7f, 0.78f, 1f, 0.06f * dark * (1 - i / 34f)));
            }
            GameApp.drawRect(wx(785), wy(265), (975 - 785) * scale, (265 - 140) * scale, new Color(0.6f, 0.7f, 1f, 0.12f * dark));
        }
        // desk lamp glow before it goes off
        if (!lampOff) {
            GameApp.drawCircle(wx(975), wy(255), 70, new Color(1f, 0.85f, 0.55f, 0.08f));
        }
        // dawn: warm light coming from the window, fading in and then settling
        if (dawn > 0) {
            float warm = dawn * (1 - 0.6f * clamp01((t - T_DAWN - 3) / 2));
            GameApp.drawRect(0, 0, W, H, new Color(1f, 0.75f, 0.45f, 0.12f * warm));
            GameApp.drawRect(wx(785), wy(265), (975 - 785) * scale, (265 - 140) * scale, new Color(1f, 0.93f, 0.75f, 0.35f * dawn));
            for (int i = 0; i < 32; i++) {
                float y = wy(270) - i * 10, w = 150 + i * 7.5f;
                GameApp.drawRect(wx(880) - w / 2 - i * 3, y - 10, w, 10, new Color(1f, 0.9f, 0.7f, 0.07f * dawn * (1 - i / 34f)));
            }
        }

        // fade in at the start
        GameApp.drawRect(0, 0, W, H, new Color(0, 0, 0, 1 - clamp01(t / 0.8f)));

        // day card
        float cardA = clamp01((t - T_CARD) / 0.8f) * (1 - clamp01((t - (T_END - 0.7f)) / 0.7f));
        if (cardA > 0) GameApp.drawRect(0, 0, W, H, new Color(0, 0, 0, 0.65f * cardA));
        GameApp.endShapeRendering();

        if (cardA > 0) {
            GameApp.startSpriteRendering();
            GameApp.drawTextCentered("dt_big", "Day " + day, W / 2, H / 2 + 40, new Color(0.95f, 0.95f, 0.95f, cardA));
            GameApp.drawTextCentered("dt_mid", (12 + day) + " June", W / 2, H / 2 - 30, new Color(0.65f, 0.65f, 0.68f, cardA));
            int left = 8 - day;   // counting the birthday itself
            String countdown = left <= 1 ? "Today is the day." : left + " days left";
            GameApp.drawTextCentered("dt_mid", countdown, W / 2, H / 2 - 75, new Color(0.65f, 0.65f, 0.68f, cardA));
            GameApp.endSpriteRendering();
        }
    }

    @Override
    public void hide() {
        GameApp.disposeTexture("dt_room");
        GameApp.disposeTexture("dt_ageha");
        GameApp.disposeFont("dt_big");
        GameApp.disposeFont("dt_mid");
        GameApp.disposeSound("dt_tick");
        GameApp.disposeSound("dt_click");
        GameApp.disposeSound("dt_birds");
    }
}
