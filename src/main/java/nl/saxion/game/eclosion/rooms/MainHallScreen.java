package nl.saxion.game.eclosion.rooms;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import nl.saxion.game.eclosion.ui.DialogueLine;
import nl.saxion.game.eclosion.explore.ExploreScreen;
import nl.saxion.game.eclosion.explore.Npc;

/**
 * The main hall: four exits, the staircase top right, the window top left.
 * Ageha comes in through the hallway on the left. Bluma stands just below the window.
 *
 * Positions are IMAGE pixels of the main hall picture (1669 x 942).
 * Items to look at: window, TV, wine, cat bed and bowls. Bluma: talk (minigames).
 */
public class MainHallScreen extends ExploreScreen {

    public MainHallScreen() {
        super("main_hall", "textures/rooms/main_hall.png", 1669, 942);
        agehaH = 92;   // the hall is drawn smaller than his room
        portraits.add(BLUMA_PORTRAIT);
        portraits.add("ageha_neutral");
    }

    private Npc bluma;
    private boolean talking = false;

    /**
     * Bluma's portrait in dialogue. For now always the neutral one; the other expressions
     * are ready in textures/portraits: bluma_surprised, bluma_sad, bluma_worried.
     */
    private static final String BLUMA_PORTRAIT = "bluma_neutral";

    /**
     * Talking to Bluma: with portraits, Ageha on the LEFT and Bluma on the RIGHT.
     * She only says "...", then the three options appear.
     */
    private void talkToBluma() {
        turnToAgeha(bluma);
        talking = true;
        dialogue.setPortraits("ageha_neutral", BLUMA_PORTRAIT);   // (left, right)
        dialogue.start(bluma(BLUMA_PORTRAIT, "..."));
        // under her line, three options
        dialogue.ask(this::blumaChoice, "Minigame 1", "Minigame 2", "Exit");
    }

    private void blumaChoice(int choice) {
        endTalk();
        if (choice == 0) {
            startMinigame(1);
        } else if (choice == 1) {
            startMinigame(2);
        }
        // choice 2 = Exit: the conversation just ends
    }

    /** Fades over to the minigame. When it is done, Ageha comes back here, in front of Bluma. */
    private void startMinigame(int number) {
        if (number == 1) {
            goTo("Floor1FlowerScreen");   // Minigame 1: "Queen of the Night" (Dawon)
        } else {
            goTo("MiniGame2");            // Minigame 2: find the odd colour (Giang)
        }
    }

    /** Name used by the minigames when they send Ageha back to the hall. */
    public static final String FROM_MINIGAME = "Minigame";

    /** Back to normal: no portraits (inner voice), Bluma looks forward again. */
    private void endTalk() {
        talking = false;
        dialogue.setPortraits(null, null);
        bluma.facing = "down";
    }

    private DialogueLine bluma(String portrait, String text) {
        return new DialogueLine("Bluma", portrait, DialogueLine.Side.RIGHT, text);
    }

    @Override
    protected void onTextFinished() {
        if (talking) {
            endTalk();                           // done talking: she faces forward again
        }
    }

