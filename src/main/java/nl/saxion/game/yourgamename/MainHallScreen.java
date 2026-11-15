package nl.saxion.game.yourgamename;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

public class MainHallScreen extends ScalableGameScreen {
    Ageha ageha = new Ageha(100, 350);

    Rectangle mainFloor = new Rectangle(125, 115, 1030, 330);

    Rectangle leftCorridor = new Rectangle(25, 345, 105, 45);
    Rectangle rightCorridor = new Rectangle(1150, 345, 105, 45);
    Rectangle topCorridor = new Rectangle(620, 445, 40, 250);
    Rectangle bottomCorridor = new Rectangle(620, 25, 40, 100);

    Rectangle stairs      = new Rectangle(920, 402, 268, 74);
    Rectangle tvBounds    = new Rectangle(106, 160, 104, 130);
    Rectangle wineTable   = new Rectangle(298, 164, 62, 44);
    Rectangle armchair    = new Rectangle(384, 156, 78, 84);
    Rectangle sofa        = new Rectangle(178, 88, 204, 72);
    Rectangle centerTable = new Rectangle(598, 230, 80, 78);
    Rectangle petBed      = new Rectangle(924, 112, 84, 52);
    Rectangle petBowls    = new Rectangle(1026, 124, 62, 22);

    Rectangle toLeftCorridorZone =
            new Rectangle(0, 322, 45, 86);

    Rectangle toRightCorridorZone =
            new Rectangle(1186, 322, 94, 86);

    Rectangle toUpCorridorZone =
            new Rectangle(596, 476, 88, 244);

    Rectangle toDownCorridorZone =
            new Rectangle(596, 0, 88, 84);
    public MainHallScreen(){
        super(1280,720);
    }

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
        GameApp.addTexture("main_hall",
                "textures/backgrounds/main_hall.png");
        GameApp.addFont("main", "fonts/basic.ttf", 24);

        ageha.setPosition(100, 350);

    }

    public void render(float delta){
        super.render(delta);

        GameApp.clearScreen("black");
        GameApp.startSpriteRendering();
        GameApp.drawTexture("main_hall",0,0,1280,720);

        ageha.update(delta,
                new Rectangle[]{
                        mainFloor,
                        topCorridor,
                        bottomCorridor,
                        leftCorridor,
                        rightCorridor
                },
                stairs,
                armchair,
                tvBounds,
                wineTable,
                sofa,
                centerTable,
                petBed,
                petBowls
        );

        if(ageha.isNearDoor(toLeftCorridorZone)){
            GameApp.switchScreen("CorridorScreen");
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
        GameApp.disposeTexture("main_hall");
        GameApp.disposeFont("main");
    }

}
