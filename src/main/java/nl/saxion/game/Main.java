package nl.saxion.game;

import nl.saxion.game.eclosion.minigames.Floor1FlowerScreen;
import nl.saxion.game.eclosion.minigames.MiniGame2;
import nl.saxion.game.eclosion.rooms.AgehaRoomScreen;
import nl.saxion.game.eclosion.rooms.CorridorScreen;
import nl.saxion.game.eclosion.rooms.MainHallScreen;
import nl.saxion.game.eclosion.ui.TitleScreen;
import nl.saxion.gameapp.GameApp;

public class Main {
    public static void main(String[] args) {
        // Menus
        GameApp.addScreen("TitleScreen", new TitleScreen());

        // Floor 1 (ground floor): the places Ageha walks around in
        GameApp.addScreen("AgehaRoomScreen", new AgehaRoomScreen());
        GameApp.addScreen("CorridorScreen", new CorridorScreen());
        GameApp.addScreen("MainHallScreen", new MainHallScreen());

        // Minigames (started from Bluma's options in the main hall)
        GameApp.addScreen("Floor1FlowerScreen", new Floor1FlowerScreen());   // minigame 1 (from Dawon's branch)
        GameApp.addScreen("MiniGame2", new MiniGame2());                     // minigame 2 (from Giang's branch)

        // Start on the title screen (any key -> menu -> New Game -> Ageha's room)
        GameApp.start("Eclosion", 1280, 720, 60, false, "TitleScreen");
    }
}
