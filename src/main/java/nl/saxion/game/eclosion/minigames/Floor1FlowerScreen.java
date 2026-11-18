package nl.saxion.game.eclosion.minigames;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import java.util.ArrayList;
import nl.saxion.game.eclosion.audio.Bgm;
import nl.saxion.game.eclosion.audio.Sfx;
import nl.saxion.game.eclosion.explore.ExploreScreen;
import nl.saxion.game.eclosion.rooms.MainHallScreen;
import nl.saxion.game.eclosion.ui.ClickEffect;
import nl.saxion.game.eclosion.ui.PixelFont;
import nl.saxion.game.eclosion.ui.ScreenFader;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * FLOOR 1 MINIGAME - "Queen of the Night"
 *
 * Queen of the Night flowers fall from the dark sky. Move Ageha left/right and catch them.
 *  - Pass criterion  : catch PASS_TARGET flowers before the night ends  -> next floor
 *  - Item criterion  : catch ITEM_TARGET flowers                         -> memory item + photo piece
 *  - No harsh game over: when the night ends you just try again.
 *    After HELP_AFTER_FAILS failures a little ghost comes to help (wider catch, slower flowers).
 *
 * Controls: A/D or LEFT/RIGHT to move, Enter / Space / E / click to start / continue, Esc = back.
 *
 * Made by Dawon (branch Dawon, eclosion.floor1.Floor1FlowerScreen). Changed for this version:
 * started from Bluma in the main hall and goes back there, the game's pixel font, Ageha walk frames,
 * sounds and music, fade-in, click circle, and the same keys as the rest of the game.
 */
public class Floor1FlowerScreen extends ScalableGameScreen {

    // ---------- tuning (team can change these numbers in Sprint 0) ----------
    static final int PASS_TARGET = 15;
    static final int ITEM_TARGET = 25;
    static final float NIGHT_LENGTH = 45f;       // seconds
    static final int HELP_AFTER_FAILS = 2;

    static final float AGEHA_SPEED = 430f;
    static final float AGEHA_HEIGHT = 160f;
    static final float AGEHA_WIDTH = AGEHA_HEIGHT * 243f / 330f;   // the game's Ageha walk frames are 243 x 330
    static final float AGEHA_Y = 40f;
    static final float CATCH_WIDTH = 120f;       // width of the catch zone (wider than the sprite)
    static final float CATCH_HEIGHT = 110f;      // upper body + head

    static final float SPAWN_START = 0.95f;      // seconds between flowers at start of the night
    static final float SPAWN_END = 0.45f;        // ...and at the end
    static final float FALL_MIN = 150f;
    static final float FALL_MAX = 240f;
    static final float FALL_BONUS_END = 120f;    // extra speed reached at the end of the night

    // Withered flowers (the twist): catching one takes points away
    static final float WILTED_CHANCE_START = 0.18f;  // chance a new flower is withered at the start
    static final float WILTED_CHANCE_END = 0.35f;    // ...and at the end of the night
    static final float WILTED_CHANCE_GHOST = 0.12f;  // lower when the ghost is helping
    static final int WILTED_PENALTY = 2;

    // Started from Bluma's options in the main hall, so it goes back there
    // (Dawon's demo went to "EveningRoomScreen" here)
    static final String NEXT_SCREEN = "MainHallScreen";

    // ---------- state ----------
    enum State { INTRO, PLAYING, RESULT }
    State state = State.INTRO;

    float agehaX;                 // left x of the sprite
    boolean facingLeft = false;
    boolean moving = false;
    float walkTimer = 0f;

    ArrayList<Flower> flowers = new ArrayList<>();
    float spawnTimer = 0f;
    float timeLeft = NIGHT_LENGTH;
    int caught = 0;
    int missed = 0;
    int failCount = 0;
    boolean ghostHelping = false;
    boolean passed = false;
    boolean gotItem = false;

    // pentatonic scale -> every caught flower plays the next note (a little melody)
    static final float[] NOTE_PITCHES = {1.0f, 1.125f, 1.25f, 1.5f, 1.667f, 2.0f};

    float catchFlash = 0f;        // little white flash when catching
    float flashX, flashY;
    float wiltFlash = 0f;         // dark flash + "-2" when catching a withered flower
    float wiltX, wiltY;