    @Override
    protected void buildRoom() {
        floor = imageRect(122, 320, 1426, 513);
        extraFloor.add(imageRect(0, 414, 124, 100));     // hallway on the left (where Ageha comes in)
        addExit(imageRect(0, 414, 22, 100), "CorridorScreen");   // the left hallway goes back to the corridor

        // The other three hallways: Ageha can walk into them, but at the end they are closed for now
        // (PLACEHOLDER: later addExit(zone, "TheNewScreen") instead of addExitNotMadeYet)
        String notReady = "(This part is not ready yet.)";
        extraFloor.add(imageRect(779, 0, 111, 322));             // top hallway
        addExitNotMadeYet(imageRect(779, 0, 111, 130), notReady); // (stops him while his head is still on screen)
        extraFloor.add(imageRect(779, 831, 111, 111));           // bottom hallway
        addExitNotMadeYet(imageRect(779, 918, 111, 24), notReady);
        extraFloor.add(imageRect(1546, 414, 123, 101));          // right hallway
        addExitNotMadeYet(imageRect(1638, 414, 31, 101), notReady);

        // Furniture blocks only where it stands on the floor (its footprint), so Ageha can walk
        // close around it. When he walks behind something, the cut-out picture of it (prop) is
        // drawn over him, so he never looks like he stands on top of it.
        solids.add(imageRect(140, 682, 133, 44));     // TV cabinet
        solids.add(imageRect(394, 690, 74, 34));      // small table with the wine bottle and glass
        solids.add(imageRect(503, 684, 94, 52));      // armchair
        solids.add(imageRect(234, 772, 264, 54));     // sofa
        solids.add(imageRect(786, 604, 98, 38));      // round table with the paper
        solids.add(imageRect(1207, 740, 106, 52));    // pet bed
        solids.add(imageRect(1338, 756, 82, 19));     // bowls

        // cut-outs: picture position (x, y, w, h) and the front edge on the floor
        addProp("hall_tv", 134, 550, 146, 184, 726);
        addProp("hall_wine_table", 382, 614, 96, 116, 724);
        addProp("hall_armchair", 494, 618, 112, 126, 736);
        addProp("hall_sofa", 226, 724, 280, 108, 826);
        addProp("hall_round_table", 774, 532, 117, 116, 642);
        addProp("hall_pet_bed", 1199, 714, 122, 86, 792);
        // Things to look at with E (story texts). Areas are image pixels;
        // the window's area reaches down to the floor, so Ageha can stand under it and press E.
        addItem("Window", imageRect(150, 130, 228, 200),
                lines("The same mist..."),
                lines("..."));
        addItem("TV", imageRect(134, 550, 146, 184),
                lines("An old television...", "The screen doesn't seem to work"),
                lines("It won't work..."));
        addItem("Wine and glass", imageRect(382, 614, 96, 116),
                lines("A pleasant aroma comes from the bottle."),
                lines("I don't have any use for this right now..."));
        addItem("Cat bed", imageRect(1199, 714, 122, 86),
                lines("A fluffy bed for a furred friend"),
                lines("A fluffy bed for a furred friend"));
        addItem("Cat food and water", imageRect(1330, 740, 100, 45),
                lines("The bowls look full", "It must be the Mother who refilled the bowls"),
                lines("The bowls look full"));

        // The chandelier and its chain hang from the ceiling: always drawn over Ageha and Bluma
        // (front edge far below the picture = always in front)
        addProp("hall_chandelier", 760, 0, 150, 505, 99999);

        // Staircase: you go in at the left end of the lower part, walk right, then up the steps.
        // The steps are drawn at x 1210..1538 (lower part, y 337..400) and 1468..1538 (going up,
        // y 92..337). On them only the middle of Ageha's feet must be on a step, so he can walk
        // on the whole width; the walls around stop him as soon as his feet leave the steps.
        solids.add(imageRect(1200, 318, 268, 19));    // wall edge above the lower part
        solids.add(imageRect(1232, 400, 318, 14));    // side of the stairs: no climbing in from below
        stepFloor.add(imageRect(1212, 341, 324, 52));  // lower part (left to right)
        stepFloor.add(imageRect(1472, 96, 62, 250));   // the steps going up along the right wall
        // top of the stairs: later the second floor (PLACEHOLDER until that screen exists)
        addExitNotMadeYet(imageRect(1468, 92, 70, 14), "(The second floor is not made yet.)");

        // Bluma: frames are 284 x 428, she is 412 high inside them. Feet just below the window.
        bluma = addNpc("bluma_walk", 284, 428, 412, 265, 350, "down");
        // E near her: Ageha walks up to her, then they talk
        addSpecial("Bluma", npcArea(bluma), () -> walkUpTo(bluma, this::talkToBluma));
    }

    @Override
    protected void addLights() {
        // candles of the chandelier: a soft warm glow on the floor under it
        Rectangle candles = imageRect(835, 540, 1, 1);
        light.addLight(candles.x, candles.y, 190, new Color(0.22f, 0.13f, 0.04f, 0f));
    }

    @Override
    protected void placePlayer() {
        if (FROM_MINIGAME.equals(arrivedFrom)) {
            placeBeside(bluma);                   // back from a minigame: standing next to Bluma
        } else {
            placeFeetAtImage(70, 478, "right");   // in the left hallway (coming from the corridor), walking in
        }
    }
}
