
package nl.saxion.game.yourgamename;
import com.badlogic.gdx.graphics.Color;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.Gdx;


public class YourGameScreen extends ScalableGameScreen {

    // Create Ageha and set his starting position

    Ageha ageha = new Ageha(500, 200);

    // Bedroom collision boundaries

    Rectangle bedBounds =
            new Rectangle(323, 292, 108, 237);

    Rectangle sideTableBounds =
            new Rectangle(442, 425, 62, 63);

    Rectangle mainTableBounds =
            new Rectangle(646, 430, 156, 82);

    Rectangle chairBounds =
            new Rectangle(699, 394, 42, 70);

    Rectangle wardrobeBounds =
            new Rectangle(839, 481, 106, 116);

    Rectangle safeBounds =
            new Rectangle(950, 332, 38, 88);

    Rectangle roomBounds = new Rectangle(335, 215, 620, 370);

    Rectangle middleWall =
            new Rectangle(301, 490, 687, 126);

    Rectangle rightWardrobeWall =
            new Rectangle(945, 480, 43, 12);

    // Bedroom door
    Rectangle doorBounds =
            new Rectangle(680, 180, 86, 45);

    // Door question
    boolean showDoorQuestion = false;
    boolean doorQuestionDismissed = false;

    Rectangle yesButton = new Rectangle(480, 270, 100, 50);
    Rectangle noButton = new Rectangle(650, 270, 100, 50);

    public YourGameScreen() {
        super(1280, 720);
    }

    @Override
    public void show() {

        // Load Ageha's walking and idle images
        GameApp.addTexture("ageha_down_a",
                "textures/Ageha_move/ageha_down_a.png");
        GameApp.addTexture("ageha_down_b",
                "textures/Ageha_move/ageha_down_b.png");
        GameApp.addTexture("ageha_down_idle",
                "textures/Ageha_move/ageha_down_idle.png");

        GameApp.addTexture("ageha_left_a",
                "textures/Ageha_move/ageha_left_a.png");
        GameApp.addTexture("ageha_left_b",
                "textures/Ageha_move/ageha_left_b.png");
        GameApp.addTexture("ageha_left_idle",
                "textures/Ageha_move/ageha_left_idle.png");

        GameApp.addTexture("ageha_right_a",
                "textures/Ageha_move/ageha_right_a.png");
        GameApp.addTexture("ageha_right_b",
                "textures/Ageha_move/ageha_right_b.png");
        GameApp.addTexture("ageha_right_idle",
                "textures/Ageha_move/ageha_right_idle.png");

        GameApp.addTexture("ageha_up_a",
                "textures/Ageha_move/ageha_up_a.png");
        GameApp.addTexture("ageha_up_b",
                "textures/Ageha_move/ageha_up_b.png");
        GameApp.addTexture("ageha_up_idle",
                "textures/Ageha_move/ageha_up_idle.png");

        // Load bedroom background
        GameApp.addTexture("room",
                "textures/backgrounds/ageha_room.png");

        GameApp.addFont("main", "fonts/basic.ttf", 24);

        GameApp.addColor("grassGreen", 34, 139, 34);
    }

    @Override
    public void render(float delta) {

        super.render(delta);

        GameApp.clearScreen("black");

        GameApp.startSpriteRendering();

        // Draw bedroom
        GameApp.drawTexture("room", 0, 0, 1280, 720);

        // Update Ageha's movement and collision
        // Update Ageha only when the question is not showing



        ageha.update(delta,
                new Rectangle[]{roomBounds}
                ,chairBounds,
                bedBounds,
                sideTableBounds,
                mainTableBounds,
                wardrobeBounds,
                safeBounds,
                middleWall,
                rightWardrobeWall
        );

            // Check if Ageha has reached the door
        if (ageha.isNearDoor(doorBounds) &&
                Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.E)) {

            GameApp.switchScreen("CorridorScreen");
        }


        // Draw Ageha with the correct animation
        ageha.draw(delta);



        GameApp.endSpriteRendering();
    }


    @Override
    public void hide() {
        GameApp.disposeTexture("ageha_down_a");
        GameApp.disposeTexture("ageha_down_b");
        GameApp.disposeTexture("ageha_down_idle");
        GameApp.disposeTexture("ageha_up_idle");
        GameApp.disposeTexture("ageha_up_a");
        GameApp.disposeTexture("ageha_up_b");
        GameApp.disposeTexture("ageha_left_b");
        GameApp.disposeTexture("ageha_left_a");
        GameApp.disposeTexture("ageha_left_idle");
        GameApp.disposeTexture("ageha_right_b");
        GameApp.disposeTexture("ageha_right_a");
        GameApp.disposeTexture("ageha_right_idle");
        GameApp.disposeTexture("room");
        GameApp.disposeFont("main");
    }
}