    float[] starX = new float[60];
    float[] starY = new float[60];
    float[] starPhase = new float[60];
    float totalTime = 0f;

    public Floor1FlowerScreen() {
        super(1280, 720);
    }

    @Override
    public void show() {
        // the game's pixel font (like the dialogue box) instead of basic.ttf
        GameApp.addFont("f1_big", PixelFont.make(48));
        GameApp.addFont("f1_mid", PixelFont.make(28));
        GameApp.addFont("f1_small", PixelFont.make(20));
        // the game's own sounds: soft glass chime (Kenney, CC0) and
        // "PS2 horror / mystery ambient theme" (Tarush Singhal, CC0) as music
        GameApp.addSound("catch", "audio/sfx_catch.wav");
        Bgm.play(Bgm.MINIGAME1, 0.45f);

        // the same Ageha walk frames as in the rooms (textures/sprites/ageha_walk_*)
        for (String d : AGEHA_DIRECTIONS) {
            for (int n = 1; n <= 3; n++) {
                GameApp.addTexture(agehaFrame(d, n), "textures/sprites/" + agehaFrame(d, n) + ".png");
            }
        }
        fader.fadeIn();

        for (int i = 0; i < starX.length; i++) {
            starX[i] = GameApp.random(0, getWorldWidth());
            starY[i] = GameApp.random(200, getWorldHeight());
            starPhase[i] = GameApp.random(0, 6.28f);
        }

        GameApp.enableTransparency();
        state = State.INTRO;
        failCount = 0;
        ghostHelping = false;
        resetRound();
    }

    void resetRound() {
        flowers.clear();
        agehaX = getWorldWidth() / 2 - AGEHA_WIDTH / 2;
        spawnTimer = 0f;
        timeLeft = NIGHT_LENGTH;
        caught = 0;
        missed = 0;
        passed = false;
        gotItem = false;
        catchFlash = 0f;
        wiltFlash = 0f;
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        totalTime += delta;

        fader.update(delta);
        if (fader.busy()) {
            draw();
            fader.render(getWorldWidth(), getWorldHeight());
            return;                      // no input while the screen fades in
        }

        // Escape: give up and go back to the main hall
        if (GameApp.isKeyJustPressed(Input.Keys.ESCAPE)) {
            backToHall();
            return;
        }

        if (state == State.INTRO) {
            if (confirmPressed()) {
                resetRound();
                state = State.PLAYING;
            }
        } else if (state == State.PLAYING) {
            updatePlaying(delta);
        } else if (state == State.RESULT) {
            if (confirmPressed()) {
                if (passed) {
                    // TODO: save gotItem somewhere global (photo piece) before switching
                    backToHall();
                    return;
                } else {
                    resetRound();
                    state = State.PLAYING;
                }
            }
        }

        draw();
        ClickEffect.updateAndDraw(getMouseX(), getMouseY());
    }

    /** Like everywhere in the game, Enter / Space / E / a click goes on. */
    boolean confirmPressed() {
        return GameApp.isKeyJustPressed(Input.Keys.ENTER) || GameApp.isKeyJustPressed(Input.Keys.SPACE)
                || GameApp.isKeyJustPressed(Input.Keys.E) || GameApp.isButtonJustPressed(Input.Buttons.LEFT);
    }

    static final String[] AGEHA_DIRECTIONS = {"left", "right", "down"};

    static String agehaFrame(String direction, int number) {
        return "ageha_walk_" + direction + "_" + number;
    }

    final ScreenFader fader = new ScreenFader();

    // ---------- update ----------

