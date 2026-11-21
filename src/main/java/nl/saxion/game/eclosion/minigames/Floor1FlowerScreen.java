package nl.saxion.game.eclosion.minigames;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import java.util.ArrayList;
import nl.saxion.game.eclosion.audio.Bgm;
import nl.saxion.game.eclosion.audio.Sfx;
import nl.saxion.game.eclosion.explore.ExploreScreen;
import nl.saxion.game.eclosion.items.Items;
import nl.saxion.game.eclosion.ui.ItemPopup;
import nl.saxion.game.eclosion.rooms.MainHallScreen;
import nl.saxion.game.eclosion.ui.ClickEffect;
import nl.saxion.game.eclosion.ui.DialogueBox;
import nl.saxion.game.eclosion.ui.DialogueLine;
import nl.saxion.game.eclosion.ui.PixelFont;
import nl.saxion.game.eclosion.ui.ScreenFader;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * FLOOR 1 MINIGAME - "Queen of the Night"
 *
 * Queen of the Night flowers fall from the dark sky. Move Ageha left/right and catch them.
 *  - Pass criterion  : catch PASS_TARGET flowers before the night ends  -> next floor
 *  - At PASS_TARGET the game stops and asks (dialogue box): stop here, or keep going for the item?
 *      stop        -> floor cleared, no item (safe)
 *      keep going  -> GREEDY phase: a bit more time, but faster flowers and more withered ones.
 *                     Reach ITEM_TARGET -> memory item + photo piece.
 *                     End the night under PASS_TARGET again -> the night is lost, try again (the risk).
 *  - No harsh game over: when the night ends you just try again.
 *    After HELP_AFTER_FAILS failures a little ghost comes to help (wider catch, slower flowers).
 *
 * Controls: A/D or LEFT/RIGHT to move, Enter / Space / E / click for the text, Esc = back.
 * The intro, the choice and the result use the game's DialogueBox (Ageha's inner voice).
 *
 * Made by Dawon (branch Dawon, eclosion.floor1.Floor1FlowerScreen). Changed for this version:
 * started from Bluma in the main hall and goes back there, the game's pixel font, Ageha walk frames,
 * sounds and music, fade-in, click circle, and the same keys as the rest of the game.
 */
public class Floor1FlowerScreen extends ScalableGameScreen {

    // ---------- tuning (team can change these numbers in Sprint 0) ----------
    static final int PASS_TARGET = 15;
    static final int ITEM_TARGET = 25;
    static final float NIGHT_LENGTH = 40f;       // seconds
    static final int HELP_AFTER_FAILS = 2;

    static final float AGEHA_SPEED = 400f;
    static final float AGEHA_HEIGHT = 160f;
    static final float AGEHA_WIDTH = AGEHA_HEIGHT * 243f / 330f;   // the game's Ageha walk frames are 243 x 330
    static final float AGEHA_Y = 40f;
    static final float CATCH_WIDTH = 100f;       // width of the catch zone (a little wider than the sprite)
    static final float CATCH_HEIGHT = 110f;      // upper body + head

    static final float SPAWN_START = 1.0f;       // seconds between flowers at start of the night
    static final float SPAWN_END = 0.55f;        // ...and at the end
    static final float FALL_MIN = 170f;
    static final float FALL_MAX = 260f;
    static final float FALL_BONUS_END = 140f;    // extra speed reached at the end of the night

    // Withered flowers (the twist): catching one takes points away
    static final float WILTED_CHANCE_START = 0.25f;  // chance a new flower is withered at the start
    static final float WILTED_CHANCE_END = 0.40f;    // ...and at the end of the night
    static final float WILTED_CHANCE_GHOST = 0.12f;  // lower when the ghost is helping
    static final int WILTED_PENALTY = 2;

    // Greedy phase (chose "keep going" at PASS_TARGET)
    static final float GREEDY_BONUS_TIME = 12f;      // seconds added to the night
    static final float GREEDY_EXTRA_WILTED = 0.12f;  // more withered flowers
    static final float GREEDY_SPEED = 1.15f;         // flowers fall faster

    // Started from Bluma's options in the main hall, so it goes back there (and then it is night)
    static final String NEXT_SCREEN = "MainHallScreen";

    // ---------- state ----------
    // TALK = the dialogue box is open (intro, the choice at 15, the result); the night is frozen
    // ITEM = the "memory item found" panel is shown (hair tie)
    enum State { TALK, PLAYING, ITEM }
    State state = State.TALK;

