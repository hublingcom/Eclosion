package nl.saxion.game.eclosion;

import com.badlogic.gdx.graphics.Color;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * DOOR TRANSITION - a short "open the door and walk through" between two places.
 *
 *   DoorTransitionScreen.go("CorridorScreen");                 // just go
 *   DoorTransitionScreen.go("CorridorScreen", "Corridor");     // with a small place name
 *
 * Dark screen, light leaking under a door, the door swings open (creak), light floods out,
 * two footsteps, then the next screen. About 2 seconds.
 *
 * Sounds: audio/door_open.wav, audio/step_1.wav, audio/step_2.wav
 */
public class DoorTransitionScreen extends ScalableGameScreen {

    static String nextScreen = "RoomScreen";
    static String placeName = "";

    public static void go(String next) {
        go(next, "");
    }

    public static void go(String next, String name) {
        nextScreen = next;
        placeName = name;
        GameApp.switchScreen("DoorTransitionScreen");
    }

    static final float W = 1280, H = 720;
    static final float T_OPEN = 0.35f, T_STEP1 = 1.05f, T_STEP2 = 1.45f, T_END = 2.1f;

    float t;
    boolean opened, step1, step2;

    public DoorTransitionScreen() {
        super(1280, 720);
    }

    @Override
    public void show() {
        GameApp.addSound("dr_open", "audio/door_open.wav");
        GameApp.addSound("dr_step1", "audio/step_1.wav");
        GameApp.addSound("dr_step2", "audio/step_2.wav");
        GameApp.addFont("dr_name", "fonts/basic.ttf", 30);
        t = 0;
        opened = step1 = step2 = false;
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        t += delta;
        if (!opened && t > T_OPEN) { GameApp.playSound("dr_open", 0.85f); opened = true; }
        if (!step1 && t > T_STEP1) { GameApp.playSound("dr_step1", 0.8f); step1 = true; }
        if (!step2 && t > T_STEP2) { GameApp.playSound("dr_step2", 0.8f); step2 = true; }
        if (t > T_END) {
            GameApp.switchScreen(nextScreen);
            return;
        }
        draw();
    }

    float clamp01(float v) { return Math.max(0, Math.min(1, v)); }

    void draw() {
        GameApp.clearScreen(0, 0, 0);
        float cx = W / 2, doorW = 200, doorH = 380, doorY = 150;
        float open = clamp01((t - T_OPEN) / 0.7f);
        open = open * open * (3 - 2 * open);                      // smooth
        float flood = clamp01((t - 1.1f) / 0.8f);                 // light fills the screen
        Color warm = new Color(1f, 0.95f, 0.85f, 1);

        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        // light from the other side (visible through the opening)
        GameApp.drawRect(cx - doorW / 2, doorY, doorW * open, doorH, new Color(warm.r, warm.g, warm.b, 0.9f * open));
        // glow on the floor in front of the door
        for (int i = 0; i < 6; i++) {
            float w = doorW * (0.4f + open) + i * 60;
            GameApp.drawRect(cx - w / 2, doorY - 12 - i * 22, w, 12 + i * 22, new Color(1f, 0.92f, 0.75f, 0.04f * (6 - i) * (0.3f + open)));
        }
        // the door leaf: hinge on the right, it gets narrower as it swings toward us
        float leafW = doorW * (1 - open * 0.85f);
        GameApp.drawRect(cx + doorW / 2 - leafW, doorY, leafW, doorH, new Color(0.08f, 0.07f, 0.07f, 1));
        GameApp.drawRect(cx + doorW / 2 - leafW + leafW * 0.1f, doorY + doorH * 0.55f, leafW * 0.8f, doorH * 0.35f, new Color(0.11f, 0.1f, 0.1f, 1));
        GameApp.drawRect(cx + doorW / 2 - leafW + leafW * 0.1f, doorY + doorH * 0.08f, leafW * 0.8f, doorH * 0.4f, new Color(0.11f, 0.1f, 0.1f, 1));
        // thin line of light under the closed door
        GameApp.drawRect(cx - doorW / 2, doorY, doorW, 3, new Color(1f, 0.9f, 0.7f, 0.9f));
        // door frame
        Color frame = new Color(0.16f, 0.15f, 0.14f, 1);
        GameApp.drawRect(cx - doorW / 2 - 12, doorY, 12, doorH + 12, frame);
        GameApp.drawRect(cx + doorW / 2, doorY, 12, doorH + 12, frame);
        GameApp.drawRect(cx - doorW / 2 - 12, doorY + doorH, doorW + 24, 12, frame);
        // light floods the whole screen, then quick fade to black before the next place
        if (flood > 0) GameApp.drawRect(0, 0, W, H, new Color(warm.r, warm.g, warm.b, flood));
        float out = clamp01((t - (T_END - 0.35f)) / 0.35f);
        if (out > 0) GameApp.drawRect(0, 0, W, H, new Color(0, 0, 0, out));
        // fade in from black at the very start
        GameApp.drawRect(0, 0, W, H, new Color(0, 0, 0, 1 - clamp01(t / 0.25f)));
        GameApp.endShapeRendering();

        if (!placeName.isEmpty() && flood > 0.3f) {
            float a = clamp01((flood - 0.3f) / 0.4f) * (1 - out);
            GameApp.startSpriteRendering();
            GameApp.drawTextCentered("dr_name", placeName, cx, H / 2, new Color(0.25f, 0.22f, 0.2f, a));
            GameApp.endSpriteRendering();
        }
    }

    @Override
    public void hide() {
        GameApp.disposeSound("dr_open");
        GameApp.disposeSound("dr_step1");
        GameApp.disposeSound("dr_step2");
        GameApp.disposeFont("dr_name");
    }
}
