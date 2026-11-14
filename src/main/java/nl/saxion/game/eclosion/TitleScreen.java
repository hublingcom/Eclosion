package nl.saxion.game.eclosion;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * TITLE SCREEN - the mansion at night in the rain, title "ECLOSION", a small white butterfly.
 *
 * ENTER  -> door creaks, music fades, screen fades to black -> START_SCREEN
 * Dev shortcuts (for testing): 1 = Floor 1 minigame, 2 = Ageha's room, 3 = stairs -> Floor 1
 *
 * Sounds (src/main/resources/audio): title_bgm.ogg, rain.ogg, thunder.wav, door_open.wav
 * Font: fonts/basic.ttf
 */
public class TitleScreen extends ScalableGameScreen {

    static final String START_SCREEN = "RoomScreen";   // where "Press ENTER" takes you (Day 1: Ageha's room)

    static final float W = 1280, H = 720;
    static final float GROUND_Y = 120;
    static final int DROPS = 220;

    enum State { FADE_IN, IDLE, LEAVING }
    State state;
    float timer, totalTime;

    // rain
    float[] dropX = new float[DROPS], dropY = new float[DROPS], dropSpeed = new float[DROPS], dropLen = new float[DROPS];
    // clouds
    float[] cloudX = {150, 520, 900, 1180};
    float[] cloudY = {600, 650, 590, 640};
    // lightning
    float nextFlash, flash, thunderDelay;
    boolean thunderPending;

    public TitleScreen() {
        super(1280, 720);
    }

    @Override
    public void show() {
        GameApp.addFont("ti_title", "fonts/basic.ttf", 110);
        GameApp.addFont("ti_sub", "fonts/basic.ttf", 26);
        GameApp.addFont("ti_small", "fonts/basic.ttf", 18);
        GameApp.addFont("ti_graffiti", "fonts/basic.ttf", 22);

        GameApp.addMusic("title_bgm", "audio/title_bgm.ogg");
        GameApp.addMusic("rain", "audio/rain.ogg");
        GameApp.addSound("thunder", "audio/thunder.wav");
        GameApp.addSound("door_open", "audio/door_open.wav");
        GameApp.playMusic("rain", true, 0.35f);
        GameApp.playMusic("title_bgm", true, 0.5f);

        GameApp.enableTransparency();
        for (int i = 0; i < DROPS; i++) resetDrop(i, true);

        state = State.FADE_IN;
        timer = 0;
        totalTime = 0;
        nextFlash = GameApp.random(5, 9);
        flash = 0;
        thunderPending = false;
    }

    void resetDrop(int i, boolean anywhere) {
        dropX[i] = GameApp.random(0, W + 200);
        dropY[i] = anywhere ? GameApp.random(0, H) : H + GameApp.random(0, 60);
        dropSpeed[i] = GameApp.random(650, 950);
        dropLen[i] = GameApp.random(12, 26);
    }

    // ======================= UPDATE =======================

    @Override
    public void render(float delta) {
        super.render(delta);
        totalTime += delta;
        timer += delta;

        updateRain(delta);
        updateWeather(delta);

        if (state == State.FADE_IN && timer > 2.0f) {
            state = State.IDLE;
            timer = 0;
        } else if (state == State.IDLE) {
            if (GameApp.isKeyJustPressed(Input.Keys.ENTER)) {
                GameApp.playSound("door_open", 0.9f);
                state = State.LEAVING;
                timer = 0;
            }
            // dev shortcuts
            if (GameApp.isKeyJustPressed(Input.Keys.NUM_1)) { GameApp.switchScreen("Floor1FlowerScreen"); return; }
            if (GameApp.isKeyJustPressed(Input.Keys.NUM_2)) { GameApp.switchScreen("RoomScreen"); return; }
            if (GameApp.isKeyJustPressed(Input.Keys.NUM_3)) {
                FloorTransitionScreen.prepare("Day 2", "Floor 1", "Loneliness", "Floor1FlowerScreen");
                GameApp.switchScreen("FloorTransitionScreen");
                return;
            }
        } else if (state == State.LEAVING) {
            float v = Math.max(0f, 1f - timer / 2.2f);
            GameApp.getMusic("title_bgm").setVolume(0.5f * v);
            GameApp.getMusic("rain").setVolume(0.35f * v);
            if (timer > 2.4f) {
                GameApp.switchScreen(START_SCREEN);
                return;
            }
        }

        draw();
    }

