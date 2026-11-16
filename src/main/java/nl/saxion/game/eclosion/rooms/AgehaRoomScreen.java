package nl.saxion.game.eclosion.rooms;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import nl.saxion.game.eclosion.explore.ExploreScreen;
import nl.saxion.game.eclosion.ui.SafeKeypad;

/**
 * Ageha's room (walking, light, E to look at things and doors all come from ExploreScreen).
 * The safe opens a keypad: the player types the 6-digit code (SAFE_CODE).
 * The door leads to the corridor.
 *
 * Positions are IMAGE pixels of Agehas_room_refined.png (1670 x 942).
 */
public class AgehaRoomScreen extends ExploreScreen {

    // ---------- safe ----------
    private static final String SAFE_CODE = "123456";       // placeholder: put the real 6-digit code here
    private final SafeKeypad keypad = new SafeKeypad(SAFE_CODE);
    private boolean safeOpen = false;
    private boolean safeSeen = false;
    private boolean keypadAfterText = false;                 // open the keypad when the current text ends

    public AgehaRoomScreen() {
        super("ageha_room", "textures/rooms/ageha_room.png", 1670, 942);
    }

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

        // Story texts. Window and wardrobe: still placeholder texts.
        addItem("Bed", bed,
                lines("I don't feel like resting now..."),
                lines("I don't feel like resting now..."));
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
        addDoor("Door", imageRect(888, 690, 112, 40), "CorridorScreen");
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
        } else {
            placeFeetAtImage(604, 467, "right"); // start of the game: by the bed, next to the nightstand
        }
    }

    // ---------- mirror ----------
    private boolean mirrorSeen = false;

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

    @Override
    protected void onTextFinished() {
        if (keypadAfterText) {
            keypadAfterText = false;
            keypad.open();
        }
    }

    @Override
    protected boolean overlayOpen() {
        return keypad.isOpen();
    }

    @Override
    protected void updateOverlay(float delta) {
        SafeKeypad.Result result = keypad.update(delta, getMouseX(), getMouseY(), getWorldWidth(), getWorldHeight());
        if (result == SafeKeypad.Result.CORRECT) {
            safeOpen = true;
            dialogue.start(lines("*click*", "The safe is open."));   // placeholder: what is inside?
        }
    }

    @Override
    protected void renderOverlay() {
        keypad.render(getMouseX(), getMouseY(), getWorldWidth(), getWorldHeight());
    }
}
