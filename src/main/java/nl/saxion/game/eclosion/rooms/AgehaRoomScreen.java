package nl.saxion.game.eclosion.rooms;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import nl.saxion.game.eclosion.audio.Sfx;
import nl.saxion.game.eclosion.explore.ExploreScreen;
import nl.saxion.game.eclosion.explore.Npc;
import nl.saxion.game.eclosion.explore.TimeOfDay;
import nl.saxion.game.eclosion.ui.DialogueLine;
import nl.saxion.game.eclosion.ui.SafeKeypad;

import static nl.saxion.game.eclosion.rooms.BedCutscene.AWAKE;
import static nl.saxion.game.eclosion.rooms.BedCutscene.BED_WITH_AGEHA;
import static nl.saxion.game.eclosion.rooms.BedCutscene.EMPTY_BED;

/**
 * Ageha's room, where the game starts (walking, light, E to look at things all come from ExploreScreen).
 *
 * What happens here:
 *   1. the opening cutscene (Ageha asleep in bed, see {@link BedCutscene});
 *   2. the first time he uses the door, Niri is standing behind it (a short talk, then Niri walks away);
 *   3. after that the door leads to the corridor;
 *   4. after a minigame it is night (see TimeOfDay): E at the bed and he sleeps until the morning.
 * The safe opens a keypad: the player types the 6-digit code (SAFE_CODE).
 *
 * Positions are IMAGE pixels of the room picture (1670 x 942).
 */
public class AgehaRoomScreen extends ExploreScreen implements BedCutscene.Room {

    // ---------- portraits ----------
    private static final String AGEHA_PORTRAIT = "ageha_neutral";
    private static final String NIRI_LAUGH = "niri_laugh";
    private static final String NIRI_SAD = "niri_sad";

    // ---------- the cutscenes in bed (the opening, going to sleep) ----------
    private final BedCutscene bedScene = new BedCutscene(this);

    // ---------- Niri at the door ----------
    private static final float DOOR_X = 944;                 // image x of the middle of the door
    private static final float NIRI_OUTSIDE_Y = 735;         // image y: in the door frame (he fades in and out here)
    private static final float NIRI_INSIDE_Y = 698;          // image y: just inside the door
    private static final float NIRI_SPEED = 105;             // world units per second (calm walking)
    private static final float NIRI_APPEARS_AFTER = 0.2f;    // seconds after the door opens
    private static final float STEP_BACK = 64;               // how far Ageha steps back (world units)
    private static final float STEP_BACK_TIME = 1.0f;        // he also moves sideways to stand right in front of the door
    private static final float BEAT_BEFORE_TALK = 0.35f;     // both stand still a moment before Niri speaks

    private enum DoorScene { NONE, OPENING, TALKING, LEAVING }

    private Npc niri;
    private DoorScene doorScene = DoorScene.NONE;
    private boolean niriMet = false;         // after the talk the door simply leads to the corridor
    private boolean niriVisible = false;
    private float doorTimer = 0;
    private float stepStartX, stepStartY;
    private float standingStill = 0;                         // how long both have been standing in place

    // ---------- mirror ----------
    private boolean mirrorSeen = false;

    // ---------- safe ----------
    private static final String SAFE_CODE = "123456";       // placeholder: put the real 6-digit code here
    private final SafeKeypad keypad = new SafeKeypad(SAFE_CODE);
    private boolean safeOpen = false;
    private boolean safeSeen = false;
    private boolean keypadAfterText = false;                 // open the keypad when the current text ends

    public AgehaRoomScreen() {
        super("ageha_room", "textures/rooms/ageha_room.png", 1670, 942);
        agehaH = 98;   // his door is drawn 17% wider than the corridor doors (see CorridorScreen)
        portraits.add(AGEHA_PORTRAIT);
        portraits.add(NIRI_LAUGH);
        portraits.add(NIRI_SAD);
    }

