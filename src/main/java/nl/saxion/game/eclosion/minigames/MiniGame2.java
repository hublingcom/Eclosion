package nl.saxion.game.eclosion.minigames;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import nl.saxion.game.eclosion.audio.Sfx;
import nl.saxion.game.eclosion.explore.ExploreScreen;
import nl.saxion.game.eclosion.rooms.MainHallScreen;
import nl.saxion.game.eclosion.ui.ClickEffect;
import nl.saxion.game.eclosion.ui.PixelFont;
import nl.saxion.game.eclosion.ui.ScreenFader;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * MINIGAME 2 - "Find the odd colour"
 * 9 circles on the floor of the library; one has a slightly different colour (a bit lighter or darker,
 * and the difference gets smaller every round).
 * Walk Ageha onto it and stand still on it for 1 second to choose it (a white ring fills up).
 * 5 rounds, 7 seconds per round. Choosing a wrong circle or running out of time = game over.
 *
 * Controls: WASD / arrow keys, or click somewhere to walk there. Any key / click to start,
 * Enter / Space / E / click (or Y) to go back at the end, Esc = back.
 *
 * Made by Giang (branch Giang, yourgamename.MiniGame2). Changed for this version: started from
 * Bluma in the main hall and goes back there; Ageha walks onto the circle instead of clicking it;
 * the game's pixel font with centred text, the same keys as the rest of the game, a small round/time
 * counter, fade-in and the click circle. The circles, colours, rounds and timer are Giang's.
 */
public class MiniGame2 extends ScalableGameScreen {
    private static final int RULES = 0, PLAYING = 1, WON = 2, LOST = 3;

    int circleX[] = {562, 640, 718, 562, 640, 718, 562, 640, 718};
    int circleY[] = {296, 296, 296, 219, 219, 219, 142, 142, 142};
    int radius = 32;
    int oddCircle;
    int r;
    int g;
    int b;
    // How different the odd circle is (0..255 per colour channel). Giang had 40 every round, which was
    // too easy: now it starts smaller and gets smaller every round, and the odd one can be lighter OR darker.
    static final int DIFF_START = 26;
    static final int DIFF_PER_ROUND = 4;        // round 1: 26, round 2: 22, ... round 5: 10
    int diff = DIFF_START;
    int state = RULES;
    private float countdown;
    /** Seconds per round: Giang had 5 for clicking; walking + standing 1 s needs a bit more. */
    private static final float ROUND_TIME = 7f;
    int maxRound = 5;
    int round;

    // ---------- Ageha walking on the library floor ----------
    private static final float AGEHA_H = 96;
    private static final float AGEHA_W = AGEHA_H * 243f / 330f;     // the game's walk frames are 243 x 330
    private static final float SPEED = 250;                           // world units per second
    private static final float STEP_LENGTH = 22;                      // distance per animation frame
    private static final int[] WALK_CYCLE = {1, 2, 3, 2};            // step, stand, step, stand
    private static final String[] DIRECTIONS = {"down", "up", "left", "right"};
    // where his feet may go: the floor inside the frame, in front of the desk
    private static final float FLOOR_LEFT = 280, FLOOR_RIGHT = 1000, FLOOR_BOTTOM = 100, FLOOR_TOP = 380;
    private static final float START_X = 420, START_Y = 210;         // feet, left of the circles (so none is hidden)

    private float feetX, feetY;                                      // middle of his feet
    private String facing = "up";
    private float walkDistance = 0;
    private int lastStepIndex = -1;
    private boolean walking = false;
    private boolean hasTarget = false;
    private float targetX, targetY;

    // ---------- choosing by standing on a circle ----------
    private static final float CHOOSE_TIME = 1f;                     // stand still this long on a circle
    private int standingOn = -1;                                     // circle under his feet, or -1
    private float standTime = 0;
    private int lockedCircle = -1;   // the circle just chosen: it only counts again after he steps off

    private final ScreenFader fader = new ScreenFader();

    public MiniGame2() {
        super(1280, 720);
    }

    @Override
    public void show() {
        GameApp.addTexture("library_intro", "textures/Background/library_intro.png");
        GameApp.addTexture("library_game", "textures/Background/library_game.png");
        GameApp.addFont("mg2_big", PixelFont.make(44));
        GameApp.addFont("mg2_text", PixelFont.make(21));   // the rules must fit on the small card
        GameApp.addColor("black", Color.BLACK);
        for (String d : DIRECTIONS) {
            for (int n = 1; n <= 3; n++) {
                GameApp.addTexture(frame(d, n), "textures/sprites/" + frame(d, n) + ".png");
            }
        }
        state = RULES;      // start at the rules again every time you come in
        newRound();
        placeAgeha();
        fader.fadeIn();
    }

    private static String frame(String direction, int number) {
        return "ageha_walk_" + direction + "_" + number;
    }