    void updateRain(float delta) {
        for (int i = 0; i < DROPS; i++) {
            dropY[i] -= dropSpeed[i] * delta;
            dropX[i] -= dropSpeed[i] * 0.22f * delta;
            if (dropY[i] < -30) resetDrop(i, false);
        }
        for (int i = 0; i < cloudX.length; i++) {
            cloudX[i] += 8 * delta;
            if (cloudX[i] > W + 250) cloudX[i] = -250;
        }
    }

    void updateWeather(float delta) {
        nextFlash -= delta;
        if (nextFlash <= 0) {
            flash = 1f;
            thunderPending = true;
            thunderDelay = GameApp.random(0.4f, 1.2f);   // light first, sound a moment later
            nextFlash = GameApp.random(9, 16);
        }
        if (flash > 0) flash = Math.max(0, flash - delta * 2.2f);
        if (thunderPending) {
            thunderDelay -= delta;
            if (thunderDelay <= 0) {
                GameApp.getSound("thunder").play(0.8f, GameApp.random(0.9f, 1.05f), 0f);
                thunderPending = false;
            }
        }
    }

    // ======================= DRAW =======================

    void draw() {
        GameApp.clearScreen(10, 11, 14);

        GameApp.startShapeRenderingFilled();
        drawSky();
        drawMansion();
        drawFence();
        drawButterfly();
        GameApp.endShapeRendering();

        GameApp.startShapeRenderingOutlined();
        drawRain();
        GameApp.endShapeRendering();

        GameApp.startSpriteRendering();
        drawTexts();
        GameApp.endSpriteRendering();

        GameApp.startShapeRenderingFilled();
        drawOverlays();
        GameApp.endShapeRendering();
    }

    void drawSky() {
        // vertical gradient
        for (int i = 0; i < 12; i++) {
            float c = 0.04f + i * 0.007f;
            GameApp.drawRect(0, H - (i + 1) * 60, W, 60, new Color(c, c, c * 1.15f, 1));
        }
        // moon behind thin clouds
        GameApp.drawCircle(1040, 600, 70, new Color(1, 1, 1, 0.04f));
        GameApp.drawCircle(1040, 600, 46, new Color(0.78f, 0.78f, 0.8f, 1));
        for (int i = 0; i < cloudX.length; i++) {
            GameApp.drawEllipseCentered(cloudX[i], cloudY[i], 420, 70, new Color(0.16f, 0.16f, 0.18f, 0.55f));
            GameApp.drawEllipseCentered(cloudX[i] + 90, cloudY[i] + 18, 260, 60, new Color(0.18f, 0.18f, 0.2f, 0.45f));
        }
        // trees at the sides
        Color tree = new Color(0.035f, 0.035f, 0.045f, 1);
        GameApp.drawCircle(60, 260, 140, tree);
        GameApp.drawCircle(150, 360, 110, tree);
        GameApp.drawCircle(1220, 280, 150, tree);
        GameApp.drawCircle(1120, 380, 100, tree);
        GameApp.drawRect(0, 0, W, GROUND_Y, new Color(0.05f, 0.05f, 0.06f, 1));
    }

    /** a triangle roof made of thin slices (ShapeRenderer has no filled polygons) */
    void roof(float cx, float baseY, float baseW, float h, Color c) {
        int n = 40;
        for (int k = 0; k < n; k++) {
            float w = baseW * (1f - (float) k / n);
            GameApp.drawRect(cx - w / 2, baseY + k * h / n, w, h / n + 1, c);
        }
    }

