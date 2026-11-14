package nl.saxion.game;

import nl.saxion.game.yourgamename.CorridorScreen;
import nl.saxion.game.yourgamename.MainMenuScreen;
import nl.saxion.game.yourgamename.RoomScreen;
import nl.saxion.game.yourgamename.YourGameScreen;
import nl.saxion.gameapp.GameApp;

public class Main {
    public static void main(String[] args) {
        // Add screens
        GameApp.addScreen("MainMenuScreen", new MainMenuScreen());
        GameApp.addScreen("YourGameScreen", new YourGameScreen());
        GameApp.addScreen("RoomScreen", new RoomScreen());
        GameApp.addScreen("CorridorScreen", new CorridorScreen());

        // Start game loop and show main menu screen
        GameApp.start("Eclosion", 1280, 720, 60, false, "MainMenuScreen");
    }
}