    void updatePlaying(float delta) {
        // --- move Ageha ---
        moving = false;
        if (GameApp.isKeyPressed(Input.Keys.LEFT) || GameApp.isKeyPressed(Input.Keys.A)) {
            agehaX -= AGEHA_SPEED * delta;
            facingLeft = true;
            moving = true;
        }
        if (GameApp.isKeyPressed(Input.Keys.RIGHT) || GameApp.isKeyPressed(Input.Keys.D)) {
            agehaX += AGEHA_SPEED * delta;
            facingLeft = false;
            moving = true;
        }
        agehaX = GameApp.clamp(agehaX, 0, getWorldWidth() - AGEHA_WIDTH);
        if (moving) walkTimer += delta;

        // --- timer ---
        timeLeft -= delta;
        float progress = 1f - timeLeft / NIGHT_LENGTH;   // 0 at start -> 1 at end of night

        // --- spawn flowers ---
        spawnTimer -= delta;
        if (spawnTimer <= 0) {
            spawnFlower(progress);
            spawnTimer = SPAWN_START + (SPAWN_END - SPAWN_START) * progress;
        }

        // --- move flowers + catch check ---
        float catchW = ghostHelping ? CATCH_WIDTH * 1.6f : CATCH_WIDTH;
        float catchX = agehaX + AGEHA_WIDTH / 2 - catchW / 2;
        float catchY = AGEHA_Y + AGEHA_HEIGHT - CATCH_HEIGHT;

        for (Flower f : flowers) {
            f.swayTime += delta;
            f.y -= f.speed * delta;
            f.x = f.startX + (float) Math.sin(f.swayTime * 2f) * f.swayAmount;
            f.rotation += 40f * delta;

            if (GameApp.rectCircleOverlap(catchX, catchY, catchW, CATCH_HEIGHT, f.x, f.y, f.radius)) {
                f.caught = true;
                if (f.wilted) {
                    caught = Math.max(0, caught - WILTED_PENALTY);
                    wiltFlash = 0.6f;
                    wiltX = f.x;
                    wiltY = f.y;
                    // same sound, low and quiet -> sounds "wrong"
                    GameApp.getSound("catch").play(0.4f * Sfx.volume, 0.5f, 0f);
                } else {
                    caught++;
                    catchFlash = 0.25f;
                    flashX = f.x;
                    flashY = f.y;
                    playCatchSound();
                }
            } else if (f.y < -f.radius) {
                f.missed = true;
                missed++;
            }
        }
        flowers.removeIf(f -> f.caught || f.missed);

        if (catchFlash > 0) catchFlash -= delta;
        if (wiltFlash > 0) wiltFlash -= delta;

        // --- end of night (or all flowers for the item already caught) ---
        if (timeLeft <= 0 || caught >= ITEM_TARGET) {
            timeLeft = Math.max(0, timeLeft);
            passed = caught >= PASS_TARGET;
            gotItem = caught >= ITEM_TARGET;
            if (!passed) {
                failCount++;
                if (failCount >= HELP_AFTER_FAILS) ghostHelping = true;
            }
            state = State.RESULT;
        }
    }

    void playCatchSound() {
        float pitch = NOTE_PITCHES[(caught - 1) % NOTE_PITCHES.length];
        // volume 0.6, pitch = note, pan 0 (center)
        GameApp.getSound("catch").play(0.45f * Sfx.volume, pitch, 0f);
    }

    void spawnFlower(float progress) {
        Flower f = new Flower();
        f.radius = GameApp.random(16, 22);
        f.startX = GameApp.random(60, getWorldWidth() - 60);
        f.x = f.startX;
        f.y = getWorldHeight() + f.radius;
        f.speed = GameApp.random(FALL_MIN, FALL_MAX) + FALL_BONUS_END * progress;
        if (ghostHelping) f.speed *= 0.85f;
        f.swayAmount = GameApp.random(10, 40);
        f.swayTime = GameApp.random(0, 6.28f);
        f.rotation = GameApp.random(0, 360);
        float wiltChance = ghostHelping
                ? WILTED_CHANCE_GHOST
                : WILTED_CHANCE_START + (WILTED_CHANCE_END - WILTED_CHANCE_START) * progress;
        f.wilted = GameApp.random(0f, 1f) < wiltChance;
        flowers.add(f);
    }

    // ---------- draw ----------

    void draw() {
        // Black-and-white world (color returns after this floor -> yellow)
        GameApp.clearScreen(12, 12, 16);

        GameApp.enableTransparency();   // SpriteBatch.end() turns blending off, so turn it on again
        GameApp.startShapeRenderingFilled();
        drawBackground();
        for (Flower f : flowers) drawFlower(f);
        if (catchFlash > 0) {
            int a = (int) (255 * (catchFlash / 0.25f));
            GameApp.drawCircle(flashX, flashY, 30 + (0.25f - catchFlash) * 120, new Color(1, 1, 1, a / 255f * 0.5f));
        }
        if (wiltFlash > 0) {
            float k = wiltFlash / 0.6f;
            GameApp.drawCircle(wiltX, wiltY, 30 + (1 - k) * 60, new Color(0.35f, 0.1f, 0.2f, 0.5f * k));
        }
        if (ghostHelping && state != State.INTRO) drawGhost();
        GameApp.endShapeRendering();

        GameApp.startSpriteRendering();
        drawAgeha();
        drawHud();
        GameApp.endSpriteRendering();

        if (state == State.INTRO) drawOverlay(introLines());
        if (state == State.RESULT) drawOverlay(resultLines());
    }

