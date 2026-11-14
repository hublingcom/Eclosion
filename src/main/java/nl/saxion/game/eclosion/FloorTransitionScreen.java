package nl.saxion.game.eclosion;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * STAIRS SCENE (side view) - Ageha climbs the mansion stairs to the next floor.
 * Reusable for every floor: call prepare(...) before switching to this screen.
 *
 *   FloorTransitionScreen.prepare("Day 2", "Floor 1", "Loneliness", "Floor1FlowerScreen");
 *   GameApp.switchScreen("FloorTransitionScreen");
 *
 * Controls: hold D / RIGHT (or W / UP) to walk up. ENTER skips the title card at the end.
 *
 * Flow: fade in -> Ageha walks onto the stairs and climbs (footsteps, creaks)
 *       -> reaches the door on the landing -> door opens, light floods in
 *       -> black title card "Day 2 / Floor 1 / Loneliness" -> next screen.
 *
 * Images (src/main/resources/images): ageha_right_idle.png, ageha_right_a.png, ageha_right_b.png
 * Sounds (src/main/resources/audio):  step_1.wav, step_2.wav, creak.wav, door_open.wav
 */
public class FloorTransitionScreen extends ScalableGameScreen {

    // ---------- what this transition shows (set with prepare) ----------
    static String dayText = "Day 2";
    static String floorText = "Floor 1";
    static String themeText = "Loneliness";
    static String nextScreen = "Floor1FlowerScreen";

    public static void prepare(String day, String floor, String theme, String next) {
        dayText = day;
        floorText = floor;
        themeText = theme;
        nextScreen = next;
    }

    // ---------- layout (side view, stairs go up to the right) ----------
    static final float W = 1280, H = 720;
    static final int STEPS = 14;
    static final float STEP_W = 60;          // horizontal size of one step
    static final float STEP_H = 26;          // height of one step
    static final float GROUND_Y = 90;        // floor height at the bottom
    static final float STAIRS_X = 160;       // where the first step starts
    static final float LANDING_X = STAIRS_X + STEPS * STEP_W;
    static final float TOP_Y = GROUND_Y + STEPS * STEP_H;

    static final float AGEHA_H = 180;
    static final float AGEHA_W = AGEHA_H * 194f / 320f;
    static final float WALK_SPEED = 3f;      // steps per second (slow: he doesn't want to go)
    static final float START_POS = -1.8f;    // a bit before the stairs
    static final float END_POS = STEPS + 1.6f; // in front of the door on the landing

    // ---------- state ----------
    enum State { FADE_IN, CLIMBING, DOOR, TITLE }
    State state;
    float timer;
    float pos;               // position along the stairs in "steps" (negative = still on the ground floor)
    int lastStepIndex;
    boolean moving;
    float idleTime;
    float totalTime;

    public FloorTransitionScreen() {
        super(1280, 720);
    }

    @Override
    public void show() {
        GameApp.addFont("tr_big", "fonts/basic.ttf", 60);
        GameApp.addFont("tr_small", "fonts/basic.ttf", 28);
        for (String f : new String[]{"idle", "a", "b"}) {
            GameApp.addTexture("ageha_right_" + f, "images/ageha_right_" + f + ".png");
        }
        GameApp.addSound("step_1", "audio/step_1.wav");
        GameApp.addSound("step_2", "audio/step_2.wav");
        GameApp.addSound("creak", "audio/creak.wav");
        GameApp.addSound("door_open", "audio/door_open.wav");
        GameApp.enableTransparency();

        state = State.FADE_IN;
        timer = 0;
        pos = START_POS;
        lastStepIndex = (int) Math.floor(pos);
        idleTime = 0;
        totalTime = 0;
    }

    // ======================= UPDATE =======================

    @Override
    public void render(float delta) {
        super.render(delta);
        totalTime += delta;
        timer += delta;

        switch (state) {
            case FADE_IN:
                if (timer > 1.0f) { state = State.CLIMBING; timer = 0; }
                break;
            case CLIMBING:
                updateClimbing(delta);
                break;
            case DOOR:
                if (timer > 2.0f) { state = State.TITLE; timer = 0; }
                break;
            case TITLE:
                if (timer > 3.2f || (timer > 0.5f && GameApp.isKeyJustPressed(Input.Keys.ENTER))) {
                    GameApp.switchScreen(nextScreen);
                    return;
                }
                break;
        }
        draw();
    }

    void updateClimbing(float delta) {
        moving = GameApp.isKeyPressed(Input.Keys.RIGHT) || GameApp.isKeyPressed(Input.Keys.D)
                || GameApp.isKeyPressed(Input.Keys.UP) || GameApp.isKeyPressed(Input.Keys.W);
        if (moving) {
            pos += WALK_SPEED * delta;
            idleTime = 0;
        } else {
            idleTime += delta;
        }

        // one footstep sound per step
        int idx = (int) Math.floor(pos);
        if (idx != lastStepIndex) {
            lastStepIndex = idx;
            String s = (idx % 2 == 0) ? "step_1" : "step_2";
            GameApp.getSound(s).play(1.0f, GameApp.random(0.92f, 1.08f), 0f);
            boolean onStairs = idx >= 0 && idx < STEPS;
            if (onStairs && GameApp.random(0, 1) < 0.45f) {
                GameApp.getSound("creak").play(0.7f, GameApp.random(0.85f, 1.15f), 0f);
            }
        }

        if (pos >= END_POS) {
            pos = END_POS;
            GameApp.playSound("door_open", 1.0f);
            state = State.DOOR;
            timer = 0;
        }
    }

