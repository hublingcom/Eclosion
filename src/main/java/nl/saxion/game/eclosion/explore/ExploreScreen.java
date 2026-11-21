package nl.saxion.game.eclosion.explore;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import nl.saxion.game.eclosion.audio.Bgm;
import nl.saxion.game.eclosion.audio.Sfx;
import nl.saxion.game.eclosion.ui.ClickEffect;
import nl.saxion.game.eclosion.ui.CollectionMenu;
import nl.saxion.game.eclosion.ui.DialogueBox;
import nl.saxion.game.eclosion.ui.DialogueLine;
import nl.saxion.game.eclosion.ui.PauseMenu;
import nl.saxion.game.eclosion.ui.ScreenFader;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for every place Ageha can walk around in (his room, the corridor, ...).
 *
 * Walk with the arrow keys / WASD, or left click somewhere to walk there.
 * Stand next to something and press E to look at it (only E; clicking only walks). The text appears
 * in the dialogue box as Ageha's inner voice (no name, no portrait). Space / Enter / E / click = next line.
 * Doors: press E at a door, the screen fades to black and the next place fades in.
 * Esc opens the pause menu (Resume / Settings / Title Screen / Quit Game).
 *
 * A place only has to say which picture it uses and fill in buildRoom() and placePlayer().
 * All positions there are IMAGE pixels of the picture (x, y from the top-left, like in Krita);
 * imageRect() converts them to the game world.
 */
public abstract class ExploreScreen extends ScalableGameScreen {

    // ---------- picture ----------
    protected static final float WORLD_W = 1280, WORLD_H = 720;
    private final String textureName;
    private final String imagePath;
    protected final float imgW, imgH;
    protected final float scale;     // image pixels -> world units
    protected final float roomY;     // centres the picture vertically

    // ---------- Ageha ----------
    protected float agehaH = 110;                            // height on screen (a place can change it to fit its furniture)
    private static final float NORMAL_H = 110;               // the size the speeds below are made for
    private static final float SPEED = 240;                  // world units per second (at NORMAL_H)
    private static final float ACCELERATION = 10;            // how fast he gets up to speed (higher = snappier)
    private static final float DECELERATION = 13;            // how fast he stops
    private static final float ARRIVE_DISTANCE = 40;         // click-walk: starts slowing down this close to the spot
    private static final float STEP_LENGTH = 24;             // distance walked per animation frame
    private static final float BOB = 1.5f;                   // tiny up/down movement while walking
    private static final String[] DIRECTIONS = {"down", "up", "left", "right"};
    private static final int[] WALK_CYCLE = {1, 2, 3, 2};   // step, stand, step, stand

    protected float x, y;                                    // bottom-left corner of the sprite (world)
    private float vx = 0, vy = 0;                            // current speed
    protected String facing = "down";
    private boolean walking = false;
    private float walkDistance = 0;                          // distance walked since he started moving
    private int lastStepIndex = -1;                          // for the footstep sounds
    /** True while Ageha is not drawn (e.g. he is asleep and drawn as part of the bed). */
    protected boolean agehaHidden = false;

    // ---------- light ----------
    protected RoomLight light;
    private Color agehaTint;

    // ---------- layout of the place (filled in by buildRoom) ----------
    protected Rectangle floor;                               // where Ageha's feet may go
    protected final List<Rectangle> extraFloor = new ArrayList<>();   // more walkable floor (e.g. a hallway leading in)
    /**
     * Narrow places like stairs: here only the MIDDLE of his feet has to be inside, so he can
     * walk on the whole width of the steps (his feet box is almost as wide as the stairs).
     */
    protected final List<Rectangle> stepFloor = new ArrayList<>();
    protected final List<Rectangle> solids = new ArrayList<>();   // furniture footprints, walls
    private final List<Interactable> items = new ArrayList<>();   // things to look at with E (and doors)
    private final List<Npc> npcs = new ArrayList<>();             // other characters standing in the place
    private final List<Prop> props = new ArrayList<>();           // furniture drawn over Ageha when he is behind it
    private final List<Exit> exits = new ArrayList<>();           // open ways out (ends of hallways)
    private boolean built = false;                           // layout is made once, so "seen" etc. is remembered

    /** An open way out (the end of a hallway): walking into it goes to the other place, no E needed. */
    private record Exit(Rectangle zone, String targetScreen, String notYet) { }

    /** Something to draw, sorted by how far back it stands. */
    private record Layer(float baseY, Runnable draw) { }

    // ---------- text ----------
    protected final DialogueBox dialogue = new DialogueBox();
    /** Dialogue portraits this place needs (texture names in textures/portraits). Fill in the constructor. */
    protected final List<String> portraits = new ArrayList<>();

    // ---------- click to walk ----------
    private boolean hasTarget = false;
    private float targetX, targetY;                          // where the middle of the feet should go
    private float lastDistance = Float.MAX_VALUE;            // to notice when a click-walk is stuck
    private float stuckTime = 0;
    private static final float STUCK_SPEED = 8f;             // getting closer slower than this ...
    private static final float STUCK_TIME = 0.35f;           // ... for this long = stuck, stop
    private static final float CORNER_HELP = 16f;            // keyboard: slip around edges up to this far off

    // ---------- walking up to someone before talking ----------
    private Npc talkTo = null;                               // who Ageha is walking up to
    private Runnable startTalk = null;                       // the conversation to start when he gets there

    // ---------- doors and fading to another place ----------
    /** Name of the screen Ageha just came from (set when he goes through a door or a hallway). */
    protected static String arrivedFrom = null;
    /** True while Ageha goes through a door (the next place then plays the door closing). */
    private static boolean throughDoor = false;
    private final ScreenFader fader = new ScreenFader();
    private String nextScreen = null;                        // where to go once the screen is black