    final DialogueBox dialogue = new DialogueBox();
    final ItemPopup itemPopup = new ItemPopup();
    Runnable afterTalk = null;    // what happens when the dialogue box closes (null = nothing)
    boolean askedAtPass = false;  // the choice at PASS_TARGET was already shown this night
    boolean greedy = false;       // chose to keep going for the item

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
        DialogueBox.loadAssets();
        dialogue.setPortraits(null, null);   // Ageha's inner voice: no portraits
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
        failCount = 0;
        ghostHelping = false;
        resetRound();
        startIntro();
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
        askedAtPass = false;
        greedy = false;
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

        if (state == State.TALK) {
            dialogue.update(delta, getMouseX(), getMouseY());
            if (!dialogue.isActive() && afterTalk != null) {
                Runnable next = afterTalk;
                afterTalk = null;
                next.run();
                if (switchedScreen) {
                    return;
                }
            }
        } else if (state == State.PLAYING) {
            updatePlaying(delta);
        } else if (state == State.ITEM) {
            itemPopup.update(delta);
            if (!itemPopup.isOpen()) {
                backToHallAfterTalk();      // panel closed: leave
                return;
            }
        }

        draw();
        if (state == State.TALK) {
            dialogue.render(getWorldWidth(), getWorldHeight());
        } else if (state == State.ITEM) {
            itemPopup.render(getWorldWidth(), getWorldHeight());
        }
        ClickEffect.updateAndDraw(getMouseX(), getMouseY());
    }

    boolean switchedScreen = false;

    // ---------- talking (intro / choice / result) ----------

    static DialogueLine thought(String text) {
        return DialogueLine.narration(text);
    }

    /** Opens the dialogue box; when it closes, then runs (null = nothing). */
    void talk(Runnable then, DialogueLine... lines) {
        dialogue.start(lines);
        afterTalk = then;
        state = State.TALK;
    }

    void startIntro() {
        switchedScreen = false;
        explainRules();
    }

    /** The rules, in Ageha's inner voice (no portraits). */
    void explainRules() {
        dialogue.setPortraits(null, null);
        talk(this::startNight,
                thought("...Flowers? Falling from the ceiling?"),
                thought("Queen of the Night. They only bloom for one night."),
                thought("Catch " + PASS_TARGET + " white ones before they hit the ground. "
                        + "The grey, withered ones take " + WILTED_PENALTY + " away."),
                thought("[A / D or LEFT / RIGHT to move]"));
    }

    void startNight() {
        resetRound();
        state = State.PLAYING;
    }

    /** Reached PASS_TARGET: the night freezes and Ageha has to choose. */
    void askAtPass() {
        askedAtPass = true;
        dialogue.start(
                thought(PASS_TARGET + " flowers. That's enough to get through this floor."),
                thought("...But something glints between the petals that are still falling."),
                thought("If I keep going and end the night under " + PASS_TARGET + ", it's all for nothing."));
        dialogue.ask(this::passChoice, "Stop here", "Keep going (" + ITEM_TARGET + ")");
        afterTalk = null;
        state = State.TALK;
    }

    void passChoice(int choice) {
        if (choice == 0) {
            passed = true;
            gotItem = false;
            showResult();
        } else {
            greedy = true;
            timeLeft += GREEDY_BONUS_TIME;
            for (Flower f : flowers) {
                f.speed *= GREEDY_SPEED;
            }
            state = State.PLAYING;
        }
    }

    /** The night is over (time, stopped at the choice, or reached the item). */
    void endNight() {
        timeLeft = Math.max(0, timeLeft);
        gotItem = greedy && caught >= ITEM_TARGET;
        if (gotItem) {
            Items.give(Items.HAIR_TIE);     // shown in the collection (I) from now on
        }
        passed = caught >= PASS_TARGET;
        if (!passed) {
            failCount++;
            if (failCount >= HELP_AFTER_FAILS) ghostHelping = true;
        }
        showResult();
    }

    void showResult() {
        dialogue.setPortraits(null, null);
        if (gotItem) {
            talk(this::showItem,
                    thought("The last flower lands in my hands."),
                    thought("Something is tangled between the petals... a hair tie?"),
                    thought("...Why does it feel familiar?"));
        } else if (passed && greedy) {
            talk(this::backToHallAfterTalk,
                    thought("The night ends before I can reach it."),
                    thought("Still, " + caught + " flowers. The floor is cleared."),
                    thought("(Whatever was glinting is gone.)"));
        } else if (passed) {
            talk(this::backToHallAfterTalk,
                    thought(caught + " flowers. The floor is cleared."),
                    thought("(I feel like I left something behind here...)"));
        } else if (greedy) {
            talk(this::startNight,
                    thought("I reached for too much. The petals faded in my hands."),
                    thought(caught + " / " + PASS_TARGET + "... The night is lost."),
                    ghostHelping ? thought("A small ghost floats closer. \"Let me help this time.\"")
                            : thought("The flowers will bloom again tonight."));
        } else {
            talk(this::startNight,
                    thought("The petals faded..."),
                    thought(caught + " / " + PASS_TARGET + "."),
                    ghostHelping ? thought("A small ghost floats closer. \"Let me help this time.\"")
                            : thought("The flowers will bloom again tonight."));
        }
    }

    /** The hair tie panel; when it closes, render() leaves the minigame. */
    void showItem() {
        itemPopup.open(Items.HAIR_TIE);
        state = State.ITEM;
    }

    void backToHallAfterTalk() {
        // TODO: save gotItem somewhere global (photo piece) before switching
        switchedScreen = true;
        backToHall();
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
        // 0 at start -> 1 at end of night (stays 1 in the greedy phase, which can be longer than the night)
        float progress = greedy ? 1f : GameApp.clamp(1f - timeLeft / NIGHT_LENGTH, 0f, 1f);

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

        // --- reached the pass target for the first time: stop and choose ---
        if (!askedAtPass && caught >= PASS_TARGET) {
            askAtPass();
            return;
        }
        // --- end of night (or the item already reached in the greedy phase) ---
        if (timeLeft <= 0 || (greedy && caught >= ITEM_TARGET)) {
            endNight();
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
        if (greedy) f.speed *= GREEDY_SPEED;
        f.swayAmount = GameApp.random(10, 40);
        f.swayTime = GameApp.random(0, 6.28f);
        f.rotation = GameApp.random(0, 360);
        float wiltChance = ghostHelping
                ? WILTED_CHANCE_GHOST
                : WILTED_CHANCE_START + (WILTED_CHANCE_END - WILTED_CHANCE_START) * progress;
        if (greedy) wiltChance += GREEDY_EXTRA_WILTED;
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
        if (ghostHelping) drawGhost();
        GameApp.endShapeRendering();

        GameApp.startSpriteRendering();
        drawAgeha();
        drawHud();
        GameApp.endSpriteRendering();

        if (state == State.TALK || state == State.ITEM) {
            // dim the night a little behind the dialogue box / the item panel
            GameApp.enableTransparency();
            GameApp.startShapeRenderingFilled();
            GameApp.drawRect(0, 0, getWorldWidth(), getWorldHeight(), new Color(0, 0, 0, 0.45f));
            GameApp.endShapeRendering();
        }
    }

    void drawBackground() {
        // twinkling stars
        for (int i = 0; i < starX.length; i++) {
            float b = 0.4f + 0.3f * (float) Math.sin(totalTime * 1.5f + starPhase[i]);
            GameApp.drawCircle(starX[i], starY[i], 1.6f, new Color(b, b, b, 1));
        }
        // moon
        Color moon = greedy ? new Color(0.80f, 0.55f, 0.58f, 1) : new Color(0.85f, 0.85f, 0.85f, 1);
        GameApp.drawCircle(1100, 610, 55, moon);
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
        if (greedy) {
            GameApp.drawText("f1_mid", "Flowers: " + caught + " / " + ITEM_TARGET, 170, 690, "white");
            String warn = caught < PASS_TARGET ? "Under " + PASS_TARGET + "! The night will be lost."
                    : "Don't fall under " + PASS_TARGET + ".";
            GameApp.drawText("f1_small", warn, 170, 645, caught < PASS_TARGET ? "red-400" : "gray-400");
        } else {
            GameApp.drawText("f1_mid", "Flowers: " + caught + " / " + PASS_TARGET, 170, 690, "white");
        }
        GameApp.drawText("f1_mid", "Night: " + (int) Math.ceil(timeLeft) + "s", 900, 690, "white");
        if (ghostHelping) {
            GameApp.drawText("f1_small", "A little ghost is helping you.", 170, 610, "gray-300");
        }
        if (wiltFlash > 0) {
            GameApp.drawText("f1_mid", "-" + WILTED_PENALTY, wiltX - 15, wiltY + 60 + (0.6f - wiltFlash) * 60, "red-400");
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
        DialogueBox.disposeAssets();
        // (the music stops when the main hall plays its own again)
        for (String d : AGEHA_DIRECTIONS) {
            for (int n = 1; n <= 3; n++) {
                GameApp.disposeTexture(agehaFrame(d, n));
            }
        }
    }
}