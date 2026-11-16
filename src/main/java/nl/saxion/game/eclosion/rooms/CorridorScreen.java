package nl.saxion.game.eclosion.rooms;

import com.badlogic.gdx.math.Rectangle;
import nl.saxion.game.eclosion.explore.ExploreScreen;

/**
 * The corridor outside Ageha's room. The door in the middle leads back to his room.
 *
 * Positions are IMAGE pixels of the corridor picture (1670 x 941).
 */
public class CorridorScreen extends ExploreScreen {

    public CorridorScreen() {
        super("corridor", "textures/rooms/corridor.png", 1670, 941);
        agehaH = 82;   // the doors here are about 96 high: Ageha must be a bit shorter than a door
    }

    @Override
    protected void buildRoom() {
        floor = imageRect(334, 524, 1012, 164);
        // the narrow passage in the right wall leads to the main hall
        extraFloor.add(imageRect(1340, 523, 130, 93));

        Rectangle phoneTable = imageRect(450, 516, 84, 24);
        solids.add(phoneTable);

        // Story texts. The phone is still a placeholder (dialing comes later).
        addItem("Phone", imageRect(447, 465, 90, 75),
                lines("An old phone.", "There's no dial tone."),
                lines("Still silent."));
        addItem("Boarded door", imageRect(546, 395, 106, 130),
                lines("A horrible stench comes behind the door"),
                lines("A horrible stench comes behind the door"));
        addItem("Window", imageRect(1050, 395, 222, 130),
                lines("I can't see anything because of the mist..."),
                lines("I can't see anything because of the mist..."));
        addDoor("Door to my room", imageRect(905, 397, 96, 130), "AgehaRoomScreen");
        addExit(imageRect(1450, 523, 20, 93), "MainHallScreen");    // the end of the passage leads to the main hall
    }

    @Override
    protected void placePlayer() {
        if ("MainHallScreen".equals(arrivedFrom)) {
            placeFeetAtImage(1395, 585, "left");  // coming back from the main hall, in the passage
        } else {
            placeFeetAtImage(952, 545, "down");   // just outside the door of his room
        }
    }
}