    private void placeAgeha() {
        feetX = START_X;
        feetY = START_Y;
        facing = "right";
        walking = false;
        hasTarget = false;
        standingOn = -1;
        standTime = 0;
        lockedCircle = -1;
    }

    /** Back to the main hall, Ageha standing next to Bluma. */
    private void backToHall() {
        ExploreScreen.comeBackFrom(MainHallScreen.FROM_MINIGAME);
        GameApp.switchScreen("MainHallScreen");
    }

    /** Like everywhere in the game: Enter / Space / E / a click goes on (Y still works too). */
    private boolean confirmPressed() {
        return GameApp.isKeyJustPressed(Input.Keys.ENTER) || GameApp.isKeyJustPressed(Input.Keys.SPACE)
                || GameApp.isKeyJustPressed(Input.Keys.E) || GameApp.isKeyJustPressed(Input.Keys.Y)
                || GameApp.isButtonJustPressed(Input.Buttons.LEFT);
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        fader.update(delta);
        GameApp.clearScreen("black");
        if (!fader.busy()) {
            update(delta);
        }
        draw();
        fader.render(getWorldWidth(), getWorldHeight());
        ClickEffect.updateAndDraw(getMouseX(), getMouseY());
    }

    private void update(float delta) {
        if (GameApp.isKeyJustPressed(Input.Keys.ESCAPE)) {
            backToHall();
            return;
        }
        if (state == RULES) {
            // any key (or a click) starts; Esc is handled above and goes back to the hall
            if (Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY) || GameApp.isButtonJustPressed(Input.Buttons.LEFT)) {
                state = PLAYING;
                round = 0;
                countdown = ROUND_TIME;
                newRound();
                placeAgeha();
            }
        } else if (state == PLAYING) {
            countdown -= delta;
            if (countdown < 0) {
                state = LOST;
                return;
            }
            moveAgeha(delta);
            chooseByStanding(delta);
        } else if (confirmPressed()) {     // WON or LOST
            backToHall();
        }
    }

    // ---------- walking ----------

    private void moveAgeha(float delta) {
        float dx = 0, dy = 0;
        if (GameApp.isKeyPressed(Input.Keys.LEFT) || GameApp.isKeyPressed(Input.Keys.A)) dx -= 1;
        if (GameApp.isKeyPressed(Input.Keys.RIGHT) || GameApp.isKeyPressed(Input.Keys.D)) dx += 1;
        if (GameApp.isKeyPressed(Input.Keys.UP) || GameApp.isKeyPressed(Input.Keys.W)) dy += 1;
        if (GameApp.isKeyPressed(Input.Keys.DOWN) || GameApp.isKeyPressed(Input.Keys.S)) dy -= 1;

        if (GameApp.isButtonJustPressed(Input.Buttons.LEFT)) {
            // click: walk to that spot (on the floor)
            targetX = clamp(getMouseX(), FLOOR_LEFT, FLOOR_RIGHT);
            targetY = clamp(getMouseY(), FLOOR_BOTTOM, FLOOR_TOP);
            hasTarget = true;
        }
        if (dx != 0 || dy != 0) {
            hasTarget = false;                       // the keyboard takes over from a click
        } else if (hasTarget) {
            dx = targetX - feetX;
            dy = targetY - feetY;
            if (Math.abs(dx) < 2 && Math.abs(dy) < 2) {
                hasTarget = false;
                dx = 0;
                dy = 0;
            }
        }

        walking = dx != 0 || dy != 0;
        if (walking) {
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            float step = Math.min(SPEED * delta, hasTarget ? length : Float.MAX_VALUE);
            feetX = clamp(feetX + dx / length * step, FLOOR_LEFT, FLOOR_RIGHT);
            feetY = clamp(feetY + dy / length * step, FLOOR_BOTTOM, FLOOR_TOP);
            facing = Math.abs(dx) > Math.abs(dy) ? (dx < 0 ? "left" : "right") : (dy > 0 ? "up" : "down");
            walkDistance += step;
            int stepIndex = (int) (walkDistance / STEP_LENGTH);
            if (stepIndex != lastStepIndex) {
                lastStepIndex = stepIndex;
                if (WALK_CYCLE[stepIndex % WALK_CYCLE.length] != 2) {
                    Sfx.step();
                }
            }
        } else {
            walkDistance = 0;
            lastStepIndex = -1;
        }
    }

    // ---------- choosing a circle ----------

    /** Standing still on a circle for CHOOSE_TIME chooses it. */
    private void chooseByStanding(float delta) {
        int under = circleUnderFeet();
        if (under != lockedCircle) {
            lockedCircle = -1;                       // he stepped off the circle he chose last round
        }
        if (under < 0 || under == lockedCircle || walking) {
            standingOn = under == lockedCircle ? -1 : under;
            standTime = 0;                           // walking (or nothing under him): start over
            return;
        }
        if (under != standingOn) {
            standingOn = under;
            standTime = 0;
        }
        standTime += delta;
        if (standTime >= CHOOSE_TIME) {
            choose(under);
        }
    }

    private void choose(int circle) {
        standingOn = -1;
        standTime = 0;
        if (circle == oddCircle) {
            Sfx.play(Sfx.CONFIRM);
            round++;
            countdown = ROUND_TIME;
            lockedCircle = circle;                   // the new round doesn't count this circle until he steps off
            if (round >= maxRound) {
                state = WON;
            } else {
                newRound();
            }
        } else {
            Sfx.play(Sfx.KEY_WRONG);
            state = LOST;
        }
    }

    private int circleUnderFeet() {
        for (int i = 0; i < 9; i++) {
            if (GameApp.pointInCircle(feetX, feetY, circleX[i], circleY[i], radius)) return i;
        }
        return -1;
    }

    // ---------- drawing ----------

    private void draw() {
        float centerX = getWorldWidth() / 2;
        GameApp.startSpriteRendering();
        GameApp.drawTexture(state == PLAYING ? "library_game" : "library_intro", 0, 0, getWorldWidth(), getWorldHeight());
        if (state == RULES) {
            GameApp.drawTextCentered("mg2_big", "Rule", centerX, 470, "black");
            GameApp.drawTextCentered("mg2_text", "Find the circle with a different color", centerX, 405, "black");
            GameApp.drawTextCentered("mg2_text", "Walk onto it and stand still for 1 second", centerX, 370, "black");
            GameApp.drawTextCentered("mg2_text", "5 rounds - " + (int) ROUND_TIME + " seconds per round", centerX, 335, "black");
            GameApp.drawTextCentered("mg2_text", "Press any key to start", centerX, 255, "black");
        } else if (state == WON) {
            GameApp.drawTextCentered("mg2_big", "You Win", centerX, 400, "black");
            GameApp.drawTextCentered("mg2_text", "Press Enter to go back", centerX, 300, "black");
        } else if (state == LOST) {
            GameApp.drawTextCentered("mg2_big", "Game Over", centerX, 400, "black");
            GameApp.drawTextCentered("mg2_text", "Press Enter to go back", centerX, 300, "black");
        } else {
            GameApp.drawText("mg2_text", "Round " + (round + 1) + " / " + maxRound, 40, 690, "white");
            GameApp.drawText("mg2_text", "Time: " + (int) Math.ceil(Math.max(0, countdown)), 1100, 690, "white");
        }
        GameApp.endSpriteRendering();

        if (state != PLAYING) {
            return;
        }
        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        // the ring that fills while Ageha stands on a circle (drawn under the circle, so it shows as a ring)
        if (standingOn >= 0 && standTime > 0) {
            float degrees = 360f * Math.min(1f, standTime / CHOOSE_TIME);
            GameApp.getShapeRenderer().setColor(1f, 1f, 1f, 0.9f);
            // starts at the bottom and goes round clockwise (the top is behind Ageha's body)
            GameApp.getShapeRenderer().arc(circleX[standingOn], circleY[standingOn], radius + 7, 270 - degrees, degrees, 48);
        }
        for (int i = 0; i < 9; i++) {
            GameApp.drawCircle(circleX[i], circleY[i], radius, i == oddCircle ? "odd" : "normal");
        }
        // a soft shadow under his feet
        GameApp.drawEllipseCentered(feetX, feetY + 2, AGEHA_W * 0.7f, 12, new Color(0, 0, 0, 0.35f));
        GameApp.endShapeRendering();
        GameApp.disableTransparency();

        int frameNumber = 2;                         // standing
        if (walking) {
            frameNumber = WALK_CYCLE[(int) (walkDistance / STEP_LENGTH) % WALK_CYCLE.length];
        }
        GameApp.startSpriteRendering();
        GameApp.drawTexture(frame(facing, frameNumber), Math.round(feetX - AGEHA_W / 2), Math.round(feetY - 4),
                AGEHA_W, AGEHA_H);
        GameApp.endSpriteRendering();
    }

    public void newRound() {
        oddCircle = GameApp.randomInt(0, 9);
        r = GameApp.randomInt(40, 200);
        g = GameApp.randomInt(40, 200);
        b = GameApp.randomInt(40, 200);
        diff = Math.max(8, DIFF_START - round * DIFF_PER_ROUND);
        int d = GameApp.randomInt(0, 2) == 0 ? diff : -diff;     // lighter or darker
        GameApp.addColor("odd", r + d, g + d, b + d);
        GameApp.addColor("normal", r, g, b);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void hide() {
        GameApp.disposeFont("mg2_big");
        GameApp.disposeFont("mg2_text");
        GameApp.disposeTexture("library_game");
        GameApp.disposeTexture("library_intro");
        for (String d : DIRECTIONS) {
            for (int n = 1; n <= 3; n++) {
                GameApp.disposeTexture(frame(d, n));
            }
        }
    }
}
