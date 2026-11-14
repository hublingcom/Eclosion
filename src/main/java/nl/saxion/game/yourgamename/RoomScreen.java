package nl.saxion.game.yourgamename;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Rectangle;
import nl.saxion.game.eclosion.DoorTransitionScreen;
import nl.saxion.game.eclosion.FloorTransitionScreen;
import nl.saxion.game.eclosion.NightCutsceneScreen;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * Ageha's room (top-down). Walk with arrows / WASD, or click / tap to walk there.
 * Press E near the desk or the door to interact.
 *
 * Images needed in src/main/resources/images/ :
 *   room.png                       (the bedroom with black margin, 1600 x 902)
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
    static final float IMG_W = 1600, IMG_H = 902;

    final float scale = WORLD_H / IMG_H;                    // image pixels -> world units
    final float offsetX = (WORLD_W - IMG_W * scale) / 2;    // centers the room on screen

    // player (top-left corner, in image pixels). Sprites are 194 x 320, so same ratio here.
    float px = 741, py = 377;
    static final float PH = 200;
    static final float PW = PH * 194f / 320f;
    static final float SPEED = 4.5f;

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
    Rectangle floor      = new Rectangle(376, 282, 859, 394);
    Rectangle bed        = new Rectangle(405, 237, 134, 295);
    Rectangle nightstand = new Rectangle(553, 292, 76, 72);
    Rectangle desk       = new Rectangle(811, 262, 191, 105);
    Rectangle chair      = new Rectangle(873, 317, 53, 90);
    Rectangle wardrobe   = new Rectangle(1051, 155, 131, 150);
    Rectangle cabinet    = new Rectangle(1187, 375, 48, 112); // black box on the right wall
    Rectangle door       = new Rectangle(850, 657, 108, 41);  // bottom wall; not solid, just a spot to interact with
    Rectangle[] solids = { bed, nightstand, desk, chair, wardrobe, cabinet };

    String message = "";

    public RoomScreen() {
        super(1280, 720);
    }

    @Override
    public void show() {
        GameApp.addTexture("room", "images/room.png");
        for (String d : DIRECTIONS) {
            for (String f : FRAMES) {
                GameApp.addTexture(spriteKey(d, f), "images/" + spriteKey(d, f) + ".png");
            }
        }
        GameApp.addFont("roomfont", "fonts/basic.ttf", 40);
        GameApp.addMusic("room_bgm", "audio/room_bgm.ogg");
        GameApp.playMusic("room_bgm", true, 0.4f);   // loop, volume 0.4
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
        Rectangle feet = new Rectangle(x + 23, y + PH - 30, PW - 46, 30);
        if (!floor.contains(feet)) return false;
        for (Rectangle r : solids) {
            if (feet.overlaps(r)) return false;
        }
        return true;
    }

    void interact() {
        Rectangle me = new Rectangle(px, py, PW, PH);
        if (me.overlaps(grow(desk, 60))) {
            message = "A desk. Something is missing from the drawer...";
        } else if (me.overlaps(grow(door, 60))) {
            // leave the room: door -> stairs -> night cutscene -> Floor 1
            // (later: go to the corridor first, once CorridorScreen exists)
            message = "";
            FloorTransitionScreen.prepare("Day 2", "Floor 1", "Loneliness", "NightCutsceneScreen");
            NightCutsceneScreen.prepare("Floor1FlowerScreen");
            DoorTransitionScreen.go("FloorTransitionScreen");
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
        GameApp.stopMusic("room_bgm");
        GameApp.disposeMusic("room_bgm");
    }
}