    void drawBackground() {
        // twinkling stars
        for (int i = 0; i < starX.length; i++) {
            float b = 0.4f + 0.3f * (float) Math.sin(totalTime * 1.5f + starPhase[i]);
            GameApp.drawCircle(starX[i], starY[i], 1.6f, new Color(b, b, b, 1));
        }
        // moon
        GameApp.drawCircle(1100, 610, 55, new Color(0.85f, 0.85f, 0.85f, 1));
        GameApp.drawCircle(1120, 625, 50, new Color(12 / 255f, 12 / 255f, 16 / 255f, 1)); // crescent cut
        // garden floor
        GameApp.drawRect(0, 0, getWorldWidth(), AGEHA_Y + 8, new Color(0.18f, 0.18f, 0.2f, 1));
        // mansion silhouette on the left
        GameApp.drawRect(0, AGEHA_Y, 140, 380, new Color(0.08f, 0.08f, 0.1f, 1));
        GameApp.drawRect(40, AGEHA_Y + 380, 60, 60, new Color(0.08f, 0.08f, 0.1f, 1));
        GameApp.drawRect(30, AGEHA_Y + 200, 30, 45, new Color(0.25f, 0.25f, 0.25f, 1)); // dim window
    }

    /** Queen of the Night: big white flower, 6 petals + pale center. */
    void drawFlower(Flower f) {
        if (f.wilted) {
            drawWiltedFlower(f);
            return;
        }
        Color petal = new Color(0.95f, 0.95f, 0.95f, 1);
        Color center = new Color(0.75f, 0.75f, 0.7f, 1);
        GameApp.drawCircle(f.x, f.y, f.radius * 1.4f, new Color(1, 1, 1, 0.08f)); // soft glow
        for (int i = 0; i < 6; i++) {
            double ang = Math.toRadians(f.rotation + i * 60);
            float px = f.x + (float) Math.cos(ang) * f.radius * 0.6f;
            float py = f.y + (float) Math.sin(ang) * f.radius * 0.6f;
            GameApp.drawCircle(px, py, f.radius * 0.45f, petal);
        }
        GameApp.drawCircle(f.x, f.y, f.radius * 0.35f, center);
    }

    /** Withered flower: dark grey, drooping petals, no glow. Don't catch it! */
    void drawWiltedFlower(Flower f) {
        Color petal = new Color(0.30f, 0.27f, 0.31f, 1);
        Color center = new Color(0.10f, 0.08f, 0.10f, 1);
        // stem-like dark shadow so it reads as "dead"
        GameApp.drawCircle(f.x, f.y, f.radius * 1.3f, new Color(0.25f, 0.05f, 0.12f, 0.18f));
        // 5 petals hanging down (only the lower half of the circle)
        for (int i = 0; i < 5; i++) {
            double ang = Math.toRadians(200 + i * 35 + Math.sin(f.swayTime * 2 + i) * 6);
            float px = f.x + (float) Math.cos(ang) * f.radius * 0.7f;
            float py = f.y + (float) Math.sin(ang) * f.radius * 0.7f;
            GameApp.drawCircle(px, py, f.radius * 0.38f, petal);
        }
        GameApp.drawCircle(f.x, f.y, f.radius * 0.35f, center);
    }

    /** Little helper ghost floating next to Ageha. */
    void drawGhost() {
        float gx = agehaX + AGEHA_WIDTH + 40;
        float gy = AGEHA_Y + 150 + (float) Math.sin(totalTime * 3) * 8;
        Color body = new Color(1, 1, 1, 0.55f);
        GameApp.drawCircle(gx, gy, 26, body);
        GameApp.drawRect(gx - 26, gy - 30, 52, 30, body);
        GameApp.drawCircle(gx - 9, gy + 4, 4, new Color(0, 0, 0, 0.8f));
        GameApp.drawCircle(gx + 9, gy + 4, 4, new Color(0, 0, 0, 0.8f));
    }

