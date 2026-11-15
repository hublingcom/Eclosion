package nl.saxion.game.eclosion;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * EVENING ROOM - after the floor is cleared, Ageha is back in his room at night.
 * Only the desk lamp is on. Walk to the bed and press E to sleep (-> night scene -> End of Demo).
 *
 * Controls: WASD / arrow keys, or click to walk there. E near the bed = sleep.
 *
 * Images: images/room.png (1600x902, with black margin), textures/Ageha_move/ageha_*.png
 * Music:  audio/room_bgm.ogg
 *
 * Positions are in IMAGE pixels from the top-left of room.png (same numbers as the old RoomScreen).
 */
public class EveningRoomScreen extends ScalableGameScreen {

    static final float WORLD_W = 1280, WORLD_H = 720;
    static final float IMG_W = 1600, IMG_H = 902;
    final float scale = WORLD_H / IMG_H;
    final float offsetX = (WORLD_W - IMG_W * scale) / 2;

    // player starts just inside the door (bottom middle)
    static final float START_X = 860, START_Y = 470;
    float px, py;
    // same on-screen size as in Chidera's room (80 x 100), converted to image pixels
    static final float PH = 100f * IMG_H / WORLD_H;
    static final float PW = 80f * IMG_H / WORLD_H;
    static final float SPEED = 4.5f;

    boolean hasTarget = false;
    float targetX, targetY;

    static final String[] DIRECTIONS = { "down", "up", "left", "right" };
    static final String[] FRAMES = { "idle", "a", "b" };
    static final int[] WALK_CYCLE = { 1, 0, 2, 0 };
    static final float FRAME_TIME = 0.14f;
    String facing = "up";
    boolean walking = false;
    float walkTime = 0;
    float time = 0;
    float fadeOut = -1;      // >= 0 while fading out to the sleep scene

    Rectangle floor      = new Rectangle(376, 282, 859, 394);
    Rectangle bed        = new Rectangle(405, 237, 134, 295);
    Rectangle nightstand = new Rectangle(553, 292, 76, 72);
    Rectangle desk       = new Rectangle(811, 262, 191, 105);
    Rectangle chair      = new Rectangle(873, 317, 53, 90);
    Rectangle wardrobe   = new Rectangle(1051, 155, 131, 150);
    Rectangle cabinet    = new Rectangle(1187, 375, 48, 112);
    Rectangle[] solids = { bed, nightstand, desk, chair, wardrobe, cabinet };

    public EveningRoomScreen() {
        super(1280, 720);
    }

    @Override
    public void show() {
        GameApp.addTexture("ev_room", "images/room.png");
        for (String d : DIRECTIONS) {
            for (String f : FRAMES) {
                GameApp.addTexture("ev_" + key(d, f), "textures/Ageha_move/" + key(d, f) + ".png");
            }
        }
        GameApp.addFont("ev_font", "fonts/basic.ttf", 28);
        GameApp.addMusic("ev_bgm", "audio/room_bgm.ogg");
        GameApp.playMusic("ev_bgm", true, 0.25f);
        px = START_X;
        py = START_Y;
        facing = "up";
        hasTarget = false;
        time = 0;
        fadeOut = -1;
    }

    String key(String direction, String frame) {
        return "ageha_" + direction + "_" + frame;
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        time += delta;

        if (fadeOut >= 0) {
            fadeOut += delta;
            if (fadeOut > 0.8f) {
                DayTransitionScreen.prepareDemoEnd();
                GameApp.switchScreen("DayTransitionScreen");
                return;
            }
        } else {
            handleInput(delta);
        }

        // ---------- room + Ageha ----------
        GameApp.clearScreen("black");
        GameApp.startSpriteRendering();
        GameApp.drawTexture("ev_room", offsetX, 0, IMG_W * scale, WORLD_H);
        String frame = "idle";
        if (walking) frame = FRAMES[WALK_CYCLE[(int) (walkTime / FRAME_TIME) % WALK_CYCLE.length]];
        GameApp.drawTexture("ev_" + key(facing, frame), wx(px), WORLD_H - (py + PH) * scale, PW * scale, PH * scale);
        GameApp.endSpriteRendering();

        // ---------- night: dark blue, only the desk lamp is on ----------
        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        GameApp.drawRect(0, 0, WORLD_W, WORLD_H, new Color(0.02f, 0.03f, 0.08f, 0.6f));
        float flicker = 0.9f + 0.1f * (float) Math.sin(time * 7) * (float) Math.sin(time * 3.3f);
        for (int i = 0; i < 6; i++) {
            GameApp.drawCircle(wx(975), wy(255), 40 + i * 35, new Color(1f, 0.82f, 0.5f, 0.035f * flicker));
        }
        // soft glow over the bed so the player knows where to go
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 2.5f);
        if (fadeOut < 0) {
            GameApp.drawRect(wx(405), wy(237 + 295), 134 * scale, 295 * scale, new Color(0.8f, 0.85f, 1f, 0.05f + 0.05f * pulse));
        }
        if (fadeOut >= 0) {
            GameApp.drawRect(0, 0, WORLD_W, WORLD_H, new Color(0, 0, 0, Math.min(1f, fadeOut / 0.8f)));
        }
        GameApp.endShapeRendering();

