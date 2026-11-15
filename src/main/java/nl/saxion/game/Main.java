package nl.saxion.game;

import nl.saxion.game.yourgamename.YourGameScreen;
import nl.saxion.game.yourgamename.MainMenuScreen;
import nl.saxion.game.yourgamename.CorridorScreen;
import nl.saxion.game.yourgamename.MainHallScreen;
import nl.saxion.game.eclosion.floor1.Floor1FlowerScreen;
import nl.saxion.game.eclosion.FloorTransitionScreen;
import nl.saxion.game.eclosion.TitleScreen;
import nl.saxion.game.eclosion.NightCutsceneScreen;
import nl.saxion.game.eclosion.DayTransitionScreen;
import nl.saxion.game.eclosion.DoorTransitionScreen;
import nl.saxion.game.eclosion.EveningRoomScreen;
import nl.saxion.gameapp.GameApp;

public class Main {
    public static void main(String[] args) {
        // Add screens
        GameApp.addScreen("TitleScreen", new TitleScreen());
        GameApp.addScreen("MainMenuScreen", new MainMenuScreen());
        GameApp.addScreen("YourGameScreen", new YourGameScreen());       // Ageha's room (Chidera)
        GameApp.addScreen("CorridorScreen", new CorridorScreen());       // corridor (Chidera)
        GameApp.addScreen("MainHallScreen", new MainHallScreen());       // main hall (Chidera)
        GameApp.addScreen("FloorTransitionScreen", new FloorTransitionScreen());
        GameApp.addScreen("NightCutsceneScreen", new NightCutsceneScreen());
        GameApp.addScreen("Floor1FlowerScreen", new Floor1FlowerScreen());
        GameApp.addScreen("EveningRoomScreen", new EveningRoomScreen());
        GameApp.addScreen("DayTransitionScreen", new DayTransitionScreen());
        GameApp.addScreen("DoorTransitionScreen", new DoorTransitionScreen());

        // Demo: Title -> Ageha's room -> corridor -> main hall -> stairs -> night cutscene -> Floor 1 -> back in the room at night -> bed -> sleep -> End of Demo
        GameApp.start("Eclosion", 1280, 720, 60, false, "TitleScreen");
    }
}