    void window(float x, float y, boolean lit) {
        GameApp.drawRect(x - 3, y - 3, 40, 58, new Color(0.13f, 0.13f, 0.15f, 1));
        if (lit) {
            float f = 0.75f + 0.1f * (float) Math.sin(totalTime * 7) + 0.05f * (float) Math.sin(totalTime * 17);
            GameApp.drawCircle(x + 17, y + 26, 60, new Color(1f, 0.92f, 0.7f, 0.06f));
            GameApp.drawRect(x, y, 34, 52, new Color(0.95f * f, 0.88f * f, 0.65f * f, 1));
        } else {
            GameApp.drawRect(x, y, 34, 52, new Color(0.05f, 0.05f, 0.065f, 1));
        }
        GameApp.drawRect(x + 16, y, 2, 52, new Color(0.13f, 0.13f, 0.15f, 1));
        GameApp.drawRect(x, y + 25, 34, 2, new Color(0.13f, 0.13f, 0.15f, 1));
    }

    void drawMansion() {
        Color wall = new Color(0.09f, 0.09f, 0.105f, 1);
        Color wall2 = new Color(0.075f, 0.075f, 0.09f, 1);
        Color roofC = new Color(0.06f, 0.06f, 0.07f, 1);

        // towers and body
        GameApp.drawRect(300, GROUND_Y, 100, 380, wall2);           // left tower
        roof(350, GROUND_Y + 380, 130, 140, roofC);
        GameApp.drawRect(880, GROUND_Y, 100, 350, wall2);           // right tower
        roof(930, GROUND_Y + 350, 130, 120, roofC);
        GameApp.drawRect(380, GROUND_Y, 520, 300, wall);            // main body
        roof(640, GROUND_Y + 300, 580, 130, roofC);
        GameApp.drawRect(760, GROUND_Y + 330, 34, 90, roofC);       // chimney

        // windows (only one is lit: the top of the right tower)
        float[] rows = {GROUND_Y + 40, GROUND_Y + 130, GROUND_Y + 220};
        float[] cols = {430, 500, 570, 690, 760, 830};
        for (float r : rows) for (float c : cols) {
            if (r == rows[0] && (c == 570 || c == 690)) continue;  // space for the door
            window(c, r, false);
        }
        window(333, GROUND_Y + 150, false);
        window(333, GROUND_Y + 260, false);
        window(913, GROUND_Y + 150, false);
        window(913, GROUND_Y + 260, true);

        // front door + steps
        GameApp.drawRect(600, GROUND_Y, 80, 120, new Color(0.04f, 0.035f, 0.04f, 1));
        GameApp.drawRect(594, GROUND_Y + 118, 92, 8, new Color(0.13f, 0.13f, 0.15f, 1));
        GameApp.drawRect(580, GROUND_Y - 8, 120, 8, new Color(0.12f, 0.12f, 0.13f, 1));
        // path to the gate
        for (int i = 0; i < 8; i++) {
            float w = 90 + i * 22;
            GameApp.drawRect(640 - w / 2, GROUND_Y - 16 - i * 14, w, 14, new Color(0.075f + i * 0.003f, 0.075f, 0.08f, 1));
        }
    }

    void drawFence() {
        Color iron = new Color(0.025f, 0.025f, 0.03f, 1);
        for (float x = 0; x < W; x += 24) {
            if (x > 560 && x < 720) continue;   // gate opening
            GameApp.drawRect(x, 0, 5, 128, iron);
            GameApp.drawRect(x - 1, 126, 7, 8, iron);
        }
        GameApp.drawRect(0, 30, 560, 5, iron);
        GameApp.drawRect(720, 30, W - 720, 5, iron);
        GameApp.drawRect(0, 105, 560, 5, iron);
        GameApp.drawRect(720, 105, W - 720, 5, iron);
        // gate (tall bars + arch)
        GameApp.drawRect(552, 0, 14, 190, iron);
        GameApp.drawRect(714, 0, 14, 190, iron);
        for (float x = 578; x < 710; x += 20) GameApp.drawRect(x, 0, 4, 160, iron);
        GameApp.drawRect(566, 158, 148, 5, iron);
        GameApp.drawRect(566, 60, 148, 5, iron);
        GameApp.drawEllipseCentered(640, 168, 170, 34, iron);
        GameApp.drawEllipseCentered(640, 168, 150, 22, new Color(0.06f, 0.06f, 0.07f, 1));
    }

