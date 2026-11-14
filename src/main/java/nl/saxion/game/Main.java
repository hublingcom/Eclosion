package nl.saxion.game;

import nl.saxion.game.yourgamename.YourGameScreen;
import nl.saxion.game.yourgamename.MainMenuScreen;
import nl.saxion.game.yourgamename.RoomScreen;
import nl.saxion.game.eclosion.floor1.Floor1FlowerScreen;
import nl.saxion.game.eclosion.FloorTransitionScreen;
import nl.saxion.game.eclosion.TitleScreen;
import nl.saxion.game.eclosion.NightCutsceneScreen;
import nl.saxion.game.eclosion.DialogueDemoScreen;
import nl.saxion.game.eclosion.DayTransitionScreen;
import nl.saxion.game.eclosion.DoorTransitionScreen;
import nl.saxion.game.yourgamename.OddColorScreen;
import nl.saxion.gameapp.GameApp;

public class Main {
    public static void main(String[] args) {
        // Add screens
        GameApp.addScreen("TitleScreen", new TitleScreen());
        GameApp.addScreen("MainMenuScreen", new MainMenuScreen());
        GameApp.addScreen("YourGameScreen", new YourGameScreen());
        GameApp.addScreen("Floor1FlowerScreen", new Floor1FlowerScreen());
        GameApp.addScreen("RoomScreen", new RoomScreen());
        GameApp.addScreen("FloorTransitionScreen", new FloorTransitionScreen());
        GameApp.addScreen("NightCutsceneScreen", new NightCutsceneScreen());
        GameApp.addScreen("OddColorScreen", new OddColorScreen());
        GameApp.addScreen("DialogueDemoScreen", new DialogueDemoScreen());
        GameApp.addScreen("DayTransitionScreen", new DayTransitionScreen());
        GameApp.addScreen("DoorTransitionScreen", new DoorTransitionScreen());

        // Start game loop and show the title screen
        GameApp.start("Eclosion", 1280, 720, 60, false, "TitleScreen");
    }
}