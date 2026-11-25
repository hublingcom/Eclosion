package nl.saxion.game.yourgamename;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;
import com.badlogic.gdx.math.Rectangle;

public class FIndOddColor extends ScalableGameScreen {
    // Spawn Ageha near the bottom door
    Ageha agehaComeIn = new Ageha(675, 100);
    Ageha agehaPlaying = new Ageha(300, 100);

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
    int diff = 120;
    int state = 4;
    private float countdown;
    int maxRound = 10;
    int round;
    private static final float CHOOSE_TIME = 1f;   // đứng yên trên vòng tròn 1 giây thì chọn
    int standingOn = -1;                           // vòng tròn dưới chân, -1 = không có
    float standTime = 0;
    int lockedCircle = -1;                         // vòng vừa chọn đúng, phải bước ra rồi mới tính lại
    float lastFeetX, lastFeetY;                    // để biết Ageha có đang đi không


    public FIndOddColor() {
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
        GameApp.addTexture("library_game", "textures/Background/library.png");
        GameApp.addTexture("library_while_playing", "textures/Background/library_while_playing.jpeg");
        GameApp.addFont("rule", "fonts/DotGothic16-Regular.ttf", 22);
        GameApp.addFont("score", "fonts/DotGothic16-Regular.ttf", 30);
        GameApp.addFont("title", "fonts/DotGothic16-Regular.ttf", 40);
        GameApp.addColor("black", Color.BLACK);
        GameApp.addColor("gold", Color.GOLD);
        newRound();
    }

    public void render(float delta) {
        super.render(delta);
        GameApp.clearScreen("black");

        // Game's introduction
        if (state == 0) {
            GameApp.startSpriteRendering();
            GameApp.drawTexture("library_intro", 0, 0, getWorldWidth(), getWorldHeight());
            GameApp.drawText("title", "Rule", 600, 460, "black");
            GameApp.drawText("rule", "Find the circle with a different color", 435, 400, "black");
            GameApp.drawText("rule", "10 rounds - 10 seconds per round", 480, 350, "black");
            GameApp.drawText("rule", "Press [Y] to start ", 540, 255, "black");
            GameApp.endSpriteRendering();
            if (GameApp.isKeyJustPressed(Input.Keys.Y)) {
                state = 1;
                round = 0;
                countdown = 10f;
                standingOn = -1;
                standTime = 0;
                lockedCircle = -1;
                newRound();
                agehaPlaying = new Ageha(300, 100);
                diff = 60;
                score = 0;
            }
            // while playing
        } else if (state == 1) {
            agehaPlaying.update(delta, walkableAreas, obstacles);
            GameApp.startSpriteRendering();
            GameApp.drawTexture("library_while_playing", 0, 0, getWorldWidth(), getWorldHeight());
            GameApp.drawText("score", "Score:" + score, 855, 275, "black");
            GameApp.endSpriteRendering();
            GameApp.enableTransparency();
            GameApp.startShapeRenderingFilled();
            if (standingOn >= 0 && standTime > 0) {
                float degrees = 360f * Math.min(1f, standTime / CHOOSE_TIME);
                GameApp.getShapeRenderer().setColor(1f, 1f, 1f, 0.9f);
                GameApp.getShapeRenderer().arc(circleX[standingOn], circleY[standingOn],
                        radius + 7, 270 - degrees, degrees, 48);
            }
            for (int i = 0; i < 9; i++) {
                GameApp.drawCircle(circleX[i], circleY[i], radius, i == oddCircle ? "odd" : "normal");
            }
            GameApp.endShapeRendering();
            GameApp.disableTransparency();
            GameApp.startSpriteRendering();
            agehaPlaying.draw(delta);
            GameApp.endSpriteRendering();
            countdown -= delta;
            if (countdown < 0) {
                state = 3;
                return;
            }
            chooseByStanding(delta);


        } else if (state == 2) {
            GameApp.startSpriteRendering();
            GameApp.drawTexture("library_intro", 0, 0, getWorldWidth(), getWorldHeight());
            GameApp.drawText("title", "You Win", 565, 400, "black");
            GameApp.drawText("rule", "Press [Enter] to go back", 510, 255, "black");
            GameApp.endSpriteRendering();
            if (GameApp.isKeyJustPressed(Input.Keys.ENTER)) {
                state = 4;
            }
            // Game over screen
        } else if (state == 3) {
            GameApp.startSpriteRendering();
            GameApp.drawTexture("library_intro", 0, 0, getWorldWidth(), getWorldHeight());
            GameApp.drawText("title", "Game Over", 545, 400, "black");
            GameApp.drawText("rule", "Press [Y] to restart ", 540, 255, "black");
            GameApp.endSpriteRendering();
            if (GameApp.isKeyJustPressed(Input.Keys.Y)) {
                diff = 60;
                state = 1;
                round = 0;
                score = 0;
                countdown = 10f;
                standingOn = -1;
                standTime = 0;
                lockedCircle = -1;
                agehaPlaying = new Ageha(300, 100);
                newRound();
            }
            // normal room
        } else if (state == 4) {
            agehaComeIn.update(delta, walkableAreas, obstacles);
            GameApp.startSpriteRendering();
            GameApp.drawTexture("library_game", 0, 0, getWorldWidth(), getWorldHeight());
            GameApp.endSpriteRendering();
            // item
            GameApp.startShapeRenderingFilled();
            GameApp.drawCircle(645, 475, 10, "gold");
            GameApp.endShapeRendering();

            GameApp.startSpriteRendering();
            agehaComeIn.draw(delta);
            GameApp.endSpriteRendering();
            if (agehaComeIn.isNearDoor(item)) {
                if (GameApp.isKeyJustPressed(Input.Keys.E)) {
                    state = 0;
                }
            }

        }
    }

