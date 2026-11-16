package nl.saxion.game.eclosion.explore;

import com.badlogic.gdx.math.Rectangle;
import nl.saxion.game.eclosion.ui.DialogueLine;

/**
 * Something Ageha can look at with E: either lines of his inner voice (first time / again),
 * or an action for special things (doors, the safe, talking to someone).
 * Made with ExploreScreen.addItem / addSpecial / addDoor.
 */
public class Interactable {
    final String name;
    final Rectangle area;           // world units
    final DialogueLine[] firstTime;
    final DialogueLine[] again;
    final Runnable action;
    boolean seen = false;

    Interactable(String name, Rectangle area, DialogueLine[] firstTime, DialogueLine[] again, Runnable action) {
        this.name = name;
        this.area = area;
        this.firstTime = firstTime;
        this.again = again;
        this.action = action;
    }
}
