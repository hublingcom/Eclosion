package nl.saxion.game.yourgamename;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.Gdx;

public class PianoMemory extends ScalableGameScreen {
    public PianoMemory() {
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
    }

    @Override
    public void render(float delta) {
        super.render(delta);

    }

    @Override
    public void hide() {
        GameApp.disposeFont("basic");
        GameApp.disposeFont("rule");
        GameApp.disposeTexture("library_game");
        GameApp.disposeTexture("library_intro");
    }
}
