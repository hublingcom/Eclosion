package nl.saxion.game.yourgamename;
import com.badlogic.gdx.graphics.Color;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.Gdx;

public class CorridorScreen extends ScalableGameScreen{
    // Create Ageha inside the corridor
    Ageha ageha = new Ageha(800, 230);

    // Walkable corridor area
    Rectangle corridorBounds = new Rectangle(290, 220, 765, 85);

    Rectangle roomDoorBounds =
            new Rectangle(693, 317, 75, 101);

    Rectangle toMainHallZone =
            new Rectangle(1040, 190, 40, 145);

    // Door question
    boolean showDoorQuestion = false;
    boolean doorQuestionDismissed = false;


    Rectangle yesButton = new Rectangle(480, 270, 100, 50);
    Rectangle noButton = new Rectangle(650, 270, 100, 50);


    public CorridorScreen(){
        super(1280,720);
    }

    @Override
    public void show(){
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
        GameApp.addTexture("corridor",
                "textures/backgrounds/corridor.png");
        GameApp.addFont("main", "fonts/basic.ttf", 24);

        ageha.setPosition(800, 230);
    }

    @Override
    public void render(float delta){
        super.render(delta);

        GameApp.clearScreen("black");
        GameApp.startSpriteRendering();
        GameApp.drawTexture("corridor",0,0,1280,720);


        ageha.update(delta,
                new Rectangle[]{corridorBounds}
        );

            // Check if Ageha has reached the door
            if (ageha.isNearDoor(roomDoorBounds) &&
                    Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.E)) {

                GameApp.switchScreen("YourGameScreen");
            }

        if (ageha.isNearDoor(toMainHallZone)) {
            GameApp.switchScreen("MainHallScreen");
        }

        ageha.draw(delta);

        GameApp.endSpriteRendering();
    }
    public void hide(){
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
        GameApp.disposeTexture("corridor");
        GameApp.disposeFont("main");
    }
}