        // ---------- hint ----------
        if (fadeOut < 0) {
            GameApp.startSpriteRendering();
            if (nearBed()) {
                GameApp.drawTextCentered("ev_font", "[E]  Sleep", WORLD_W / 2, 50, new Color(0.95f, 0.95f, 0.95f, 0.6f + 0.4f * pulse));
            } else {
                GameApp.drawTextCentered("ev_font", "It's late. Go to bed.", WORLD_W / 2, 50, new Color(0.8f, 0.8f, 0.85f, 0.75f));
            }
            GameApp.endSpriteRendering();
        }
    }

    float wx(float ix) { return offsetX + ix * scale; }
    float wy(float iy) { return WORLD_H - iy * scale; }

    boolean nearBed() {
        Rectangle me = new Rectangle(px, py, PW, PH);
        return me.overlaps(grow(bed, 50));
    }

    // ---------- movement (WASD / arrows / click) ----------
    void handleInput(float delta) {
        float dx = 0, dy = 0;
        if (GameApp.isKeyPressed(Input.Keys.LEFT)  || GameApp.isKeyPressed(Input.Keys.A)) dx -= SPEED;
        if (GameApp.isKeyPressed(Input.Keys.RIGHT) || GameApp.isKeyPressed(Input.Keys.D)) dx += SPEED;
        if (GameApp.isKeyPressed(Input.Keys.UP)    || GameApp.isKeyPressed(Input.Keys.W)) dy -= SPEED;
        if (GameApp.isKeyPressed(Input.Keys.DOWN)  || GameApp.isKeyPressed(Input.Keys.S)) dy += SPEED;

        if (GameApp.isButtonJustPressed(Input.Buttons.LEFT)) {
            targetX = (getMouseX() - offsetX) / scale;
            targetY = (WORLD_H - getMouseY()) / scale;
            hasTarget = true;
        }
        if (dx != 0 || dy != 0) hasTarget = false;
        if (hasTarget) {
            float tx = targetX - (px + PW / 2), ty = targetY - (py + PH - 15);
            float len = (float) Math.sqrt(tx * tx + ty * ty);
            if (len < SPEED) hasTarget = false;
            else { dx = tx / len * SPEED; dy = ty / len * SPEED; }
        }

        if (dx != 0 || dy != 0) {
            if (Math.abs(dx) > Math.abs(dy)) facing = dx < 0 ? "left" : "right";
            else facing = dy < 0 ? "up" : "down";
        }
        boolean moved = false;
        if (dx != 0 && canStandAt(px + dx, py)) { px += dx; moved = true; }
        if (dy != 0 && canStandAt(px, py + dy)) { py += dy; moved = true; }
        if (hasTarget && !moved) hasTarget = false;
        walking = moved;
        if (walking) walkTime += delta; else walkTime = 0;

        if (GameApp.isKeyJustPressed(Input.Keys.E) && nearBed()) {
            fadeOut = 0;    // -> sleep
        }
    }

    boolean canStandAt(float x, float y) {
        Rectangle feet = new Rectangle(x + 23, y + PH - 30, PW - 46, 30);
        if (!floor.contains(feet)) return false;
        for (Rectangle r : solids) {
            if (feet.overlaps(r)) return false;
        }
        return true;
    }

    Rectangle grow(Rectangle r, float a) {
        return new Rectangle(r.x - a, r.y - a, r.width + a * 2, r.height + a * 2);
    }

    @Override
    public void hide() {
        GameApp.disposeTexture("ev_room");
        for (String d : DIRECTIONS) {
            for (String f : FRAMES) {
                GameApp.disposeTexture("ev_" + key(d, f));
            }
        }
        GameApp.disposeFont("ev_font");
        GameApp.stopMusic("ev_bgm");
        GameApp.disposeMusic("ev_bgm");
    }
}