    void drawAgeha() {
        String name;
        if (state == State.PLAYING && moving) {
            String dir = facingLeft ? "left" : "right";
            int frame = ((int) (walkTimer / 0.15f)) % 2 == 0 ? 1 : 3;   // the two step frames
            name = agehaFrame(dir, frame);
        } else {
            name = agehaFrame("down", 2);                              // standing
        }
        GameApp.drawTexture(name, agehaX, AGEHA_Y, AGEHA_WIDTH, AGEHA_HEIGHT);
    }

    void drawHud() {
        if (state == State.INTRO) return;
        GameApp.drawText("f1_mid", "Flowers: " + caught + " / " + PASS_TARGET, 170, 690, "white");
        String star = caught >= ITEM_TARGET ? "  (item!)" : "  (item: " + ITEM_TARGET + ")";
        GameApp.drawText("f1_small", star, 170, 645, "gray-400");
        GameApp.drawText("f1_mid", "Night: " + (int) Math.ceil(timeLeft) + "s", 900, 690, "white");
        if (ghostHelping) {
            GameApp.drawText("f1_small", "A little ghost is helping you.", 170, 610, "gray-300");
        }
        if (wiltFlash > 0) {
            GameApp.drawText("f1_mid", "-" + WILTED_PENALTY, wiltX - 15, wiltY + 60 + (0.6f - wiltFlash) * 60, "red-400");
        }
    }

    void drawOverlay(String[] lines) {
        GameApp.enableTransparency();   // SpriteBatch.end() turns blending off, so turn it on again
        GameApp.startShapeRenderingFilled();
        GameApp.drawRect(0, 0, getWorldWidth(), getWorldHeight(), new Color(0, 0, 0, 0.7f));
        GameApp.endShapeRendering();

        GameApp.startSpriteRendering();
        float y = getWorldHeight() / 2 + 120;
        GameApp.drawTextCentered("f1_big", lines[0], getWorldWidth() / 2, y, "white");
        for (int i = 1; i < lines.length; i++) {
            GameApp.drawTextCentered("f1_small", lines[i], getWorldWidth() / 2, y - 70 - (i - 1) * 40, "gray-300");
        }
        GameApp.endSpriteRendering();
    }

    String[] introLines() {
        return new String[]{
                "Floor 1 - Queen of the Night",
                "\"...Flowers? Falling from the ceiling?\"",
                "They only bloom for one night. Catch them before they hit the ground.",
                "Catch " + PASS_TARGET + " to clear the floor. Catch " + ITEM_TARGET + " for something more.",
                "Don't touch the withered grey ones. They take " + WILTED_PENALTY + " flowers away.",
                "A / D or LEFT / RIGHT to move",
                "Press Enter to start"
        };
    }

    String[] resultLines() {
        if (passed && gotItem) {
            return new String[]{
                    "The night is over.",
                    "You caught " + caught + " flowers.",
                    "Something is tangled between the petals... a hair tie?",
                    "Press Enter to continue"
            };
        } else if (passed) {
            return new String[]{
                    "The night is over.",
                    "You caught " + caught + " flowers. The floor is cleared.",
                    "(You feel like you missed something here...)",
                    "Press Enter to continue"
            };
        } else {
            String extra = ghostHelping
                    ? "A small ghost floats closer. \"Let me help this time.\""
                    : "The flowers will bloom again tonight.";
            return new String[]{
                    "The petals faded...",
                    "You caught " + caught + " / " + PASS_TARGET + ".",
                    extra,
                    "Press Enter to try again"
            };
        }
    }

    /** Back to the main hall, Ageha standing in front of Bluma. */
    void backToHall() {
        ExploreScreen.comeBackFrom(MainHallScreen.FROM_MINIGAME);
        GameApp.switchScreen(NEXT_SCREEN);
    }

    @Override
    public void hide() {
        GameApp.disposeFont("f1_big");
        GameApp.disposeFont("f1_mid");
        GameApp.disposeFont("f1_small");
        GameApp.disposeSound("catch");
        // (the music stops when the main hall plays its own again)
        for (String d : AGEHA_DIRECTIONS) {
            for (int n = 1; n <= 3; n++) {
                GameApp.disposeTexture(agehaFrame(d, n));
            }
        }
    }
}