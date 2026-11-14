package nl.saxion.game.yourgamename;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * MINIGAME - "Find the odd one"
 * 9 circles in a 3x3 grid. 8 have the same color, 1 is slightly different. Click the different one.
 * Every round the difference gets smaller (harder).
 *
 *  - Pass criterion : clear ROUNDS_TO_PASS rounds
 *  - Item criterion : with at most MAX_MISTAKES_FOR_ITEM wrong clicks
 *  - No game over: a wrong click just shakes the circle and counts as a mistake.
 *
 * Controls: mouse left click. ENTER to start / continue.
 */
public class OddColorScreen extends ScalableGameScreen {

    // ---------- tuning ----------
    static final int ROUNDS_TO_PASS = 10;
    static final int MAX_MISTAKES_FOR_ITEM = 2;
    static final float START_DIFF = 0.28f;     // brightness difference in round 1 (0..1)
    static final float DIFF_SHRINK = 0.82f;    // each round the difference is multiplied by this
    static final float MIN_DIFF = 0.035f;      // never harder than this
    static final String NEXT_SCREEN = "MainMenuScreen";

    static final float RADIUS = 78;
    static final float SPACING = 200;

    enum State { INTRO, PLAYING, CORRECT, RESULT }
    State state;

    int round;               // 0-based
    int mistakes;
    int oddIndex;            // which of the 9 circles is different
    Color baseColor = new Color();
    Color oddColor = new Color();
    float diff;

    float stateTimer;
    int shakeIndex = -1;     // circle that was clicked wrong
    float shakeTimer;

    public OddColorScreen() {
        super(1280, 720);
    }

    @Override
    public void show() {
        GameApp.addFont("oc_big", "fonts/basic.ttf", 56);
        GameApp.addFont("oc_mid", "fonts/basic.ttf", 32);
        GameApp.addFont("oc_small", "fonts/basic.ttf", 24);
        GameApp.enableTransparency();
        state = State.INTRO;
        round = 0;
        mistakes = 0;
    }

    // ======================= GAME LOGIC =======================

    void newRound() {
        diff = Math.max(MIN_DIFF, START_DIFF * (float) Math.pow(DIFF_SHRINK, round));
        oddIndex = GameApp.randomInt(0, 9);

        float hue = GameApp.random(0, 360);
        float sat = GameApp.random(0.45f, 0.8f);
        float val = GameApp.random(0.45f, 0.75f);
        baseColor.fromHsv(hue, sat, val);
        baseColor.a = 1;

        // odd circle: a bit brighter or a bit darker (whichever stays inside 0..1)
        float oddVal = (val + diff <= 1f && GameApp.random(0, 1) < 0.5f) ? val + diff : val - diff;
        oddColor.fromHsv(hue, sat, oddVal);
        oddColor.a = 1;

        shakeIndex = -1;
        state = State.PLAYING;
    }

    float circleX(int i) { return getWorldWidth() / 2 + (i % 3 - 1) * SPACING; }
    float circleY(int i) { return getWorldHeight() / 2 - 30 + (1 - i / 3) * SPACING; }

    @Override
    public void render(float delta) {
        super.render(delta);
        stateTimer += delta;
        if (shakeTimer > 0) shakeTimer -= delta;

        switch (state) {
            case INTRO:
                if (GameApp.isKeyJustPressed(Input.Keys.ENTER)) {
                    round = 0;
                    mistakes = 0;
                    newRound();
                }
                break;

            case PLAYING:
                if (GameApp.isButtonJustPressed(Input.Buttons.LEFT)) {
                    handleClick(getMouseX(), getMouseY());
                }
                break;

            case CORRECT:
                // short pause so the player sees which one was right
                if (stateTimer > 0.45f) {
                    round++;
                    if (round >= ROUNDS_TO_PASS) {
                        state = State.RESULT;
                        stateTimer = 0;
                    } else {
                        newRound();
                    }
                }
                break;

            case RESULT:
                if (GameApp.isKeyJustPressed(Input.Keys.ENTER)) {
                    // TODO: save (mistakes <= MAX_MISTAKES_FOR_ITEM) somewhere for the memory item
                    GameApp.switchScreen(NEXT_SCREEN);
                    return;
                }
                break;
        }

        draw();
    }

    void handleClick(float mx, float my) {
        for (int i = 0; i < 9; i++) {
            if (GameApp.pointInCircle(mx, my, circleX(i), circleY(i), RADIUS)) {
                if (i == oddIndex) {
                    state = State.CORRECT;
                    stateTimer = 0;
                    // GameApp.playSound("correct");   // add a sound here later
                } else {
                    mistakes++;
                    shakeIndex = i;
                    shakeTimer = 0.35f;
                    // GameApp.playSound("wrong");
                }
                return;
            }
        }
    }

    // ======================= DRAW =======================

    void draw() {
        GameApp.clearScreen(18, 18, 22);

        if (state == State.PLAYING || state == State.CORRECT) {
            GameApp.startShapeRenderingFilled();
            for (int i = 0; i < 9; i++) {
                float x = circleX(i), y = circleY(i);
                if (i == shakeIndex && shakeTimer > 0) {
                    x += (float) Math.sin(shakeTimer * 60) * 10 * (shakeTimer / 0.35f);
                }
                // correct answer: white ring around it
                if (state == State.CORRECT && i == oddIndex) {
                    float grow = Math.min(1f, stateTimer / 0.2f);
                    GameApp.drawCircle(x, y, RADIUS + 8 + 6 * grow, 64, new Color(1, 1, 1, 0.9f));
                }
                GameApp.drawCircle(x, y, RADIUS, 64, i == oddIndex ? oddColor : baseColor);
            }
            GameApp.endShapeRendering();

            GameApp.startSpriteRendering();
            GameApp.drawText("oc_mid", "Round " + (round + 1) + " / " + ROUNDS_TO_PASS, 40, 690, "white");
            GameApp.drawText("oc_small", "Mistakes: " + mistakes, 40, 645,
                    mistakes <= MAX_MISTAKES_FOR_ITEM ? "gray-300" : "gray-500");
            GameApp.drawTextHorizontallyCentered("oc_small", "Find the one that is different", getWorldWidth() / 2, 40, "gray-400");
            GameApp.endSpriteRendering();
        }

        if (state == State.INTRO) {
            drawCenteredLines(new String[]{
                    "Find the odd one",
                    "One circle is a slightly different color. Click it.",
                    "It gets harder every round. Clear " + ROUNDS_TO_PASS + " rounds.",
                    "Press ENTER to start"
            });
        }

        if (state == State.RESULT) {
            boolean item = mistakes <= MAX_MISTAKES_FOR_ITEM;
            drawCenteredLines(new String[]{
                    "Cleared!",
                    "Mistakes: " + mistakes,
                    item ? "You noticed every little difference... something is waiting for you."
                         : "(You feel like you missed something here...)",
                    "Press ENTER to continue"
            });
        }
    }

    void drawCenteredLines(String[] lines) {
        GameApp.startSpriteRendering();
        float y = getWorldHeight() / 2 + 100;
        GameApp.drawTextCentered("oc_big", lines[0], getWorldWidth() / 2, y, "white");
        for (int i = 1; i < lines.length; i++) {
            GameApp.drawTextCentered("oc_small", lines[i], getWorldWidth() / 2, y - 80 - (i - 1) * 42, "gray-300");
        }
        GameApp.endSpriteRendering();
    }

    @Override
    public void hide() {
        GameApp.disposeFont("oc_big");
        GameApp.disposeFont("oc_mid");
        GameApp.disposeFont("oc_small");
    }
}
