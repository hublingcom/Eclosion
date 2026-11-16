package nl.saxion.game.yourgamename;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.Gdx;

public class MiniGame2 extends ScalableGameScreen {
    // Spawn Ageha near the bottom door
    Ageha ageha = new Ageha(675, 100);

    // Walkable floor area and furniture obstacles
    Rectangle[] walkableAreas = {new Rectangle(289, 128, 701, 356)};
    Rectangle[] obstacles = {
            new Rectangle(482, 362, 330, 134),  // bàn
            new Rectangle(277, 376, 50, 96),   // quả địa cầu
            new Rectangle(877, 356, 45, 126),   // tượng
            new Rectangle(402, 432, 25, 64),    // nến trái
            new Rectangle(947, 432, 25, 64)     // nến phải
    };
    Rectangle item = new Rectangle(560, 330, 170, 60);
    int circleX[] = {562, 640, 718, 562, 640, 718, 562, 640, 718};
    int circleY[] = {296, 296, 296, 219, 219, 219, 142, 142, 142};
    int score = 0;
    int radius = 32;
    int oddCircle;
    int r;
    int g;
    int b;
    int diff = 60;
    int state = 4;
    private float countdown;
    int maxRound = 5;
    int round;

    public MiniGame2() {
        super(1280, 720);

    }

    @Override
    public void show() {
        GameApp.addTexture("ageha_down_a", "textures/Ageha_move/ageha_down_a.png");
        GameApp.addTexture("ageha_down_b", "textures/Ageha_move/ageha_down_b.png");
        GameApp.addTexture("ageha_down_idle", "textures/Ageha_move/ageha_down_idle.png");
        GameApp.addTexture("ageha_left_a", "textures/Ageha_move/ageha_left_a.png");
        GameApp.addTexture("ageha_left_b", "textures/Ageha_move/ageha_left_b.png");
        GameApp.addTexture("ageha_left_idle", "textures/Ageha_move/ageha_left_idle.png");
        GameApp.addTexture("ageha_right_a", "textures/Ageha_move/ageha_right_a.png");
        GameApp.addTexture("ageha_right_b", "textures/Ageha_move/ageha_right_b.png");
        GameApp.addTexture("ageha_right_idle", "textures/Ageha_move/ageha_right_idle.png");
        GameApp.addTexture("ageha_up_a", "textures/Ageha_move/ageha_up_a.png");
        GameApp.addTexture("ageha_up_b", "textures/Ageha_move/ageha_up_b.png");
        GameApp.addTexture("ageha_up_idle", "textures/Ageha_move/ageha_up_idle.png");
        GameApp.addTexture("library_intro", "textures/Background/library_intro.png");
        GameApp.addTexture("library_game", "textures/Background/library_game.png");
        GameApp.addFont("basic", "fonts/basic.ttf", 50);
        GameApp.addFont("rule", "fonts/basic.ttf", 28);
        GameApp.addColor("black", Color.BLACK);
        GameApp.addColor("gold", Color.GOLD);
        newRound();
    }

    public void render(float delta) {
        super.render(delta);
        float mouseX = getMouseX();
        float mouseY = getMouseY();
        GameApp.clearScreen("black");

        // Game's introduction
        if (state == 0) {
            GameApp.startSpriteRendering();
            GameApp.drawTexture("library_intro", 0, 0, getWorldWidth(), getWorldHeight());
            GameApp.drawText("basic", "Rule", 598, 470, "black");
            GameApp.drawText("rule", "Find the circle with a different color", 440, 400, "black");
            GameApp.drawText("rule", "5 rounds - 5 seconds per round", 470, 350, "black");
            GameApp.drawText("rule", "Press [Y] to start ", 540, 255, "black");
            GameApp.endSpriteRendering();
            if (GameApp.isKeyJustPressed(Input.Keys.Y)) {
                state = 1;
                round = 0;
                countdown = 10f;
                newRound();
            }
            // while playing
        } else if (state == 1) {
            GameApp.startSpriteRendering();
            GameApp.drawTexture("library_game", 0, 0, getWorldWidth(), getWorldHeight());
            GameApp.endSpriteRendering();

            GameApp.startShapeRenderingFilled();
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
                            diff = diff - 10;
                            round++;
                            countdown = 10f;
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
            // Win screen
        } else if (state == 2) {
            GameApp.startSpriteRendering();
            GameApp.drawTexture("library_intro", 0, 0, getWorldWidth(), getWorldHeight());
            GameApp.drawText("basic", "You Win", 565, 400, "black");
            GameApp.drawText("rule", "Press [Enter] to go back", 510, 255, "black");
            GameApp.endSpriteRendering();
            if (GameApp.isKeyJustPressed(Input.Keys.ENTER)) {
                state = 4;
            }
            // Game over screen
        } else if (state == 3) {
            GameApp.startSpriteRendering();
            GameApp.drawTexture("library_intro", 0, 0, getWorldWidth(), getWorldHeight());
            GameApp.drawText("basic", "Game Over", 540, 400, "black");
            GameApp.drawText("rule", "Press [Y] to restart ", 540, 255, "black");
            GameApp.endSpriteRendering();
            if (GameApp.isKeyJustPressed(Input.Keys.Y)) {
                diff = 60;
                state = 1;
                round = 0;
                countdown = 5f;
                newRound();
            }
            // normal room
        } else if (state == 4) {
            ageha.update(delta, walkableAreas, obstacles);
            GameApp.startSpriteRendering();
            GameApp.drawTexture("library_game", 0, 0, getWorldWidth(), getWorldHeight());
            GameApp.endSpriteRendering();
            // item
            GameApp.startShapeRenderingFilled();
            GameApp.drawCircle(645, 475, 10, "gold");
            GameApp.endShapeRendering();

            GameApp.startSpriteRendering();
            ageha.draw(delta);
            GameApp.endSpriteRendering();
            if ( ageha.isNearDoor(item)) {
                if (GameApp.isKeyJustPressed(Input.Keys.E)) {
                    state = 0;
                }
            }

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