    // ======================= POSITION HELPERS =======================

    /** x of Ageha's feet (center) */
    float feetX() {
        return STAIRS_X + pos * STEP_W + STEP_W / 2;
    }

    /** y of Ageha's feet: flat on the ground, step by step on the stairs, flat on the landing */
    float feetY() {
        if (pos < 0) return GROUND_Y;
        if (pos >= STEPS) return TOP_Y;
        int base = (int) Math.floor(pos);
        float frac = pos - base;
        float lift = Math.min(1f, frac / 0.45f);       // the foot goes up in the first part of the step
        lift = lift * lift * (3 - 2 * lift);           // smooth
        return GROUND_Y + STEP_H * base + STEP_H * lift;
    }

    // ======================= DRAW =======================

    void draw() {
        GameApp.clearScreen(14, 13, 15);

        GameApp.enableTransparency();   // SpriteBatch.end() turns blending off, so turn it on again
        GameApp.startShapeRenderingFilled();
        drawRoom();
        drawStairs();
        GameApp.endShapeRendering();

        GameApp.setLineWidth(4);
        GameApp.enableTransparency();   // SpriteBatch.end() turns blending off, so turn it on again
        GameApp.startShapeRenderingOutlined();
        drawHandrail();
        GameApp.endShapeRendering();
        GameApp.setLineWidth(1);

        GameApp.startSpriteRendering();
        if (state != State.TITLE) drawAgeha();
        GameApp.endSpriteRendering();

        GameApp.enableTransparency();   // SpriteBatch.end() turns blending off, so turn it on again
        GameApp.startShapeRenderingFilled();
        drawOverlays();
        GameApp.endShapeRendering();

        if (state == State.TITLE) drawTitle();
        if (state == State.CLIMBING && idleTime > 2.5f) drawHint();
    }

    void drawRoom() {
        // wall panels
        for (int i = 0; i < 9; i++) {
            GameApp.drawRect(i * 160 + 20, 0, 2, H, new Color(0.08f, 0.075f, 0.085f, 1));
        }
        // ground floor
        GameApp.drawRect(0, 0, STAIRS_X, GROUND_Y, new Color(0.13f, 0.125f, 0.13f, 1));
        GameApp.drawRect(0, GROUND_Y - 5, STAIRS_X, 5, new Color(0.22f, 0.21f, 0.21f, 1));

        // an empty picture frame on the wall (the family photo that used to hang here)
        float fx = 520, fy = 430, fw = 110, fh = 82;
        GameApp.drawRect(fx - 6, fy - 6, fw + 12, fh + 12, new Color(0.24f, 0.22f, 0.2f, 1));
        GameApp.drawRect(fx, fy, fw, fh, new Color(0.11f, 0.105f, 0.115f, 1));
        GameApp.drawRect(fx + fw / 2 - 1, fy + fh + 6, 2, 30, new Color(0.2f, 0.2f, 0.2f, 1)); // nail string

        // landing + door at the top
        float near = Math.max(0f, Math.min(1f, (pos - (STEPS - 4)) / 5f)); // closer -> brighter light
        GameApp.drawRect(LANDING_X, 0, W - LANDING_X, TOP_Y, new Color(0.17f, 0.165f, 0.17f, 1));
        GameApp.drawRect(LANDING_X, TOP_Y - 6, W - LANDING_X, 6, new Color(0.27f, 0.26f, 0.26f, 1));
        float dx = 1130, dw = 92, dh = 175;
        GameApp.drawRect(dx - 7, TOP_Y, dw + 14, dh + 7, new Color(0.2f, 0.19f, 0.18f, 1));   // frame
        GameApp.drawRect(dx, TOP_Y, dw, dh, new Color(0.075f, 0.07f, 0.07f, 1));              // door
        GameApp.drawRect(dx + 10, TOP_Y + 95, dw - 20, 60, new Color(0.1f, 0.095f, 0.095f, 1)); // panel
        GameApp.drawRect(dx + 10, TOP_Y + 15, dw - 20, 65, new Color(0.1f, 0.095f, 0.095f, 1)); // panel
        GameApp.drawCircle(dx + dw - 14, TOP_Y + 88, 3, new Color(0.55f, 0.52f, 0.45f, 1));    // knob
        GameApp.drawRect(dx, TOP_Y, dw, 3, new Color(1, 0.97f, 0.85f, 0.2f + 0.7f * near));   // light under the door
        GameApp.drawRect(dx - 30, TOP_Y - 2, dw + 60, 2, new Color(1, 0.97f, 0.85f, 0.15f * near));
    }

