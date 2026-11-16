package nl.saxion.game.eclosion.audio;

import nl.saxion.gameapp.GameApp;

/**
 * All sound effects of the game in one place.
 *
 *   Sfx.play(Sfx.DOOR_OPEN);             // normal volume
 *   Sfx.play(Sfx.KEY, 0.4f, 0.03f);      // own volume, a little random pitch
 *   Sfx.step();                          // a footstep
 *
 * Sounds are loaded once (the first time a screen calls Sfx.load()) and stay loaded.
 * Sfx.volume and Sfx.musicVolume are changed in the settings on the title screen.
 *
 * Where the files come from (see resources/audio/CREDITS.txt): steps, doors, E, menu and keypad
 * sounds are from Kenney's CC0 packs "RPG Audio" and "Interface Sounds", made softer and muffled
 * for the atmosphere; sfx_transition and the dialogue blip are made for this game.
 */
public final class Sfx {
    private static final String[] STEPS = {"sfx_step_1", "sfx_step_2", "sfx_step_3", "sfx_step_4"};          // carpet
    private static final String[] STAIR_STEPS = {"sfx_stair_1", "sfx_stair_2", "sfx_stair_3", "sfx_stair_4"}; // wooden steps
    public static final String DOOR_OPEN = "sfx_door_open";
    public static final String DOOR_CLOSE = "sfx_door_close";
    public static final String EXAMINE = "sfx_examine";       // pressing E to look at something
    public static final String MOVE = "sfx_move";             // moving between options in a menu / choice
    public static final String CONFIRM = "sfx_confirm";       // choosing an option
    public static final String TRANSITION = "sfx_transition"; // the screen fades to another place
    public static final String KEY = "sfx_key";               // keypad button
    public static final String KEY_WRONG = "sfx_key_wrong";
    public static final String KEY_RIGHT = "sfx_key_right";

    /** 0..1, for every sound effect (settings). */
    public static float volume = 1f;
    /** 0..1, for the music (settings). */
    public static float musicVolume = 1f;

    private static boolean loaded = false;
    private static int lastStep = -1;

    private Sfx() {
    }

    public static void load() {
        if (loaded) {
            return;
        }
        for (String step : STEPS) {
            GameApp.addSound(step, "audio/" + step + ".wav");
        }
        for (String step : STAIR_STEPS) {
            GameApp.addSound(step, "audio/" + step + ".wav");
        }
        for (String name : new String[]{DOOR_OPEN, DOOR_CLOSE, EXAMINE, MOVE, CONFIRM, TRANSITION, KEY, KEY_WRONG, KEY_RIGHT}) {
            GameApp.addSound(name, "audio/" + name + ".wav");
        }
        loaded = true;
    }

    /** How loud each sound is by default (quiet: the house is silent). */
    private static float baseVolume(String name) {
        switch (name) {
            case DOOR_OPEN:
                return 0.4f;
            case DOOR_CLOSE:
                return 0.4f;
            case EXAMINE:
                return 0.35f;
            case MOVE:
                return 0.3f;
            case CONFIRM:
                return 0.4f;
            case TRANSITION:
                return 0.35f;
            default:
                return 0.45f;
        }
    }

    public static void play(String name) {
        play(name, baseVolume(name), 0f);
    }

    /** pitchVariation 0.05 = up to 5% higher or lower each time (sounds less robotic). */
    public static void play(String name, float soundVolume, float pitchVariation) {
        if (!loaded || volume <= 0) {
            return;
        }
        float pitch = 1f + ((float) Math.random() * 2 - 1) * pitchVariation;
        GameApp.getSound(name).play(soundVolume * volume, pitch, 0f);
    }

    /** A footstep on the carpet: one of four sounds (never the same one twice), quiet, a bit different each time. */
    public static void step() {
        play(STEPS[nextStep()], 0.16f, 0.06f);
    }

    /** A footstep on the wooden stairs. */
    public static void stairStep() {
        play(STAIR_STEPS[nextStep()], 0.18f, 0.06f);
    }

    private static int nextStep() {
        int i;
        do {
            i = (int) (Math.random() * STEPS.length);
        } while (i == lastStep);
        lastStep = i;
        return i;
    }
}
