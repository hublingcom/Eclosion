package nl.saxion.game.yourgamename;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

public class MiniGame2 extends ScalableGameScreen {
    int circleX[] = {562, 640, 718, 562, 640, 718, 562, 640, 718};
    int circleY[] = {296, 296, 296, 219, 219, 219, 142, 142, 142};
    int score = 0;
    int radius = 32;
    int oddCircle;
    int r;
    int g;
    int b;
    int diff = 40;
    int state = 0;
    private float countdown;
    int maxRound = 5;
    int round;

    public MiniGame2() {
        super(1280, 720);

    }

    @Override
    public void show() {
        GameApp.addTexture("library_intro", "textures/Background/library_intro.png");
        GameApp.addTexture("library_game", "textures/Background/library_game.png");
        GameApp.addFont("basic", "fonts/basic.ttf", 50);
        GameApp.addFont("rule", "fonts/basic.ttf", 28);
        GameApp.addColor("black", Color.BLACK);
        newRound();
    }

    public void render(float delta) {
        super.render(delta);
        float mouseX = getMouseX();
        float mouseY = getMouseY();
        GameApp.clearScreen("black");
        if (state == 0) {
            GameApp.startSpriteRendering();
            GameApp.drawTexture("library_intro", 0, 0, getWorldWidth(), getWorldHeight());
            GameApp.drawText("basic", "Rule", 598, 470,"black");
            GameApp.drawText("rule", "Find the circle with a different color", 440, 400, "black");
            GameApp.drawText("rule", "5 rounds - 5 seconds per round", 470, 350, "black");
            GameApp.drawText("rule", "Press [Y] to start ", 540, 255, "black");
            GameApp.endSpriteRendering();
            if (GameApp.isKeyJustPressed(Input.Keys.Y)) {
                state = 1;
                round = 0;
                countdown = 5f;
                newRound();
            }
        } else if (state == 1) {
            GameApp.startShapeRenderingFilled();
            GameApp.startSpriteRendering();
            GameApp.drawTexture("library_game", 0, 0, getWorldWidth(), getWorldHeight());
            GameApp.endSpriteRendering();
            for (int i = 0; i < 9; i++) {
                if (i == oddCircle) {
                    GameApp.drawCircle(circleX[i], circleY[i], radius, "odd");
                } else {
                    GameApp.drawCircle(circleX[i], circleY[i], radius, "normal");
                }
            }
            GameApp.endShapeRendering();
            countdown -= delta;
            if (countdown < 0) {
                state = 3;
            }
            if (GameApp.isButtonJustPressed(0)) {
                for (int i = 0; i < 9; i++) {
                    if (GameApp.pointInCircle(mouseX, mouseY, circleX[i], circleY[i], radius)) {
                        if (i == oddCircle) {
                            round++;
                            countdown = 5f;
                            if (round >= maxRound) {
                                state = 2;
                            } else {
                                newRound();
                            }
                        } else {
                            state = 3;
                        }

                    }
                }
            }
        } else if (state == 2) {
            GameApp.startSpriteRendering();
            GameApp.drawTexture("library_intro", 0, 0, getWorldWidth(), getWorldHeight());
            GameApp.drawText("basic","You Win", 565, 400,"black");
            GameApp.endSpriteRendering();

        } else if (state == 3) {
            GameApp.startSpriteRendering();
            GameApp.drawTexture("library_intro", 0, 0, getWorldWidth(), getWorldHeight());
            GameApp.drawText("basic", "Game Over",540,400, "black");
            GameApp.endSpriteRendering();

        }
    }


    public void newRound() {
        oddCircle = GameApp.randomInt(0, 9);
        r = GameApp.randomInt(40, 200);
        g = GameApp.randomInt(40, 200);
        b = GameApp.randomInt(40, 200);
        GameApp.addColor("odd", r + diff, g + diff, b + diff);
        GameApp.addColor("normal", r, g, b);

    }

    public void hide() {
        GameApp.disposeFont("basic");
        GameApp.disposeFont("rule");
        GameApp.disposeTexture("library_game");
        GameApp.disposeTexture("library_intro");


    }
}