    void drawStairs() {
        for (int i = 0; i < STEPS; i++) {
            float x = STAIRS_X + i * STEP_W;
            float top = GROUND_Y + (i + 1) * STEP_H;
            float shade = 0.15f + i * 0.002f;
            // solid block under the step
            GameApp.drawRect(x, 0, STEP_W, top, new Color(shade, shade * 0.97f, shade * 0.98f, 1));
            // riser (front face of the step)
            GameApp.drawRect(x, top - STEP_H, 3, STEP_H, new Color(0.09f, 0.085f, 0.09f, 1));
            // tread (the part you step on) with a worn carpet runner
            GameApp.drawRect(x, top - 6, STEP_W, 6, new Color(0.26f, 0.25f, 0.25f, 1));
            GameApp.drawRect(x + 8, top - 4, STEP_W - 16, 4, new Color(0.31f, 0.29f, 0.29f, 1));
        }
        // balusters (behind Ageha), one per step
        for (int i = 0; i < STEPS; i++) {
            float x = STAIRS_X + i * STEP_W + STEP_W / 2 - 2;
            float top = GROUND_Y + (i + 1) * STEP_H;
            GameApp.drawRect(x, top, 4, 92, new Color(0.11f, 0.105f, 0.11f, 1));
        }
        // newel post at the bottom
        GameApp.drawRect(STAIRS_X + 6, GROUND_Y + STEP_H, 12, 110, new Color(0.2f, 0.18f, 0.17f, 1));
    }

    void drawHandrail() {
        Color rail = new Color(0.24f, 0.22f, 0.2f, 1);
        float x0 = STAIRS_X + 12, y0 = GROUND_Y + STEP_H + 110;
        float x1 = LANDING_X - STEP_W / 2, y1 = TOP_Y + 92;
        GameApp.drawLine(x0, y0, x1, y1, rail);
        GameApp.drawLine(x1, y1, W, y1, rail);
    }

    void drawAgeha() {
        String frame = "idle";
        if (state == State.CLIMBING && moving) {
            int phase = (int) Math.floor(pos * 2) % 4;     // a - idle - b - idle, two frames per step
            if (phase < 0) phase += 4;
            frame = phase == 0 ? "a" : phase == 2 ? "b" : "idle";
        }
        GameApp.drawTexture("ageha_right_" + frame, feetX() - AGEHA_W / 2, feetY() - 4, AGEHA_W, AGEHA_H);
    }

    void drawOverlays() {
        if (state == State.FADE_IN) {
            float a = 1f - Math.min(1f, timer / 1.0f);
            GameApp.drawRect(0, 0, W, H, new Color(0, 0, 0, a));
        } else if (state == State.DOOR) {
            // door opens: warm light floods in, then everything goes black
            float t = timer / 2.0f;
            if (t < 0.55f) {
                float a = Math.min(1f, t / 0.55f);
                GameApp.drawRect(0, 0, W, H, new Color(1f, 0.98f, 0.92f, a));
            } else {
                float a = Math.min(1f, (t - 0.55f) / 0.45f);
                GameApp.drawRect(0, 0, W, H, new Color(1f, 0.98f, 0.92f, 1f));
                GameApp.drawRect(0, 0, W, H, new Color(0, 0, 0, a));
            }
        } else if (state == State.TITLE) {
            GameApp.drawRect(0, 0, W, H, new Color(0, 0, 0, 1));
        }
    }

    void drawTitle() {
        float a = Math.min(1f, timer / 0.8f);
        if (timer > 2.6f) a = Math.max(0f, 1f - (timer - 2.6f) / 0.6f);
        Color main = new Color(0.92f, 0.92f, 0.92f, a);
        Color sub = new Color(0.55f, 0.55f, 0.55f, a);
        GameApp.startSpriteRendering();
        GameApp.drawTextCentered("tr_small", dayText, W / 2, H / 2 + 70, sub);
        GameApp.drawTextCentered("tr_big", floorText, W / 2, H / 2, main);
        GameApp.drawTextCentered("tr_small", themeText, W / 2, H / 2 - 70, sub);
        GameApp.endSpriteRendering();
    }

    void drawHint() {
        float a = 0.4f + 0.3f * (float) Math.sin(totalTime * 3);
        GameApp.startSpriteRendering();
        GameApp.drawTextCentered("tr_small", "Hold D / RIGHT to climb", W / 2, 40, new Color(0.8f, 0.8f, 0.8f, a));
        GameApp.endSpriteRendering();
    }

    @Override
    public void hide() {
        GameApp.disposeFont("tr_big");
        GameApp.disposeFont("tr_small");
        for (String f : new String[]{"idle", "a", "b"}) {
            GameApp.disposeTexture("ageha_right_" + f);
        }
        GameApp.disposeSound("step_1");
        GameApp.disposeSound("step_2");
        GameApp.disposeSound("creak");
        GameApp.disposeSound("door_open");
    }
}