    void drawButterfly() {
        // a small white butterfly drifting near the title
        float bx = 640 + 330 * (float) Math.sin(totalTime * 0.23f) + 30 * (float) Math.sin(totalTime * 1.1f);
        float by = 560 + 45 * (float) Math.sin(totalTime * 0.37f) + 12 * (float) Math.sin(totalTime * 2.3f);
        float flap = 0.25f + 0.75f * Math.abs((float) Math.sin(totalTime * 9));
        Color wing = new Color(0.95f, 0.95f, 0.95f, 0.9f);
        GameApp.drawEllipseCentered(bx - 7 * flap, by + 4, 14 * flap, 16, wing);
        GameApp.drawEllipseCentered(bx + 7 * flap, by + 4, 14 * flap, 16, wing);
        GameApp.drawEllipseCentered(bx - 5 * flap, by - 6, 10 * flap, 10, wing);
        GameApp.drawEllipseCentered(bx + 5 * flap, by - 6, 10 * flap, 10, wing);
        GameApp.drawRect(bx - 1, by - 9, 2, 18, new Color(0.3f, 0.3f, 0.3f, 1));
    }

    void drawRain() {
        Color drop = new Color(0.6f, 0.62f, 0.68f, 0.35f);
        for (int i = 0; i < DROPS; i++) {
            GameApp.drawLine(dropX[i], dropY[i], dropX[i] + dropLen[i] * 0.22f, dropY[i] + dropLen[i], drop);
        }
    }

    void drawTexts() {
        float a = state == State.FADE_IN ? Math.min(1f, timer / 2f) : 1f;
        GameApp.drawTextCentered("ti_title", "ECLOSION", W / 2, 560, new Color(0.9f, 0.9f, 0.92f, a));
        GameApp.drawTextCentered("ti_sub", "seven days until eighteen", W / 2, 480, new Color(0.55f, 0.55f, 0.6f, a));
        // graffiti on the gate (from the kids at school)
        GameApp.drawTextCentered("ti_graffiti", "GO AWAY", 640, 95, new Color(0.55f, 0.55f, 0.55f, 0.55f));
        if (state == State.IDLE) {
            float blink = 0.45f + 0.35f * (float) Math.sin(totalTime * 2.5f);
            GameApp.drawTextCentered("ti_sub", "Press ENTER", W / 2, 30, new Color(0.85f, 0.85f, 0.85f, blink));
            GameApp.drawText("ti_small", "dev: 1 floor1  2 room  3 stairs", W - 290, 18, new Color(0.4f, 0.4f, 0.4f, 0.5f));
        }
    }

    void drawOverlays() {
        if (flash > 0) {
            float f = flash > 0.7f ? flash : flash * 0.5f;          // double flicker
            GameApp.drawRect(0, 0, W, H, new Color(0.85f, 0.87f, 0.95f, 0.35f * f));
        }
        if (state == State.FADE_IN) {
            GameApp.drawRect(0, 0, W, H, new Color(0, 0, 0, 1f - Math.min(1f, timer / 2f)));
        } else if (state == State.LEAVING) {
            GameApp.drawRect(0, 0, W, H, new Color(0, 0, 0, Math.min(1f, timer / 2.2f)));
        }
    }

    @Override
    public void hide() {
        GameApp.stopMusic("title_bgm");
        GameApp.stopMusic("rain");
        GameApp.disposeMusic("title_bgm");
        GameApp.disposeMusic("rain");
        GameApp.disposeSound("thunder");
        GameApp.disposeSound("door_open");
        GameApp.disposeFont("ti_title");
        GameApp.disposeFont("ti_sub");
        GameApp.disposeFont("ti_small");
        GameApp.disposeFont("ti_graffiti");
    }
}
