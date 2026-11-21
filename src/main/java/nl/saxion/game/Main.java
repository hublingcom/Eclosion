package nl.saxion.game;

import nl.saxion.game.eclosion.explore.TimeOfDay;
import nl.saxion.game.eclosion.items.Items;
import nl.saxion.game.eclosion.minigames.Floor1FlowerScreen;
import nl.saxion.game.eclosion.minigames.MiniGame2;
import nl.saxion.game.eclosion.rooms.AgehaRoomScreen;
import nl.saxion.game.eclosion.rooms.CorridorScreen;
import nl.saxion.game.eclosion.rooms.MainHallScreen;
import nl.saxion.game.eclosion.ui.NightClock;
import nl.saxion.game.eclosion.ui.Settings;
import nl.saxion.game.eclosion.ui.TitleScreen;
import nl.saxion.gameapp.GameApp;

public class Main {
    public static void main(String[] args) {
        // Menus
        GameApp.addScreen("TitleScreen", new TitleScreen());

        addStoryScreens();

        // Start on the title screen (any key -> menu -> New Game -> Ageha's room)
        // opens in fullscreen right away if that was saved (no small black window first)
        GameApp.start("Eclosion", 1280, 720, 60, Settings.savedFullscreen(), "TitleScreen");
    }

    /**
     * All the places and minigames, made new. Called at the start and again on New Game,
     * so a new game really starts from the beginning (Niri at the door again, nothing seen yet, day).
     */
    public static void addStoryScreens() {
        TimeOfDay.startDay();
        NightClock.reset();     // day 1 again (Dawon)
        Items.reset();          // no memory items found yet (Dawon)

        // Floor 1 (ground floor): the places Ageha walks around in
        GameApp.addScreen("AgehaRoomScreen", new AgehaRoomScreen());
        GameApp.addScreen("CorridorScreen", new CorridorScreen());
        GameApp.addScreen("MainHallScreen", new MainHallScreen());

        // Minigames (started from Bluma's options in the main hall)
        GameApp.addScreen("Floor1FlowerScreen", new Floor1FlowerScreen());   // minigame 1 (from Dawon's branch)
        GameApp.addScreen("MiniGame2", new MiniGame2());                     // minigame 2 (from Giang's branch)
    }
}