    public int circleUnderFeet() {
        for (int i = 0; i < 9; i++) {
            if (GameApp.pointInCircle(agehaPlaying.centerX(), agehaPlaying.feetY(),
                    circleX[i], circleY[i], radius)) {
                return i;
            }
        }
        return -1;
    }

    public void chooseByStanding(float delta) {
        float fx = agehaPlaying.centerX();
        float fy = agehaPlaying.feetY();
        boolean walking = Math.abs(fx - lastFeetX) > 0.01f || Math.abs(fy - lastFeetY) > 0.01f;
        lastFeetX = fx;
        lastFeetY = fy;

        int under = circleUnderFeet();
        if (under != lockedCircle) {
            lockedCircle = -1;
        }
        if (under < 0 || under == lockedCircle || walking) {
            standingOn = (under == lockedCircle) ? -1 : under;
            standTime = 0;
            return;
        }
        if (under != standingOn) {
            standingOn = under;
            standTime = 0;
        }
        standTime += delta;
        if (standTime >= CHOOSE_TIME) {
            choose(under);
        }
    }

    public void choose(int circle) {
        standingOn = -1;
        standTime = 0;
        if (circle == oddCircle) {
            diff = diff - 5;
            round++;
            score++;
            countdown = 10f;
            lockedCircle = circle;
            agehaPlaying = new Ageha(300, 100);
            if (round >= maxRound) {
                state = 2;
            } else {
                newRound();
            }
        } else {
            state = 3;
        }
    }

    public void newRound() {
        oddCircle = GameApp.randomInt(0, 9);
        r = GameApp.randomInt(40, 200);
        g = GameApp.randomInt(40, 200);
        b = GameApp.randomInt(40, 200);
        GameApp.addColor("odd", Math.min(255, r + diff), Math.min(255, g + diff), Math.min(255, b + diff));
        GameApp.addColor("normal", r, g, b);

    }

    public void hide() {
        GameApp.disposeFont("score");
        GameApp.disposeFont("rule");
        GameApp.disposeFont("title");
        GameApp.disposeTexture("library_game");
        GameApp.disposeTexture("library_intro");
    }
}