    // ---------- music and the pause menu ----------
    private static final float MUSIC_VOLUME = 0.3f;          // floor theme volume
    private final PauseMenu pauseMenu = new PauseMenu();
    private final CollectionMenu collection = new CollectionMenu();   // I / Tab: the memory items (Dawon)

    // ---------- night (see TimeOfDay) ----------
    private static final float NIGHT_DARKNESS = 0.70f;       // 0 = like the day, 1 = all black
    private static final float NIGHT_LAMP_REACH = 1.8f;      // lamps light up a bigger pool than by day
    private static final int NIGHT_CELL = 4;                 // world units per pixel of the night shade
    private Texture nightShade;                              // made the first time it is needed in this place

    // ---------- setting up ----------
    protected ExploreScreen(String textureName, String imagePath, float imgW, float imgH) {
        super(1280, 720);
        this.textureName = textureName;
        this.imagePath = imagePath;
        this.imgW = imgW;
        this.imgH = imgH;
        this.scale = WORLD_W / imgW;
        this.roomY = (WORLD_H - imgH * scale) / 2;
    }

    /** Floor, furniture and items. Called once. */
    protected abstract void buildRoom();

    /** Put Ageha where he enters (look at arrivedFrom to see which door he came through). */
    protected abstract void placePlayer();

    @Override
    public void show() {
        GameApp.addTexture(textureName, imagePath);
        light = new RoomLight(imagePath, scale, roomY);
        for (String d : DIRECTIONS) {
            for (int n = 1; n <= 3; n++) {
                GameApp.addTexture(frameName(d, n), "textures/sprites/" + frameName(d, n) + ".png");
            }
        }
        for (String portrait : portraits) {
            GameApp.addTexture(portrait, "textures/portraits/" + portrait + ".png");
        }
        DialogueBox.loadAssets();
        Sfx.load();
        if (music() != null) {
            Bgm.play(music(), MUSIC_VOLUME);   // the same music keeps going from room to room
        } else {
            Bgm.fadeOutAndStop();              // a quiet place: the music fades away
        }
        if (throughDoor) {
            Sfx.play(Sfx.DOOR_CLOSE);        // came in through a door: it closes behind him
            throughDoor = false;
        }
        dialogue.setPortraits(null, null);   // inner voice: no portraits
        if (!built) {
            buildRoom();
            outline = new RoomOutline(imagePath);   // where clicks are ignored (the black around the room)
            walkGrid = makeWalkGrid();                // for finding a way around furniture when clicking
            built = true;
        }
        for (Npc npc : npcs) {
            for (String d : DIRECTIONS) {
                for (int n = 1; n <= 3; n++) {
                    String name = npc.spritePrefix + "_" + d + "_" + n;
                    GameApp.addTexture(name, "textures/sprites/" + name + ".png");
                }
            }
        }
        for (Prop prop : props) {
            GameApp.addTexture(prop.texture, "textures/props/" + prop.texture + ".png");
        }
        addLights();
        placePlayer();
        stopWalking();
        agehaTint = light.newTint();
        for (Npc npc : npcs) {
            npc.tint = light.newTint();
            light.lightAt(npc.x + npcW(npc) / 2, npc.y + 4, npc.tint);   // start with the right light, no fade-in
        }
        nextScreen = null;
        fader.fadeIn();
    }

    /** Lamps etc. (light.addLight). Called every time the screen is shown. */
    protected void addLights() {
    }

    // ---------- hooks a place can override ----------
    protected boolean overlayOpen() { return false; }
    protected void updateOverlay(float delta) { }
    protected void renderOverlay() { }
    /** Called once when a text in the dialogue box has finished. */
    protected void onTextFinished() { }
    /** Every frame (not while paused): for characters that move on their own while the player walks. */
    protected void updateRoom(float delta) { }

    // ---------- making the room: things to look at, doors, ways out, furniture ----------
    /** Looking at things is Ageha's inner voice: no name, no portrait. */
    protected DialogueLine[] lines(String... texts) {
        DialogueLine[] result = new DialogueLine[texts.length];
        for (int i = 0; i < texts.length; i++) {
            result[i] = DialogueLine.narration(texts[i]);
        }
        return result;
    }

    /** Something to look at with E: one text the first time, another one after that. */
    protected Interactable addItem(String name, Rectangle area, DialogueLine[] firstTime, DialogueLine[] again) {
        Interactable item = new Interactable(name, area, firstTime, again, null);
        items.add(item);
        return item;
    }

    /** Something that does its own thing when Ageha presses E at it (a keypad, a cutscene, ...). */
    protected Interactable addSpecial(String name, Rectangle area, Runnable action) {
        Interactable item = new Interactable(name, area, null, null, action);
        items.add(item);
        return item;
    }

    /** A door: E fades to black and goes to the other screen. */
    protected void addDoor(String name, Rectangle area, String targetScreen) {
        addSpecial(name, area, () -> goThroughDoor(targetScreen));
    }

    /** Walking into this area (world units) fades to black and goes to the other screen. */
    protected void addExit(Rectangle zone, String targetScreen) {
        exits.add(new Exit(zone, targetScreen, null));
    }

    /**
     * A way out to a place that is not made yet (PLACEHOLDER): walking into it shows this
     * line and Ageha steps back. Later: replace it with addExit(zone, "TheNewScreen").
     */
    protected void addExitNotMadeYet(Rectangle zone, String notYet) {
        exits.add(new Exit(zone, null, notYet));
    }

    /**
     * Furniture that can cover Ageha. (ix, iy, iw, ih) = where the cut-out picture sits in the room
     * picture, baseIy = image y of the furniture's front edge on the floor.
     */
    protected void addProp(String texture, float ix, float iy, float iw, float ih, float baseIy) {
        props.add(new Prop(texture, imageRect(ix, iy, iw, ih), imageRect(0, baseIy, 0, 0).y));
    }

