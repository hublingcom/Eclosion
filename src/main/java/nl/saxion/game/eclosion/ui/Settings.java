package nl.saxion.game.eclosion.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import nl.saxion.game.eclosion.audio.Bgm;
import nl.saxion.game.eclosion.audio.Sfx;
import nl.saxion.gameapp.GameApp;

/**
 * The settings from the title screen (music volume, sound effects volume, fullscreen),
 * remembered between games. libGDX keeps them in a small file in the user's home folder
 * (Windows: C:\Users\name\.prefs\eclosion-settings).
 */
public final class Settings {
    private static final String FILE = "eclosion-settings";
    private static boolean loaded = false;

    private Settings() {
    }

    /** Reads the saved settings once (at the start of the game) and applies them. */
    public static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Preferences prefs = Gdx.app.getPreferences(FILE);
        Sfx.musicVolume = prefs.getFloat("musicVolume", 1f);
        Sfx.volume = prefs.getFloat("soundVolume", 1f);
        Bgm.applyVolume();
        if (prefs.getBoolean("fullscreen", false) && !GameApp.isFullscreen()) {
            GameApp.switchToFullscreen();
        }
    }

    /**
     * Was the game in fullscreen last time? Read straight from the settings file, so the game can
     * open in fullscreen at once (before libGDX runs). Otherwise it would first flash a small
     * black window for a moment and then jump to fullscreen.
     */
    public static boolean savedFullscreen() {
        java.io.File file = new java.io.File(System.getProperty("user.home"), ".prefs/" + FILE);
        if (!file.exists()) {
            return false;
        }
        try (java.io.InputStream in = new java.io.FileInputStream(file)) {
            java.util.Properties saved = new java.util.Properties();
            saved.loadFromXML(in);
            return Boolean.parseBoolean(saved.getProperty("fullscreen", "false"));
        } catch (Exception e) {
            return false;                // unreadable file: just start in a window
        }
    }

    /** Call after a setting changed. */
    public static void save() {
        Preferences prefs = Gdx.app.getPreferences(FILE);
        prefs.putFloat("musicVolume", Sfx.musicVolume);
        prefs.putFloat("soundVolume", Sfx.volume);
        prefs.putBoolean("fullscreen", GameApp.isFullscreen());
        prefs.flush();
    }
}
