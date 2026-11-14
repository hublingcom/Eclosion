package nl.saxion.game.eclosion;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * START-OF-FLOOR CUTSCENE - "the night at 13", Floor 1 fragment.
 * (Story bible 2.4: "Floor 1: Black screen. Rain, and shouting behind a closed door.")
 *
 * Ageha's point of view: a dark room, only a thin line of light under a closed door.
 * Muffled voices behind it, shadows passing the light, then a door slams and the light goes out.
 * Nothing violent is shown - only sound and light.
 *
 *   NightCutsceneScreen.prepare("Floor1FlowerScreen");   // where to go afterwards
 *   GameApp.switchScreen("NightCutsceneScreen");
 *
 * ENTER skips.
 * Sounds (src/main/resources/audio): rain.ogg, voice_low.wav, voice_high.wav, door_slam.wav
 */
public class NightCutsceneScreen extends ScalableGameScreen {

    static String nextScreen = "Floor1FlowerScreen";

    public static void prepare(String next) {
        nextScreen = next;
    }

    static final float W = 1280, H = 720;
    static final float LENGTH = 15f;

    // timeline (seconds)
    static final float T_LIGHT_ON = 2.0f;
    static final float T_VOICE_1 = 3.6f;    // low voice
    static final float T_VOICE_2 = 6.6f;    // higher voice answers
    static final float T_VOICE_3 = 8.4f;    // low voice again, louder
    static final float T_SLAM = 11.2f;      // door slams, light goes out
    static final float T_END_FADE = 13.5f;

    float timer;
    boolean v1, v2, v3, slam;
    float flinch;   // small screen shake after the slam
    long idV1 = -1, idV2 = -1, idV3 = -1;   // so the slam can cut the voices off

    public NightCutsceneScreen() {
        super(1280, 720);
    }

    @Override
    public void show() {
        GameApp.addFont("nc_small", "fonts/basic.ttf", 20);
        GameApp.addMusic("rain", "audio/rain.ogg");
        GameApp.addSound("voice_low", "audio/voice_low.wav");
        GameApp.addSound("voice_high", "audio/voice_high.wav");
        GameApp.addSound("door_slam", "audio/door_slam.wav");
        GameApp.playMusic("rain", true, 0f);
        GameApp.enableTransparency();
        timer = 0;
        v1 = v2 = v3 = slam = false;
        flinch = 0;
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        timer += delta;

        // rain fades in, then out at the very end
        float rainVol = Math.min(1f, timer / 2.5f) * 0.45f;
        if (timer > T_END_FADE) rainVol *= Math.max(0f, 1f - (timer - T_END_FADE) / (LENGTH - T_END_FADE));
        GameApp.getMusic("rain").setVolume(rainVol);

        if (!v1 && timer > T_VOICE_1) { idV1 = GameApp.playSound("voice_low", 0.9f); v1 = true; }
        if (!v2 && timer > T_VOICE_2) { idV2 = GameApp.playSound("voice_high", 0.8f); v2 = true; }
        if (!v3 && timer > T_VOICE_3) { idV3 = GameApp.getSound("voice_low").play(1.0f, 0.93f, 0f); v3 = true; }
        if (!slam && timer > T_SLAM) {
            // the slam cuts everything off -> sudden silence, only the rain
            GameApp.getSound("voice_low").stop();
            GameApp.getSound("voice_high").stop();
            GameApp.playSound("door_slam", 1.0f);
            slam = true;
            flinch = 0.35f;
        }
        if (flinch > 0) flinch -= delta;

        if (timer > LENGTH || (timer > 1f && GameApp.isKeyJustPressed(Input.Keys.ENTER))) {
            GameApp.switchScreen(nextScreen);
            return;
        }
        draw();
    }

    /** how bright the line under the door is (0..1) */
    float lightLevel() {
        if (timer < T_LIGHT_ON) return 0;
        float on = Math.min(1f, (timer - T_LIGHT_ON) / 1.5f);
        if (slam) {
            float t = timer - T_SLAM;
            if (t < 0.15f) return 1f;                       // flash of the slam
            if (t < 0.6f) return (float) Math.abs(Math.sin(t * 40)) * 0.6f;   // flicker
            return 0;                                       // out
        }
        // the light trembles a little, like a ceiling lamp swinging
        return on * (0.85f + 0.1f * (float) Math.sin(timer * 3.1f) + 0.05f * (float) Math.sin(timer * 11f));
    }

    void draw() {
        GameApp.clearScreen(0, 0, 0);
        float shake = flinch > 0 ? GameApp.random(-1, 1) * 6 * (flinch / 0.35f) : 0;
        float cx = W / 2 + shake, y = 230 + shake;
        float doorW = 190;
        float L = lightLevel();

        GameApp.enableTransparency();   // SpriteBatch.end() turns blending off, so turn it on again
        GameApp.startShapeRenderingFilled();
        if (L > 0) {
            // faint door frame lit from below
            Color frame = new Color(0.07f, 0.065f, 0.06f, L);
            GameApp.drawRect(cx - doorW / 2 - 10, y, 8, 330, frame);
            GameApp.drawRect(cx + doorW / 2 + 2, y, 8, 330, frame);
            // glow on the floor in front of the door
            for (int i = 0; i < 6; i++) {
                float w = doorW + 40 + i * 50, a = 0.05f * L * (6 - i) / 6f;
                GameApp.drawRect(cx - w / 2, y - 10 - i * 18, w, 10 + i * 18, new Color(1f, 0.88f, 0.6f, a));
            }
            // the bright line under the door
            GameApp.drawRect(cx - doorW / 2, y, doorW, 5, new Color(1f, 0.9f, 0.65f, L));

            // shadows of feet passing behind the door while they argue
            drawShadow(cx, y, doorW, T_VOICE_1 - 0.3f, 2.6f, true);
            drawShadow(cx, y, doorW, T_VOICE_2 + 0.2f, 1.8f, false);
            drawShadow(cx, y, doorW, T_VOICE_3 - 0.2f, 2.8f, true);
        }
        // end: fade to black
        if (timer > T_END_FADE) {
            GameApp.drawRect(0, 0, W, H, new Color(0, 0, 0, Math.min(1f, (timer - T_END_FADE) / 1.2f)));
        }
        GameApp.endShapeRendering();

        if (timer > 1f) {
            GameApp.startSpriteRendering();
            GameApp.drawText("nc_small", "ENTER to skip", W - 170, 28, new Color(0.3f, 0.3f, 0.3f, 0.6f));
            GameApp.endSpriteRendering();
        }
    }

    /** two dark blobs (feet) moving across the light line */
    void drawShadow(float cx, float y, float doorW, float start, float dur, boolean leftToRight) {
        float t = (timer - start) / dur;
        if (t < 0 || t > 1 || slam) return;
        float p = leftToRight ? t : 1 - t;
        float x = cx - doorW / 2 - 40 + p * (doorW + 80);
        Color dark = new Color(0, 0, 0, 0.9f);
        GameApp.drawRect(x - 26, y, 22, 6, dark);
        GameApp.drawRect(x + 6, y, 22, 6, dark);
    }

    @Override
    public void hide() {
        GameApp.stopMusic("rain");
        GameApp.disposeMusic("rain");
        GameApp.disposeSound("voice_low");
        GameApp.disposeSound("voice_high");
        GameApp.disposeSound("door_slam");
        GameApp.disposeFont("nc_small");
    }
}