    /** Shows or hides a prop made with addProp (by its texture name). */
    protected void setPropVisible(String texture, boolean visible) {
        for (Prop prop : props) {
            if (prop.texture.equals(texture)) {
                prop.visible = visible;
            }
        }
    }

    /** The music of this place. Floor 1 (the corridor, the main hall) has one theme, and a darker one at night. */
    protected String music() {
        return TimeOfDay.isNight() ? Bgm.FLOOR1_NIGHT : Bgm.FLOOR1;
    }

    // ---------- going to another place ----------
    /** Opens a door and goes through it (door sound now, closing sound in the next place). */
    protected void goThroughDoor(String targetScreen) {
        throughDoor = true;
        goTo(targetScreen);
    }

    /** Fades to black and switches to that screen (with a door or transition sound). */
    protected void goTo(String screenName) {
        stopWalking();
        nextScreen = screenName;
        fader.fadeOut();
        if (throughDoor) {
            Sfx.play(Sfx.DOOR_OPEN);
        } else {
            Sfx.play(Sfx.TRANSITION);       // a hallway, a minigame, ...
        }
    }

    /** For screens that are not rooms (minigames, the title screen): say where Ageha comes back from. */
    public static void comeBackFrom(String screenName) {
        arrivedFrom = screenName;
    }

    // ---------- placing Ageha ----------
    /** Puts Ageha so that the middle of his feet is at this image pixel. */
    protected void placeFeetAtImage(float ix, float iy, String facingDirection) {
        Rectangle spot = imageRect(ix, iy, 0, 0);
        x = spot.x - agehaW() / 2;
        y = spot.y;
        facing = facingDirection;
    }

    /** For cutscenes that move Ageha themselves: show his walk frames for this distance walked so far. */
    protected void showAgehaWalking(float distanceSoFar) {
        walking = true;
        walkDistance = distanceSoFar;
    }

    // ---------- other characters ----------
    /** Niri, the guide: his walk sprites and his height compared to Ageha (a bit shorter). */
    protected static final String NIRI_SPRITES = "niri_walk";
    protected static final float NIRI_HEIGHT = 0.90f;

    /**
     * Puts another character in the room with the middle of their feet at this image pixel.
     * frameW/frameH = size of their sprite frames, figureH = how tall the character is inside a frame.
     * Ageha can't walk through them.
     */
    protected Npc addNpc(String spritePrefix, float frameW, float frameH, float figureH,
                         float ix, float iy, String facingDirection) {
        Npc npc = new Npc(spritePrefix, frameW, frameH, figureH, 1f);
        Rectangle spot = imageRect(ix, iy, 0, 0);
        npc.x = spot.x - npcW(npc) / 2;
        npc.y = spot.y;
        npc.facing = facingDirection;
        npcs.add(npc);
        blockNpc(npc);
        return npc;
    }

    /**
     * A character that comes and goes: starts hidden (show it with showNpcAtImage), doesn't block
     * Ageha, walks with moveNpcToward. relativeHeight: 1 = as tall as Ageha.
     */
    protected Npc addMovingNpc(String spritePrefix, float frameW, float frameH, float figureH, float relativeHeight) {
        Npc npc = new Npc(spritePrefix, frameW, frameH, figureH, relativeHeight);
        npc.facing = "down";
        npc.visible = false;
        npcs.add(npc);
        return npc;
    }

    /** Adds Niri as a moving character (see addMovingNpc). */
    protected Npc addNiri() {
        return addMovingNpc(NIRI_SPRITES, 322, 408, 392, NIRI_HEIGHT);
    }

    /** Shows a character with the middle of its feet at this image pixel. */
    protected void showNpcAtImage(Npc npc, float ix, float iy, String facingDirection) {
        Rectangle spot = imageRect(ix, iy, 0, 0);
        npc.x = spot.x - npcW(npc) / 2;
        npc.y = spot.y;
        npc.facing = facingDirection;
        npc.visible = true;
        npc.alpha = 1;
        npc.fadeTo = 1;
    }

    protected void hideNpc(Npc npc) {
        npc.visible = false;
    }

    /**
     * Ageha can't walk through this character: their feet AND the lower part of their body block,
     * so he can't stand right "on" them either. (For one that stands still in the room.)
     */
    protected void blockNpc(Npc npc) {
        if (npc.block != null) {
            return;
        }
        npc.block = npcBlock(npc);
        solids.add(npc.block);
        walkGridChanged();
    }

    /** Ageha can walk through this character again (for one that has left). */
    protected void unblockNpc(Npc npc) {
        if (npc.block == null) {
            return;
        }
        solids.remove(npc.block);
        npc.block = null;
        walkGridChanged();
    }

    /** The part of a character Ageha can't walk into. */
    private Rectangle npcBlock(Npc npc) {
        return new Rectangle(npc.x + npcW(npc) * 0.25f, npc.y, npcW(npc) * 0.5f, npcH(npc) * 0.22f);
    }

    /** The character slowly appears (from invisible). */
    protected void fadeNpcIn(Npc npc) {
        npc.alpha = 0;
        npc.fadeTo = 1;
    }

    /** The character slowly disappears (call hideNpc when it is gone). */
    protected void fadeNpcOut(Npc npc) {
        npc.fadeTo = 0;
    }

    /** True once a character that is fading out can't be seen any more. */
    protected boolean npcFadedOut(Npc npc) {
        return npc.alpha <= 0;
    }

    /**
     * Moves a character a bit toward this image pixel (middle of its feet), speed in world units
     * per second. Call it every frame; returns true once it is there.
     */
    protected boolean moveNpcToward(Npc npc, float ix, float iy, float speed, float delta) {
        Rectangle spot = imageRect(ix, iy, 0, 0);
        return moveNpcTowardWorld(npc, spot.x, spot.y, speed, delta);
    }

