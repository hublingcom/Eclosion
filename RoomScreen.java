package nl.saxion.game.yourgamename;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Rectangle;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * Ageha's room (top-down). Walk with arrows / WASD, or click / tap to walk there.
 * Press E near the desk or the door to interact.
 *
 * Images needed in src/main/resources/textures/ :
 *   room.png                       (the bedroom, 1557 x 1010)
 *   ageha_down_idle.png, ageha_down_a.png, ageha_down_b.png
 *   ageha_up_idle.png,   ageha_up_a.png,   ageha_up_b.png
 *   ageha_left_idle.png, ageha_left_a.png, ageha_left_b.png
 *   ageha_right_idle.png, ageha_right_a.png, ageha_right_b.png
 *   (all 12 Ageha frames come in ageha_sprites.zip)
 *
 * All positions (player, furniture) are in IMAGE pixels measured from the top-left of
 * room.png, like in an image editor. Only when drawing do we convert to the game world
 * (where y goes UP).
 */
public class RoomScreen extends ScalableGameScreen {

    static final float WORLD_W = 1280, WORLD_H = 720;
    static final float IMG_W = 1557, IMG_H = 1010;

    final float scale = WORLD_H / IMG_H;                    // image pixels -> world units
    final float offsetX = (WORLD_W - IMG_W * scale) / 2;    // centers the room on screen

    // player (top-left corner, in image pixels). Sprites are 194 x 320, so same ratio here.
    float px = 700, py = 430;
    static final float PH = 170;
    static final float PW = PH * 194f / 320f;
    static final float SPEED = 5;

    // click / tap to walk
    boolean hasTarget = false;
    float targetX, targetY;

    // walking animation
    static final String[] DIRECTIONS = { "down", "up", "left", "right" };
    static final String[] FRAMES = { "idle", "a", "b" };
    static final int[] WALK_CYCLE = { 1, 0, 2, 0 };         // a, idle, b, idle
    static final float FRAME_TIME = 0.14f;                  // seconds per frame (smaller = faster legs)
    String facing = "down";
    boolean walking = false;
    float walkTime = 0;

    // walkable floor and solid furniture (image pixels: x, y from top, width, height)
    Rectangle floor = new Rectangle(80, 300, 1398, 570);
    Rectangle bed   = new Rectangle(135, 245, 195, 345);
    Rectangle desk  = new Rectangle(667, 278, 225, 145);    // desk + chair
    Rectangle door  = new Rectangle(1275, 138, 140, 180);   // not solid, just a spot to interact with
    Rectangle[] solids = { bed, desk };

    String message = "";

    public RoomScreen() {
        super(1280, 720);
    }

    @Override
    public void show() {
        GameApp.addTexture("room", "textures/room.png");
        for (String d : DIRECTIONS) {
            for (String f : FRAMES) {
                GameApp.addTexture(spriteKey(d, f), "textures/" + spriteKey(d, f) + ".png");
            }
        }
        GameApp.addFont("roomfont", "fonts/basic.ttf", 40);
    }

    String spriteKey(String direction, String frame) {
        return "ageha_" + direction + "_" + frame;
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        handleInput(delta);

        GameApp.clearScreen("black");
        GameApp.startSpriteRendering();
        GameApp.drawTexture("room", offsetX, 0, IMG_W * scale, WORLD_H);
        drawAgeha();
        if (!message.isEmpty()) {
            GameApp.drawTextCentered("roomfont", message, WORLD_W / 2, 50, "amber-500");
        }
        GameApp.endSpriteRendering();
    }

    void drawAgeha() {
        String frame = "idle";
        if (walking) {
            int step = (int) (walkTime / FRAME_TIME) % WALK_CYCLE.length;
            frame = FRAMES[WALK_CYCLE[step]];
        }
        // y is flipped here: world y goes up, image y goes down
        GameApp.drawTexture(spriteKey(facing, frame),
                offsetX + px * scale, WORLD_H - (py + PH) * scale, PW * scale, PH * scale);
    }

    // ---------- input + movement ----------
    void handleInput(float delta) {
        float dx = 0, dy = 0;

        // keyboard
        if (GameApp.isKeyPressed(Input.Keys.LEFT)  || GameApp.isKeyPressed(Input.Keys.A)) dx -= SPEED;
        if (GameApp.isKeyPressed(Input.Keys.RIGHT) || GameApp.isKeyPressed(Input.Keys.D)) dx += SPEED;
        if (GameApp.isKeyPressed(Input.Keys.UP)    || GameApp.isKeyPressed(Input.Keys.W)) dy -= SPEED;
        if (GameApp.isKeyPressed(Input.Keys.DOWN)  || GameApp.isKeyPressed(Input.Keys.S)) dy += SPEED;

        // mouse / touch: tap somewhere to walk there
        if (GameApp.isButtonJustPressed(Input.Buttons.LEFT)) {
            targetX = (getMouseX() - offsetX) / scale;      // world -> image pixels
            targetY = (WORLD_H - getMouseY()) / scale;
            hasTarget = true;
        }
        if (dx != 0 || dy != 0) hasTarget = false;          // keyboard overrides a tap

        if (hasTarget) {
            float feetX = px + PW / 2, feetY = py + PH - 15;
            float tx = targetX - feetX, ty = targetY - feetY;
            float len = (float) Math.sqrt(tx * tx + ty * ty);
            if (len < SPEED) {
                hasTarget = false;
            } else {
                dx = tx / len * SPEED;
                dy = ty / len * SPEED;
            }
        }

        // face the way he is trying to go
        if (dx != 0 || dy != 0) {
            if (Math.abs(dx) > Math.abs(dy)) facing = dx < 0 ? "left" : "right";
            else facing = dy < 0 ? "up" : "down";
        }

        // try X and Y separately so you slide along walls instead of getting stuck
        boolean moved = false;
        if (dx != 0 && canStandAt(px + dx, py)) { px += dx; moved = true; }
        if (dy != 0 && canStandAt(px, py + dy)) { py += dy; moved = true; }
        if (hasTarget && !moved) hasTarget = false;         // blocked: stop walking

        walking = moved;
        if (walking) walkTime += delta; else walkTime = 0;

        // interact
        if (GameApp.isKeyJustPressed(Input.Keys.E)) interact();
    }

    /** Only the feet collide, so the head can overlap furniture (looks right in top-down). */
    boolean canStandAt(float x, float y) {
        Rectangle feet = new Rectangle(x + 20, y + PH - 30, PW - 40, 30);
        if (!floor.contains(feet)) return false;
        for (Rectangle r : solids) {
            if (feet.overlaps(r)) return false;
        }
        return true;
    }

    void interact() {
        Rectangle me = new Rectangle(px, py, PW, PH);
        if (me.overlaps(grow(desk, 70))) {
            message = "A desk. Something is missing from the drawer...";
        } else if (me.overlaps(grow(door, 70))) {
            message = "The door to the corridor. (Next: switch to the corridor screen!)";
        } else {
            message = "";
        }
    }

    Rectangle grow(Rectangle r, float amount) {
        return new Rectangle(r.x - amount, r.y - amount, r.width + amount * 2, r.height + amount * 2);
    }

    @Override
    public void hide() {
        GameApp.disposeTexture("room");
        for (String d : DIRECTIONS) {
            for (String f : FRAMES) {
                GameApp.disposeTexture(spriteKey(d, f));
            }
        }
        GameApp.disposeFont("roomfont");
    }
}