    // ---------- the room ----------
    @Override
    protected void buildRoom() {
        floor = imageRect(396, 298, 892, 405);

        Rectangle bed = imageRect(423, 300, 140, 228);
        Rectangle nightstand = imageRect(578, 300, 80, 72);
        Rectangle desk = imageRect(845, 290, 202, 95);
        Rectangle chair = imageRect(910, 335, 58, 85);
        Rectangle wardrobe = imageRect(1097, 290, 137, 30);
        Rectangle safe = imageRect(1240, 395, 50, 112);
        solids.add(bed);
        solids.add(nightstand);
        solids.add(desk);
        solids.add(chair);
        solids.add(wardrobe);
        solids.add(safe);

        // The drawn bed lies over the bed in the room picture. It is flat on the floor,
        // so it is always drawn under Ageha (base line at the top of the bed).
        // The other pictures are the same bed with Ageha in it (start of the game):
        // asleep, eyes half open, eyes open. Only one bed picture is shown at a time.
        addProp(EMPTY_BED, 423, 311, 140, 216, 300);
        for (String picture : BED_WITH_AGEHA) {
            addProp(picture, 423.0f, 305.9f, 140.2f, 221.9f, 300);
        }

        // Story texts. Window and wardrobe: still placeholder texts.
        addSpecial("Bed", bed, this::useBed);
        addItem("MP3 player", nightstand,
                lines("My mp3 player...", "it's been a while since I last used it."),
                lines("My mp3 player...", "it's been a while since I last used it."));
        addSpecial("Mirror", imageRect(722, 160, 58, 140), this::lookInMirror);
        addItem("Window", imageRect(808, 145, 36, 150),
                lines("The curtains are closed.", "Even the light coming through them is red."),
                lines("I don't want to open them."));
        // PLACEHOLDER: the journal is still in development
        addItem("Journal", desk,
                lines("(The journal is not made yet.)"),
                lines("(The journal is not made yet.)"));
        addItem("Wardrobe", imageRect(1097, 162, 137, 158),
                lines("My clothes.", "They all look the same."),
                lines("Same clothes as yesterday."));
        addSpecial("Safe", safe, this::useSafe);
        // The first time: Niri is behind the door. After that the door leads to the corridor.
        addSpecial("Door", imageRect(888, 690, 112, 40), this::useDoor);
        niri = addNiri();
    }

    /** His room is quiet: the floor theme only starts when he steps out into the corridor. */
    @Override
    protected String music() {
        return null;
    }

    @Override
    protected void addLights() {
        // the desk lamp: a warm glow on the floor in front of the right side of the desk
        Rectangle lampSpot = imageRect(1018, 340, 1, 1);
        light.addLight(lampSpot.x, lampSpot.y, 170, new Color(0.30f, 0.20f, 0.06f, 0f));
    }

    @Override
    protected void placePlayer() {
        if ("CorridorScreen".equals(arrivedFrom)) {
            placeFeetAtImage(944, 672, "up");    // just inside the door, coming in from the corridor
            outOfBed();
        } else {
            placeFeetAtImage(604, 467, "right"); // start of the game: by the bed, next to the nightstand ...
            agehaHidden = true;                  // ... but first he is still asleep in the bed
            bedScene.startOpening();
        }
    }

    @Override
    public void show() {
        BedCutscene.loadFonts();
        super.show();
    }

    @Override
    public void hide() {
        super.hide();
        BedCutscene.disposeFonts();
    }

    // ---------- BedCutscene.Room ----------
    @Override
    public void showBedPicture(String picture) {
        setPropVisible(EMPTY_BED, picture.equals(EMPTY_BED));
        for (String p : BED_WITH_AGEHA) {
            setPropVisible(p, picture.equals(p));
        }
    }

    @Override
    public void say(String text) {
        dialogue.start(lines(text));
    }

    @Override
    public void lieDown() {
        agehaHidden = true;
        showBedPicture(AWAKE);
    }

    @Override
    public void nightIsOver() {
        TimeOfDay.startDay();
    }

    @Override
    public void outOfBed() {
        agehaHidden = false;
        showBedPicture(EMPTY_BED);
    }

    // ---------- the bed ----------
    /** By day he doesn't want to rest; at night he goes to sleep (and the next day starts). */
    private void useBed() {
        if (TimeOfDay.isNight()) {
            bedScene.startSleeping();
        } else {
            Sfx.play(Sfx.EXAMINE);
            dialogue.start(lines("I don't feel like resting now..."));
        }
    }

    // ---------- Niri at the door (after the wake-up) ----------
    // The door opens, Ageha steps back (still looking at the door), Niri steps into the doorway
    // and they talk a little. Then Niri walks out again and the door leads to the corridor.
    private void useDoor() {
        if (niriMet || TimeOfDay.isNight()) {          // Niri never comes to the door at night
            goThroughDoor("CorridorScreen");
            return;
        }
        doorScene = DoorScene.OPENING;
        doorTimer = 0;
        stepStartX = x;
        stepStartY = y;
        standingStill = 0;
        facing = "down";
        Sfx.play(Sfx.DOOR_OPEN);
    }

    private void updateDoorScene(float delta) {
        doorTimer += delta;
        if (doorScene == DoorScene.OPENING) {
            stepBack();
            if (doorTimer >= NIRI_APPEARS_AFTER) {
                niriComesIn(delta);
            }
        } else if (doorScene == DoorScene.LEAVING) {
            boolean out = moveNpcToward(niri, DOOR_X, NIRI_OUTSIDE_Y, NIRI_SPEED, delta);   // out through the door
            if (out && npcFadedOut(niri)) {                                                  // ... and fully faded
                hideNpc(niri);
                niriVisible = false;
                niriMet = true;
                doorScene = DoorScene.NONE;
            }
        }
    }

