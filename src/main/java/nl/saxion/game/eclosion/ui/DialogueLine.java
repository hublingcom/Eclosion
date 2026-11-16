package nl.saxion.game.eclosion.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * One box of dialogue: who speaks, which portrait to show, on which side, and what they say.
 * portrait is the texture name given to GameApp.addTexture (null = keep the portrait that is already there).
 *
 * DialogueLine.together(...) puts several speakers in the same box, one after the other.
 */
public class DialogueLine {
    public enum Side { LEFT, RIGHT, NONE }

    public final String speaker;
    public final String portrait;
    public final Side side;
    public final String text;

    /** Only used by together(): the lines shown in one box. Empty for a normal line. */
    private final List<DialogueLine> parts = new ArrayList<>();

    public DialogueLine(String speaker, String portrait, Side side, String text) {
        this.speaker = speaker;
        this.portrait = portrait;
        this.side = side;
        this.text = text;
    }

    /** Short version: the speaker stands on the left. */
    public DialogueLine(String speaker, String portrait, String text) {
        this(speaker, portrait, Side.LEFT, text);
    }

    /** Narration: no name. */
    public static DialogueLine narration(String text) {
        return new DialogueLine("", null, Side.NONE, text);
    }

    /** Several speakers in the same box, for example a quick back-and-forth. */
    public static DialogueLine together(DialogueLine... lines) {
        DialogueLine box = new DialogueLine("", null, Side.NONE, "");
        for (DialogueLine line : lines) {
            box.parts.add(line);
        }
        return box;
    }

    /** The lines shown in this box: the parts of a together() box, or just this line. */
    public List<DialogueLine> getParts() {
        if (parts.isEmpty()) {
            return List.of(this);
        }
        return parts;
    }
}