    /** Like moveNpcToward, but the spot (middle of the feet) is in world units. */
    protected boolean moveNpcTowardWorld(Npc npc, float wx, float wy, float speed, float delta) {
        float tx = wx - npcW(npc) / 2, ty = wy;
        float dx = tx - npc.x, dy = ty - npc.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance < 1.5f) {
            stopNpcAt(npc, tx, ty);
            return true;
        }
        // glide: speed up softly at the start and slow down softly near the spot
        float wanted = speed * Math.min(1, distance / (speed * 0.5f));
        float targetVx = dx / distance * wanted, targetVy = dy / distance * wanted;
        float smooth = Math.min(1, delta * 4);
        npc.vx += (targetVx - npc.vx) * smooth;
        npc.vy += (targetVy - npc.vy) * smooth;
        float step = (float) Math.sqrt(npc.vx * npc.vx + npc.vy * npc.vy) * delta;
        if (step >= distance) {
            stopNpcAt(npc, tx, ty);
            return true;
        }
        npc.x += npc.vx * delta;
        npc.y += npc.vy * delta;
        npc.moving = true;
        npc.walkDistance += step;
        // look where they are going
        if (Math.abs(npc.vx) > Math.abs(npc.vy)) {
            npc.facing = npc.vx > 0 ? "right" : "left";
        } else {
            npc.facing = npc.vy > 0 ? "up" : "down";
        }
        return false;
    }

    /** The character has arrived: standing still at (tx, ty), bottom-left of the sprite. */
    private void stopNpcAt(Npc npc, float tx, float ty) {
        npc.x = tx;
        npc.y = ty;
        npc.vx = 0;
        npc.vy = 0;
        npc.moving = false;
        npc.walkDistance = 0;
    }

    /**
     * Ageha walks up to this character (to the side he is on), then they turn to each other
     * and the conversation starts. If someone else already stands at that side, he walks to
     * the front of the character instead. Arrow keys / a click on the way cancel it.
     */
    protected void walkUpTo(Npc npc, Runnable conversation) {
        Rectangle block = npcBlock(npc);
        float gap = 6;
        float npcCenterX = block.x + block.width / 2;
        float dx = (x + agehaW() / 2) - npcCenterX;
        float dy = (y + 7) - (block.y + block.height / 2);
        float apart = npcW(npc) * 0.36f + agehaW() * 0.36f;   // far enough that the two sprites don't overlap
        float sideX = dx < 0 ? npcCenterX - apart : npcCenterX + apart;
        if (Math.abs(dx) > Math.abs(dy) && !spotTaken(sideX, npc.y, npc)) {
            targetX = sideX;                                     // stand beside them
            targetY = npc.y + 7;
        } else {
            // stand in front of them (below), or behind them (above) when he comes from behind
            boolean front = dy < 0 || Math.abs(dx) > Math.abs(dy);
            targetX = npcCenterX;
            targetY = front ? block.y - gap - 7 : block.y + block.height + gap + 7;
        }
        headTo(targetX, targetY);                  // walks around furniture on the way
        talkTo = npc;
        startTalk = conversation;
    }

    /** True when another visible character stands about here (middle of the feet), so Ageha would overlap them. */
    private boolean spotTaken(float feetX, float feetY, Npc except) {
        for (Npc other : npcs) {
            if (other == except || !other.visible) {
                continue;
            }
            float otherX = other.x + npcW(other) / 2;
            float minApart = npcW(other) * 0.36f + agehaW() * 0.36f;
            if (Math.abs(feetX - otherX) < minApart && Math.abs(feetY - other.y) < 30) {
                return true;
            }
        }
        return false;
    }

    // ---------- every frame ----------
    @Override
    public void render(float delta) {
        super.render(delta);
        fader.update(delta);
        Bgm.update(delta);
        if (!pauseMenu.isOpen()) {
            updateRoom(delta);              // things that move by themselves (the player can still walk)
        }

        if (fader.busy()) {
            stopWalking();                  // no walking or looking while the screen fades
        } else if (pauseMenu.isOpen()) {
            stopWalking();                  // the game stands still while the Esc menu is open
            PauseMenu.Result result = pauseMenu.update(getMouseX(), getMouseY(), getWorldWidth(), getWorldHeight());
            if (result == PauseMenu.Result.TO_TITLE) {
                goTo("TitleScreen");
            } else if (result == PauseMenu.Result.QUIT) {
                GameApp.quit();
            }
        } else if (collection.isOpen()) {
            stopWalking();                  // the game stands still while the collection is open
            collection.update(getMouseX(), getMouseY());
        } else if (overlayOpen()) {
            stopWalking();
            updateOverlay(delta);
        } else if (dialogue.isActive()) {
            dialogue.update(delta, getMouseX(), getMouseY());   // Ageha can't walk while the text is shown
            stopWalking();
            if (!dialogue.isActive()) {
                onTextFinished();
            }
        } else if (GameApp.isKeyJustPressed(Input.Keys.ESCAPE)) {
            stopWalking();
            pauseMenu.open(getMouseX(), getMouseY());   // Esc while walking around: the pause menu
        } else if (CollectionMenu.toggleKey()) {
            stopWalking();
            collection.open(getMouseX(), getMouseY());  // I / Tab while walking around: the collection
        } else {
            // a click on the black around the room (outside the pink outline) does nothing at all
            if (GameApp.isButtonJustPressed(Input.Buttons.LEFT) && !isOutsideRoom(getMouseX(), getMouseY())) {
                talkTo = null;              // a click means the player wants to go somewhere else
                startTalk = null;
                walkTo(getMouseX(), getMouseY());
            }
            float oldX = x, oldY = y;
            move(delta);
            for (Exit exit : exits) {
                if (nextScreen == null && feet(x, y).overlaps(exit.zone())) {
                    if (exit.targetScreen() != null) {
                        goTo(exit.targetScreen());   // walked out through an open way
                    } else {
                        // a way that leads somewhere not made yet: step back and say so
                        float backX = oldX + (oldX - x) * 4;
                        float backY = oldY + (oldY - y) * 4;
                        boolean free = canStandAt(backX, backY);
                        x = free ? backX : oldX;
                        y = free ? backY : oldY;
                        stopWalking();
                        dialogue.start(DialogueLine.narration(exit.notYet()));
                    }
                    break;
                }
            }
            if (startTalk != null) {
                if (!hasTarget) {
                    // arrived next to them (or something was in the way): look at each other and talk
                    turnToAgeha(talkTo);
                    Runnable talk = startTalk;
                    talkTo = null;
                    startTalk = null;
                    stopWalking();
                    talk.run();
                }
            } else {
                Interactable near = nearestItem();
                if (near != null && GameApp.isKeyJustPressed(Input.Keys.E)) {
                    lookAt(near);           // only E looks at things
                }
            }
        }

        GameApp.clearScreen("black");
        GameApp.startSpriteRendering();
        GameApp.drawTexture(textureName, 0, roomY, imgW * scale, imgH * scale);
        GameApp.endSpriteRendering();
        if (!agehaHidden) {
            drawShadow(x + agehaW() / 2, y, agehaW(), agehaTint);
        }
        for (Npc npc : npcs) {
            if (npc.visible && npc.alpha > 0.5f) {       // no shadow while fading in or out
                drawShadow(npc.x + npcW(npc) / 2, npc.y, npcW(npc), npc.tint);
            }
        }
        // Characters and furniture further back (higher up on screen) are drawn first,
        // so whatever stands in front covers them.
        List<Layer> layers = new ArrayList<>();
        if (!agehaHidden) {
            layers.add(new Layer(y, this::drawAgeha));
        }
        for (Npc npc : npcs) {
            if (npc.visible) {
                layers.add(new Layer(npc.y, () -> drawNpc(npc)));
            }
        }
        for (Prop prop : props) {
            if (!prop.visible) continue;
            layers.add(new Layer(prop.baseY, () -> GameApp.drawTexture(prop.texture,
                    prop.area.x, prop.area.y, prop.area.width, prop.area.height)));
        }
        layers.sort((a, b) -> Float.compare(b.baseY(), a.baseY()));
        GameApp.startSpriteRendering();
        for (Layer layer : layers) {
            layer.draw().run();
        }
        GameApp.endSpriteRendering();
        drawNight();

        // No "E" hint on screen: the player has to think of pressing E themselves.
        // Only when Ageha really stands behind the box (his whole body within its width), his inner voice
        // is shown at the top instead. Standing next to the box or half behind its edge: it stays at the bottom.
        float boxLeft = getWorldWidth() / 2 - DialogueBox.BOX_WIDTH / 2 + agehaW() / 2;
        float boxRight = getWorldWidth() / 2 + DialogueBox.BOX_WIDTH / 2 - agehaW() / 2;
        float agehaMiddle = x + agehaW() / 2;
        dialogue.setAtTop(y < DialogueBox.BOX_BOTTOM + DialogueBox.BOX_HEIGHT
                && agehaMiddle > boxLeft && agehaMiddle < boxRight);
        dialogue.render(getWorldWidth(), getWorldHeight());
        renderOverlay();
        collection.render(getWorldWidth(), getWorldHeight());
        pauseMenu.render(getWorldWidth(), getWorldHeight());
        fader.render(getWorldWidth(), getWorldHeight());
        ClickEffect.updateAndDraw(getMouseX(), getMouseY());   // little white circle where you click

        // Switch screens only at the very end of the frame, after everything is drawn.
        if (nextScreen != null && fader.isDoneFadingOut()) {
            String target = nextScreen;
            nextScreen = null;
            arrivedFrom = getClass().getSimpleName();
            GameApp.switchScreen(target);
        }
    }

    private void lookAt(Interactable item) {
        stopWalking();
        if (item.action != null) {
            item.action.run();
        } else {
            Sfx.play(Sfx.EXAMINE);          // looking at something
            dialogue.start(item.seen ? item.again : item.firstTime);
        }
        item.seen = true;
    }

    // ---------- movement ----------
    /** Left click: walk to that spot (only walking; looking at things is done with E). */
    private void walkTo(float mouseX, float mouseY) {
        float cx = mouseX, cy = mouseY;
        if (!onFloor(cx, cy) && !onSteps(cx, cy)) {
            // clicked outside the room (black area, a wall): first pull it onto the floor
            // (things on the wall are reached from the floor below them)
            cx = clamp(cx, floor.x + agehaW() * 0.25f, floor.x + floor.width - agehaW() * 0.25f);
            cy = clamp(cy, floor.y + 1, floor.y + floor.height - 15);
        }
        // then the nearest spot where his feet really fit (not inside furniture, not half on a wall)
        float[] spot = walkGrid.nearestFreeSpot(cx, cy);
        if (spot == null) {
            return;                         // nowhere near to stand: ignore the click
        }
        headTo(spot[0], spot[1]);
    }

    // ---------- finding a way around things (see WalkGrid) ----------
    private WalkGrid walkGrid;

    private WalkGrid makeWalkGrid() {
        return new WalkGrid((mx, my) -> canStandAt(mx - agehaW() / 2, my - 7), WORLD_W, roomY, imgH * scale);
    }

    /** Something that blocks the way came or went: the walk grid has to be made again. */
    private void walkGridChanged() {
        if (walkGrid != null) {
            walkGrid = makeWalkGrid();
        }
    }
    /** Way points still to walk to after the current target (middle of the feet). */
    private final List<float[]> path = new ArrayList<>();
    private static final float WAYPOINT_REACHED = 4;         // this close to a way point: go on to the next

    /**
     * Plans a way around furniture and walls to (tx, ty) (middle of the feet) and starts walking it.
     * If the spot can't be reached, he walks to the closest spot he can reach.
     */
    private void headTo(float tx, float ty) {
        path.clear();
        hasTarget = false;
        float[] spot = walkGrid.nearestFreeSpot(tx, ty);
        if (spot == null) {
            return;
        }
        path.addAll(walkGrid.findPath(x + agehaW() / 2, y + 7, spot[0], spot[1]));
        nextWaypoint();
    }

    /** Walks on to the next point of the way (or stops when there is none left). */
    private void nextWaypoint() {
        if (path.isEmpty()) {
            hasTarget = false;
            return;
        }
        float[] p = path.remove(0);
        targetX = p[0];
        targetY = p[1];
        hasTarget = true;
        lastDistance = Float.MAX_VALUE;
        stuckTime = 0;
    }

    private void move(float delta) {
        // 1. Which way does the player want to go? (length 0..1)
        float dx = 0, dy = 0;
        if (GameApp.isKeyPressed(Input.Keys.LEFT) || GameApp.isKeyPressed(Input.Keys.A)) dx -= 1;
        if (GameApp.isKeyPressed(Input.Keys.RIGHT) || GameApp.isKeyPressed(Input.Keys.D)) dx += 1;
        if (GameApp.isKeyPressed(Input.Keys.UP) || GameApp.isKeyPressed(Input.Keys.W)) dy += 1;
        if (GameApp.isKeyPressed(Input.Keys.DOWN) || GameApp.isKeyPressed(Input.Keys.S)) dy -= 1;
        float wanted = 1f;                  // 1 = full speed, less = slowing down

        if (dx != 0 || dy != 0) {
            hasTarget = false;              // the keyboard takes over from a click
            path.clear();
            talkTo = null;                  // ... and from walking up to someone
            startTalk = null;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            dx /= length;                   // diagonal is not faster than straight
            dy /= length;
        } else if (hasTarget) {
            float tx = targetX - (x + agehaW() / 2);
            float ty = targetY - (y + 7);
            float distance = (float) Math.sqrt(tx * tx + ty * ty);
            if (!path.isEmpty() && distance < WAYPOINT_REACHED) {
                nextWaypoint();                     // a corner of the way: on to the next point
                tx = targetX - (x + agehaW() / 2);
                ty = targetY - (y + 7);
                distance = (float) Math.sqrt(tx * tx + ty * ty);
            }
            // not getting closer for a moment (walking against something): give up and stand still
            if (lastDistance - distance < STUCK_SPEED * delta) {
                stuckTime += delta;
            } else {
                stuckTime = 0;
            }
            lastDistance = distance;
            if ((distance < 2f && path.isEmpty()) || stuckTime > STUCK_TIME) {
                hasTarget = false;          // arrived (or can't get any closer)
                path.clear();
                if (distance >= 2f) {
                    vx = 0;
                    vy = 0;
                }
            } else {
                dx = tx / distance;
                dy = ty / distance;
                if (path.isEmpty()) {
                    wanted = Math.min(1f, distance / ARRIVE_DISTANCE);   // slow down near the end
                }
            }
        }

        // 2. Speed up / slow down smoothly instead of starting and stopping instantly
        float speedNow = SPEED * size() * wanted;   // a smaller Ageha also walks a bit slower
        float targetVx = dx * speedNow, targetVy = dy * speedNow;
        float ease = 1f - (float) Math.exp(-((dx != 0 || dy != 0) ? ACCELERATION : DECELERATION) * delta);
        vx += (targetVx - vx) * ease;
        vy += (targetVy - vy) * ease;
        if (Math.abs(vx) < 1f && dx == 0) vx = 0;
        if (Math.abs(vy) < 1f && dy == 0) vy = 0;

        // 3. Turn to face the way he walks (with a little margin, so he doesn't flicker on diagonals)
        if (dx != 0 || dy != 0) {
            updateFacing(dx, dy);
        }

        // 4. Move, checking furniture for X and Y separately
        float stepX = vx * delta, stepY = vy * delta;
        boolean blockedX = stepX != 0 && !canStandAt(x + stepX, y);
        boolean blockedY = stepY != 0 && !canStandAt(x, y + stepY);
        // slide along furniture instead of getting stuck (a click-walk follows a planned way,
        // so it only brushes corners; if it really can't get on, the stuck check stops it)
        if (blockedX) vx = 0; else x += stepX;
        if (blockedY) vy = 0; else y += stepY;
        // walking straight into an edge that is just a little off (the narrow stairs, a doorway):
        // slide him sideways onto the free way
        if (!hasTarget && dx == 0 && dy != 0 && blockedY) {
            cornerHelp(0, dy, delta);
        } else if (!hasTarget && dy == 0 && dx != 0 && blockedX) {
            cornerHelp(dx, 0, delta);
        }

        // 5. Legs move with the distance walked, so the feet don't slide when he speeds up or slows down
        float speed = (float) Math.sqrt(vx * vx + vy * vy);
        walking = speed > 25f;
        if (walking) {
            walkDistance += speed * delta;
            // footstep sound each time a foot comes down (the two "step" frames of the walk)
            int stepIndex = (int) (walkDistance / (STEP_LENGTH * size()));
            if (stepIndex != lastStepIndex) {
                lastStepIndex = stepIndex;
                if (WALK_CYCLE[stepIndex % WALK_CYCLE.length] != 2) {
                    if (onSteps(x + agehaW() / 2, y + 7)) {
                        Sfx.stairStep();        // wooden stairs
                    } else {
                        Sfx.step();             // carpet
                    }
                }
            }
        } else {
            walkDistance = 0;               // next time he starts with a step
            lastStepIndex = -1;
        }
    }

    /**
     * Ageha walks (keyboard) straight into something. If the way is free a few units to the side,
     * move him a bit towards that side, so he slips around the corner instead of getting stuck.
     */
    private void cornerHelp(float dx, float dy, float delta) {
        float push = SPEED * size() * 0.6f * delta;
        float aheadX = Math.signum(dx) * 3, aheadY = Math.signum(dy) * 3;
        for (float o = 1; o <= CORNER_HELP; o += 1) {
            for (int side = -1; side <= 1; side += 2) {
                float ox = dy != 0 ? side * o : 0;   // walking up/down: look left/right
                float oy = dx != 0 ? side * o : 0;   // walking left/right: look up/down
                if (canStandAt(x + ox, y + oy) && canStandAt(x + ox + aheadX, y + oy + aheadY)) {
                    float step = Math.min(push, o);
                    x += Math.signum(ox) * step;
                    y += Math.signum(oy) * step;
                    return;
                }
            }
        }
    }

    private void updateFacing(float dx, float dy) {
        boolean horizontal = Math.abs(dx) > Math.abs(dy);
        boolean nearlyDiagonal = Math.abs(Math.abs(dx) - Math.abs(dy)) < 0.25f;
        boolean stillFits = (facing.equals("left") && dx < 0) || (facing.equals("right") && dx > 0)
                || (facing.equals("up") && dy > 0) || (facing.equals("down") && dy < 0);
        if (nearlyDiagonal && stillFits) {
            return;                         // keep the current direction
        }
        if (horizontal) facing = dx < 0 ? "left" : "right";
        else facing = dy > 0 ? "up" : "down";
    }

    protected void stopWalking() {
        hasTarget = false;
        path.clear();
        vx = 0;
        vy = 0;
        walking = false;
        walkDistance = 0;
    }

    /** Only the feet collide, so the head can overlap the wall behind (looks right in top-down). */
    private Rectangle feet(float px, float py) {
        return new Rectangle(px + agehaW() * 0.25f, py, agehaW() * 0.5f, 14);
    }

    private boolean canStandAt(float px, float py) {
        // on stairs: the middle of the feet on the steps is enough
        float middleX = px + agehaW() / 2, middleY = py + 7;
        for (Rectangle step : stepFloor) {
            if (step.contains(middleX, middleY)) return true;
        }
        Rectangle f = feet(px, py);
        // every corner of the feet must be on some floor (so the feet can cross from a hallway into the room)
        if (!onFloor(f.x, f.y) || !onFloor(f.x + f.width, f.y)
                || !onFloor(f.x, f.y + f.height) || !onFloor(f.x + f.width, f.y + f.height)) {
            return false;
        }
        for (Rectangle r : solids) {
            if (f.overlaps(r)) return false;
        }
        return true;
    }

    private boolean onSteps(float px, float py) {
        for (Rectangle step : stepFloor) {
            if (step.contains(px, py)) return true;
        }
        return false;
    }

    private boolean onFloor(float px, float py) {
        if (floor.contains(px, py)) return true;
        for (Rectangle extra : extraFloor) {
            if (extra.contains(px, py)) return true;
        }
        return false;
    }

    private boolean inReach(Interactable item) {
        return grow(feet(x, y), 28).overlaps(item.area);
    }

    /** The closest item within reach of Ageha's feet, or null. */
    private Interactable nearestItem() {
        Interactable best = null;
        float bestDistance = Float.MAX_VALUE;
        for (Interactable item : items) {
            if (!item.enabled || !inReach(item)) continue;
            float cx = item.area.x + item.area.width / 2 - (x + agehaW() / 2);
            float cy = item.area.y + item.area.height / 2 - y;
            float distance = cx * cx + cy * cy;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = item;
            }
        }
        return best;
    }

    // ---------- drawing ----------
    /** Soft dark oval under a character's feet, so they stand on the floor instead of floating over it. */
    private void drawShadow(float centerX, float feetY, float width, Color tint) {
        float cy = feetY + 4;
        float alpha = light.shadowAlpha(tint);
        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        // two ovals: a wide faint one and a smaller darker one, for a soft edge
        GameApp.drawEllipseCentered(centerX, cy, width * 0.80f, 16, new Color(0, 0, 0, alpha * 0.5f));
        GameApp.drawEllipseCentered(centerX, cy, width * 0.55f, 10, new Color(0, 0, 0, alpha * 0.6f));
        GameApp.endShapeRendering();
        GameApp.disableTransparency();
    }

    private static final float NPC_FADE_SPEED = 1.6f;        // fading in/out takes about 0.6 s

    private void drawNpc(Npc npc) {
        float dt = Gdx.graphics.getDeltaTime();
        light.update(npc.tint, npc.x + npcW(npc) / 2, npc.y + 4, dt);
        if (npc.alpha < npc.fadeTo) {
            npc.alpha = Math.min(npc.fadeTo, npc.alpha + dt * NPC_FADE_SPEED);
        } else if (npc.alpha > npc.fadeTo) {
            npc.alpha = Math.max(npc.fadeTo, npc.alpha - dt * NPC_FADE_SPEED);
        }
        GameApp.getSpriteBatch().setColor(npc.tint.r, npc.tint.g, npc.tint.b, npc.tint.a * npc.alpha);
        // standing, or walking like Ageha: step frames and a tiny bob on each step
        String texture = npc.spritePrefix + "_" + npc.facing + "_" + walkFrame(npc.moving, npc.walkDistance);
        GameApp.drawTexture(texture, npc.x, npc.y + walkBob(npc.moving, npc.walkDistance), npcW(npc), npcH(npc));
        GameApp.getSpriteBatch().setColor(Color.WHITE);
    }

    private void drawAgeha() {
        light.update(agehaTint, x + agehaW() / 2, y + 4, Gdx.graphics.getDeltaTime());
        GameApp.getSpriteBatch().setColor(agehaTint);
        // exact (not rounded) positions: he glides smoothly instead of jumping a pixel at a time
        GameApp.drawTexture(frameName(facing, walkFrame(walking, walkDistance)),
                x, y + walkBob(walking, walkDistance), agehaW(), agehaH);
        GameApp.getSpriteBatch().setColor(Color.WHITE);
    }

    /** Which walk frame to draw (1 and 3 = a step, 2 = standing), from the distance walked. */
    private int walkFrame(boolean moving, float distance) {
        if (!moving) {
            return 2;
        }
        int steps = (int) (distance / (STEP_LENGTH * size()));
        return WALK_CYCLE[steps % WALK_CYCLE.length];
    }

    /** How far up the sprite goes right now (a little up on each step). */
    private float walkBob(boolean moving, float distance) {
        if (!moving) {
            return 0;
        }
        float steps = distance / (STEP_LENGTH * size());
        return (float) Math.abs(Math.sin(steps * Math.PI / 2)) * BOB;
    }

    /** At night: a dark shade over the place, lighter in a soft pool around each lamp. */
    private void drawNight() {
        if (!TimeOfDay.isNight()) {
            return;
        }
        if (nightShade == null) {
            nightShade = makeNightShade();
        }
        GameApp.enableTransparency();
        GameApp.startSpriteRendering();
        GameApp.getSpriteBatch().draw(nightShade, 0, 0, getWorldWidth(), getWorldHeight());
        GameApp.endSpriteRendering();
        GameApp.disableTransparency();
    }

    /** A small picture of the darkness (one pixel per NIGHT_CELL world units), stretched smoothly. */
    private Texture makeNightShade() {
        int w = (int) (WORLD_W / NIGHT_CELL), h = (int) (WORLD_H / NIGHT_CELL);
        Pixmap shade = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        shade.setBlending(Pixmap.Blending.None);
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                float wx = (px + 0.5f) * NIGHT_CELL;
                float wy = WORLD_H - (py + 0.5f) * NIGHT_CELL;     // pixmap rows go down, the world goes up
                float alpha = NIGHT_DARKNESS * (1 - 0.85f * light.glowAt(wx, wy, NIGHT_LAMP_REACH));
                shade.drawPixel(px, py, Color.rgba8888(0.01f, 0.01f, 0.03f, alpha));   // almost black, a touch of blue
            }
        }
        Texture texture = new Texture(shade);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        shade.dispose();
        return texture;
    }

    // ---------- character sizes and facing ----------
    protected float npcH(Npc npc) {
        return agehaH * npc.heightFactor;
    }

    protected float npcW(Npc npc) {
        return npcH(npc) * npc.frameRatio;
    }

    /** The area around a character's body, for talking to them with E. */
    protected Rectangle npcArea(Npc npc) {
        return new Rectangle(npc.x + npcW(npc) * 0.2f, npc.y, npcW(npc) * 0.6f, npcH(npc) * 0.9f);
    }

    /** The character and Ageha turn to look at each other (for when he talks to them). */
    protected void turnToAgeha(Npc npc) {
        float dx = (x + agehaW() / 2) - (npc.x + npcW(npc) / 2);
        float dy = y - npc.y;
        if (Math.abs(dx) > Math.abs(dy)) {
            npc.facing = dx < 0 ? "left" : "right";
            facing = dx < 0 ? "right" : "left";
        } else {
            npc.facing = dy > 0 ? "up" : "down";
            facing = dy > 0 ? "down" : "up";
        }
    }

    // ---------- helpers ----------
    protected float agehaW() {
        return agehaH * 243f / 330f;
    }

    /** 1 at the normal size, smaller when Ageha is drawn smaller. */
    private float size() {
        return agehaH / NORMAL_H;
    }

    private String frameName(String direction, int number) {
        return "ageha_walk_" + direction + "_" + number;
    }

    // ---------- the black around the room (see RoomOutline) ----------
    private RoomOutline outline;

    /** True when this world point is on the black around the room (or off the picture). */
    protected boolean isOutsideRoom(float worldX, float worldY) {
        return outline.isOutside((int) (worldX / scale), (int) (imgH - (worldY - roomY) / scale));
    }

    /** Image pixels (x, y from the top) -> world rectangle (y going up). */
    protected Rectangle imageRect(float ix, float iy, float iw, float ih) {
        return new Rectangle(ix * scale, roomY + (imgH - iy - ih) * scale, iw * scale, ih * scale);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    protected Rectangle grow(Rectangle r, float amount) {
        return new Rectangle(r.x - amount, r.y - amount, r.width + amount * 2, r.height + amount * 2);
    }

    @Override
    public void hide() {
        GameApp.disposeTexture(textureName);
        for (String d : DIRECTIONS) {
            for (int n = 1; n <= 3; n++) {
                GameApp.disposeTexture(frameName(d, n));
                for (Npc npc : npcs) {
                    GameApp.disposeTexture(npc.spritePrefix + "_" + d + "_" + n);
                }
            }
        }
        for (String portrait : portraits) {
            GameApp.disposeTexture(portrait);
        }
        for (Prop prop : props) {
            GameApp.disposeTexture(prop.texture);
        }
        DialogueBox.disposeAssets();
        light.dispose();
        if (nightShade != null) {
            nightShade.dispose();
            nightShade = null;
        }
    }
}