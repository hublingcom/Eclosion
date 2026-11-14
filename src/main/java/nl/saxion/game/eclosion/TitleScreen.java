package nl.saxion.game.eclosion;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * TITLE SCREEN - painted mansion background (images/title_bg.png) with animated rain, lightning,
 * a flickering lit window, the title "ECLOSION" and a small white butterfly.
 *
 * ENTER  -> door creaks, music fades, screen fades to black -> START_SCREEN
 * Dev shortcuts (for testing): 1 = Floor 1 minigame, 2 = Ageha's room, 3 = stairs -> night cutscene -> Floor 1, 4 = odd color, 5 = dialogue demo, 6 = night -> morning, 7 = door
 *
 * Image  (src/main/resources/images): title_bg.png (1280x720)
 * Sounds (src/main/resources/audio): title_bgm.ogg, rain.ogg, thunder.wav, door_open.wav
 * Font: fonts/basic.ttf
 */
public class TitleScreen extends ScalableGameScreen {

    static final String START_SCREEN = "RoomScreen";   // where "Press ENTER" takes you (Day 1: Ageha's room)

    static final float W = 1280, H = 720;
    static final int DROPS = 130;

    enum State { FADE_IN, IDLE, LEAVING }
    State state;
    float timer, totalTime;

    // rain
    float[] dropX = new float[DROPS], dropY = new float[DROPS], dropSpeed = new float[DROPS], dropLen = new float[DROPS];
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
        GameApp.addTexture("title_bg", "images/title_bg.png");

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
            if (GameApp.isKeyJustPressed(Input.Keys.NUM_4)) { GameApp.switchScreen("OddColorScreen"); return; }
            if (GameApp.isKeyJustPressed(Input.Keys.NUM_5)) { GameApp.switchScreen("DialogueDemoScreen"); return; }
            if (GameApp.isKeyJustPressed(Input.Keys.NUM_6)) { DayTransitionScreen.prepare(2, "RoomScreen"); GameApp.switchScreen("DayTransitionScreen"); return; }
            if (GameApp.isKeyJustPressed(Input.Keys.NUM_7)) { DoorTransitionScreen.go("RoomScreen", "Ageha's room"); return; }
            if (GameApp.isKeyJustPressed(Input.Keys.NUM_3)) {
                FloorTransitionScreen.prepare("Day 2", "Floor 1", "Loneliness", "NightCutsceneScreen");
                NightCutsceneScreen.prepare("Floor1FlowerScreen");
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

    // where things are in the painting (world coordinates, y up)
    static final float WINDOW_X = 969, WINDOW_Y = 502;   // the one lit window in the right tower
    static final float TITLE_X = 330, TITLE_Y = 450;     // title sits over the dark trees on the left

    void draw() {
        GameApp.clearScreen(0, 0, 0);

        GameApp.startSpriteRendering();
        GameApp.drawTexture("title_bg", 0, 0, W, H);
        GameApp.endSpriteRendering();

        GameApp.enableTransparency();   // SpriteBatch.end() turns blending off, so turn it on again
        GameApp.startShapeRenderingFilled();
        // darken the left side a little so the title is easy to read
        for (int i = 0; i < 8; i++) {
            GameApp.drawRect(i * 90, 0, 90, H, new Color(0, 0, 0, 0.32f - i * 0.04f));
        }
        drawWindowGlow();
        drawButterfly();
        GameApp.endShapeRendering();

        GameApp.enableTransparency();   // SpriteBatch.end() turns blending off, so turn it on again
        GameApp.startShapeRenderingOutlined();
        drawRain();
        GameApp.endShapeRendering();

        GameApp.startSpriteRendering();
        drawTexts();
        GameApp.endSpriteRendering();

        GameApp.enableTransparency();   // SpriteBatch.end() turns blending off, so turn it on again
        GameApp.startShapeRenderingFilled();
        drawOverlays();
        GameApp.endShapeRendering();
    }

    /** the lit window flickers softly, like a lamp someone is still awake next to */
    void drawWindowGlow() {
        float f = 0.5f + 0.25f * (float) Math.sin(totalTime * 2.3f) + 0.12f * (float) Math.sin(totalTime * 9.7f);
        GameApp.drawCircle(WINDOW_X, WINDOW_Y, 38, new Color(1f, 0.85f, 0.5f, 0.05f * f));
        GameApp.drawCircle(WINDOW_X, WINDOW_Y, 20, new Color(1f, 0.85f, 0.5f, 0.10f * f));
        GameApp.drawRect(WINDOW_X - 5, WINDOW_Y - 14, 10, 28, new Color(1f, 0.9f, 0.6f, 0.18f * f));
    }

    void drawButterfly() {
        // a small white butterfly drifting around the title
        float bx = TITLE_X + 250 * (float) Math.sin(totalTime * 0.23f) + 30 * (float) Math.sin(totalTime * 1.1f);
        float by = TITLE_Y + 95 + 40 * (float) Math.sin(totalTime * 0.37f) + 12 * (float) Math.sin(totalTime * 2.3f);
        float flap = 0.25f + 0.75f * Math.abs((float) Math.sin(totalTime * 9));
        Color wing = new Color(0.95f, 0.95f, 0.95f, 0.9f);
        GameApp.drawEllipseCentered(bx - 7 * flap, by + 4, 14 * flap, 16, wing);
        GameApp.drawEllipseCentered(bx + 7 * flap, by + 4, 14 * flap, 16, wing);
        GameApp.drawEllipseCentered(bx - 5 * flap, by - 6, 10 * flap, 10, wing);
        GameApp.drawEllipseCentered(bx + 5 * flap, by - 6, 10 * flap, 10, wing);
        GameApp.drawRect(bx - 1, by - 9, 2, 18, new Color(0.3f, 0.3f, 0.3f, 1));
    }

    void drawRain() {
        Color drop = new Color(0.75f, 0.78f, 0.85f, 0.22f);
        for (int i = 0; i < DROPS; i++) {
            GameApp.drawLine(dropX[i], dropY[i], dropX[i] + dropLen[i] * 0.22f, dropY[i] + dropLen[i], drop);
        }
    }

    /** text with a soft dark shadow so it stays readable on the painting */
    void shadowText(String font, String text, float x, float y, Color c) {
        GameApp.drawTextCentered(font, text, x + 3, y - 3, new Color(0, 0, 0, 0.7f * c.a));
        GameApp.drawTextCentered(font, text, x, y, c);
    }

    void drawTexts() {
        float a = state == State.FADE_IN ? Math.min(1f, timer / 2f) : 1f;
        shadowText("ti_title", "ECLOSION", TITLE_X, TITLE_Y, new Color(0.92f, 0.92f, 0.94f, a));
        shadowText("ti_sub", "seven days until eighteen", TITLE_X, TITLE_Y - 75, new Color(0.65f, 0.65f, 0.7f, a));
        if (state == State.IDLE) {
            float blink = 0.45f + 0.35f * (float) Math.sin(totalTime * 2.5f);
            shadowText("ti_sub", "Press ENTER", TITLE_X, TITLE_Y - 150, new Color(0.9f, 0.9f, 0.9f, blink));
            GameApp.drawText("ti_small", "dev: 1 floor1  2 room  3 stairs  4 colors  5 dialogue  6 night  7 door", W - 560, 18, new Color(0.5f, 0.5f, 0.5f, 0.5f));
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
        GameApp.disposeTexture("title_bg");
    }
}