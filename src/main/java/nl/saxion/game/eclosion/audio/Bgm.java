package nl.saxion.game.eclosion.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

/**
 * The background music. Only one piece plays at a time.
 *
 *   Bgm.play(Bgm.FLOOR1, 0.5f);   // in show(): starts it, or lets it go on if it's already playing
 *
 * Walking from room to room keeps the same music going without starting over; a screen with
 * other music (title, a minigame) simply plays its own and the old one stops.
 * The volume follows Sfx.musicVolume (settings).
 *
 * Music (see resources/audio/CREDITS.txt): title = team, floor 1 = "Echoes of the Past" (isaiah658, CC0),
 * floor 1 at night = the same piece made slower, lower, muffled and darker for this game,
 * minigame 1 = "PS2 horror / mystery ambient theme" (Tarush Singhal, CC0).
 */
public final class Bgm {
    public static final String TITLE = "audio/title_bgm.ogg";
    public static final String FLOOR1 = "audio/music_floor1_echoes_of_the_past.wav";
    public static final String FLOOR1_NIGHT = "audio/music_floor1_night.wav";
    public static final String MINIGAME1 = "audio/music_minigame1_ps2_ambient.mp3";

    private static Music current;
    private static String currentPath;
    private static float baseVolume = 1f;
    private static float fade = 1f;          // 1 = full, goes to 0 while fading out
    private static boolean fadingOut = false;

    private Bgm() {
    }

    /** Plays this music on a loop (keeps playing if it's already the one playing). */
    public static void play(String path, float volume) {
        baseVolume = volume;
        fade = 1f;
        fadingOut = false;
        stopWhenSilent = false;
        if (current != null && path.equals(currentPath)) {
            applyVolume();
            if (!current.isPlaying()) {
                current.play();
            }
            return;
        }
        stop();
        current = Gdx.audio.newMusic(Gdx.files.internal(path));
        currentPath = path;
        current.setLooping(true);
        applyVolume();
        current.play();
    }

    /** Slowly goes silent (call update() every frame while it fades). */
    public static void fadeOut() {
        fadingOut = true;
    }

    /** Slowly goes silent and then stops (the next music starts from its beginning). */
    public static void fadeOutAndStop() {
        fadingOut = true;
        stopWhenSilent = true;
    }

    private static boolean stopWhenSilent = false;

    public static void update(float delta) {
        if (current != null && fadingOut && fade > 0) {
            fade = Math.max(0f, fade - delta / 0.75f);
            applyVolume();
        }
        if (current != null && fadingOut && fade <= 0 && stopWhenSilent) {
            stop();
        }
    }

    private static float dim = 1f;                  // 1 = normal, less while the pause menu is open

    /** Quieter music while the pause menu is open. */
    public static void setDimmed(boolean dimmed) {
        dim = dimmed ? 0.35f : 1f;
        applyVolume();
    }

    /** Call after the music volume in the settings changed. */
    public static void applyVolume() {
        if (current != null) {
            current.setVolume(baseVolume * fade * dim * Sfx.musicVolume);
        }
    }

    public static void stop() {
        if (current != null) {
            current.stop();
            current.dispose();
            current = null;
            currentPath = null;
        }
    }
}
