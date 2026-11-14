package nl.saxion.game.yourgamename;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Input;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

public class YourGameScreen extends ScalableGameScreen {
    int[] circleX = {480, 640, 800, 480, 640, 800, 480, 640, 800};
    int[] circleY = {520, 520, 520, 360, 360, 360, 200, 200, 200};
    float radius = 60;
    int oddCircle = GameApp.randomInt(0, 9);
    int r = GameApp.randomInt(0, 100);
    int g = GameApp.randomInt(0, 100);
    int b = GameApp.randomInt(0, 100);
    int diff = 15;
    boolean gameOver = false;

    public YourGameScreen() {
        super(1280, 720);
    }

    @Override
    public void show() {
        GameApp.addTexture("room", "textures/Background/room.png");
        GameApp.addColor("normal", r, g, b);
        GameApp.addColor("odd", r + diff, g + diff, b + diff);

    }

    @Override
    public void render(float delta) {
        super.render(delta);
        GameApp.clearScreen("black");
        float mouseX = getMouseX();
        float mouseY = getMouseY();
        GameApp.startSpriteRendering();
        GameApp.drawTexture("room", 0, 0, getWorldWidth(), getWorldHeight());
        GameApp.endSpriteRendering();
        GameApp.startShapeRenderingFilled();
            int x;
            int y;
            for (int i = 0; i < 9; i++) {
                x = circleX[i];
                y = circleY[i];
                if (oddCircle == i) {
                    GameApp.drawCircle(x, y, radius, "odd");
                } else {
                    GameApp.drawCircle(x, y, radius, "normal");
                }
            }

        GameApp.endShapeRendering();



    }

    @Override
    public void hide() {
        GameApp.disposeTexture("room");
    }
}