package nl.saxion.game;

import nl.saxion.game.yourgamename.MiniGame2;
import nl.saxion.game.yourgamename.YourGameScreen;
import nl.saxion.game.yourgamename.MainMenuScreen;
import nl.saxion.gameapp.GameApp;

public class Main {
    public static void main(String[] args) {
        // Add screens
        GameApp.addScreen("MainGameScreen", new MainMenuScreen());
        GameApp.addScreen("YourGameScreen", new YourGameScreen());
        GameApp.addScreen("MiniGame2", new MiniGame2());

        // Start game loop and show main menu screen
        GameApp.start("MiniGame2", 1280, 720, 60, false, "MiniGame2");
    }
}