    /**
     * Ageha takes a small step back, still facing the door, and ends right in front of it
     * (so he and Niri stand face to face, wherever he pressed E).
     */
    private void stepBack() {
        float t = Math.min(1, doorTimer / STEP_BACK_TIME);
        float eased = t * t * (3 - 2 * t);                    // starts and ends softly
        float endX = imageRect(DOOR_X, 0, 0, 0).x - agehaW() / 2;
        float sideways = (endX - stepStartX) * eased;
        x = stepStartX + sideways;
        y = stepStartY + STEP_BACK * eased;
        facing = "down";
        if (t < 1) {
            // his feet move while he steps back
            showAgehaWalking((float) Math.sqrt(sideways * sideways + STEP_BACK * eased * STEP_BACK * eased));
        }
    }

    /** Niri appears in the door frame and steps into the doorway; then the talk starts. */
    private void niriComesIn(float delta) {
        if (!niriVisible) {
            showNpcAtImage(niri, DOOR_X, NIRI_OUTSIDE_Y, "up");
            fadeNpcIn(niri);                                  // he appears in the door frame, not out of nowhere
            niriVisible = true;
        }
        boolean inside = moveNpcToward(niri, DOOR_X, NIRI_INSIDE_Y, NIRI_SPEED, delta);
        if (inside && doorTimer >= STEP_BACK_TIME) {
            niri.facing = "up";                               // Niri stays in the doorway, looking at Ageha
            standingStill += delta;
        }
        if (standingStill >= BEAT_BEFORE_TALK) {
            doorScene = DoorScene.TALKING;
            dialogue.setPortraits(AGEHA_PORTRAIT, NIRI_LAUGH);   // (left, right)
            dialogue.start(
                    niri(NIRI_LAUGH, "Ageha!"),
                    niri(NIRI_LAUGH, "Why don't you follow me to the main hall?"),
                    ageha("..."),
                    niri(NIRI_SAD, "..."),
                    ageha("Fine."),
                    niri(NIRI_LAUGH, "Okay!!"));                // Niri cheers up again
        }
    }

    private DialogueLine niri(String portrait, String text) {
        return new DialogueLine("Niri", portrait, DialogueLine.Side.RIGHT, text);
    }

    private DialogueLine ageha(String text) {
        return new DialogueLine("Ageha", AGEHA_PORTRAIT, DialogueLine.Side.LEFT, text);
    }

    // ---------- mirror ----------
    /** The first time: a cutscene (PLACEHOLDER until it is made). After that: nothing new. */
    private void lookInMirror() {
        if (!mirrorSeen) {
            mirrorSeen = true;
            dialogue.start(lines("(The mirror cutscene is not made yet.)"));
        } else {
            dialogue.start(lines("Nothing new."));
        }
    }

    // ---------- safe ----------
    /**
     * The first time: the monologue, and right after it the keypad opens.
     * After that: the keypad opens straight away.
     */
    private void useSafe() {
        if (safeOpen) {
            dialogue.start(lines("The safe is open."));
            return;
        }
        if (!safeSeen) {
            safeSeen = true;
            dialogue.start(lines("The safe is locked...", "it needs a 6-digit code to open."));
            keypadAfterText = true;          // after the text, the player can type the code
        } else {
            keypad.open();
        }
    }

    // ---------- ExploreScreen hooks ----------
    @Override
    protected void onTextFinished() {
        if (doorScene == DoorScene.TALKING) {
            dialogue.setPortraits(null, null);   // back to the inner voice
            doorScene = DoorScene.LEAVING;       // Niri walks out, Ageha can follow
            niri.facing = "down";
            fadeNpcOut(niri);
            return;
        }
        if (bedScene.isWaitingForText()) {
            bedScene.textFinished();              // the cutscene goes on after the "..."
            return;
        }
        if (keypadAfterText) {
            keypadAfterText = false;
            keypad.open();
        }
    }

    /** While this is true the player can't walk (during the "..." of the cutscene the text box takes over). */
    @Override
    protected boolean overlayOpen() {
        return (bedScene.isPlaying() && !bedScene.isWaitingForText())
                || keypad.isOpen()
                || (doorScene != DoorScene.NONE && doorScene != DoorScene.TALKING);
    }

    @Override
    protected void updateOverlay(float delta) {
        if (bedScene.isPlaying()) {
            bedScene.update(delta);
            return;
        }
        if (doorScene != DoorScene.NONE) {
            updateDoorScene(delta);
            return;
        }
        SafeKeypad.Result result = keypad.update(delta, getMouseX(), getMouseY(), getWorldWidth(), getWorldHeight());
        if (result == SafeKeypad.Result.CORRECT) {
            safeOpen = true;
            dialogue.start(lines("*click*", "The safe is open."));   // placeholder: what is inside?
        }
    }

    @Override
    protected void renderOverlay() {
        bedScene.render(getWorldWidth(), getWorldHeight());
        keypad.render(getMouseX(), getMouseY(), getWorldWidth(), getWorldHeight());
    }
}
