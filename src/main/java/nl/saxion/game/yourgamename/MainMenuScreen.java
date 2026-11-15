package nl.saxion.game.yourgamename;

import com.badlogic.gdx.Input;
import nl.saxion.game.eclosion.FloorTransitionScreen;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

public class MainMenuScreen extends ScalableGameScreen {
    public MainMenuScreen() {
        super(1280, 720);
    }

    @Override
    public void show() {
        GameApp.addFont("basic", "fonts/basic.ttf", 100);
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        // When the user presses enter, go to the next screen
        if (GameApp.isKeyJustPressed(Input.Keys.ENTER)) {
            GameApp.switchScreen("YourGameScreen");
        }

        // Test shortcuts: 1 = Floor 1 minigame, 2 = Ageha's room, 3 = stairs -> Floor 1
        if (GameApp.isKeyJustPressed(Input.Keys.NUM_1)) {
            GameApp.switchScreen("Floor1FlowerScreen");
        }
        if (GameApp.isKeyJustPressed(Input.Keys.NUM_2)) {
            GameApp.switchScreen("RoomScreen");
        }
        if (GameApp.isKeyJustPressed(Input.Keys.NUM_3)) {
            FloorTransitionScreen.prepare("Day 2", "Floor 1", "Loneliness", "Floor1FlowerScreen");
            GameApp.switchScreen("FloorTransitionScreen");
        }
        if (GameApp.isKeyJustPressed(Input.Keys.NUM_4)) {
            GameApp.switchScreen("OddColorScreen");
        }

        // Render the main menu
        GameApp.clearScreen("black");
        GameApp.startSpriteRendering();
        GameApp.drawTextCentered("basic", "ECLOSION (press enter to start)", getWorldWidth()/2, getWorldHeight()/2, "amber-500");
        GameApp.endSpriteRendering();
    }

    @Override
    public void hide() {
        GameApp.disposeFont("basic");
    }
}