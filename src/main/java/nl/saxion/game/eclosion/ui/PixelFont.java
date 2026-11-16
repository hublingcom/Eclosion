package nl.saxion.game.eclosion.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

/**
 * The game's one font: DotGothic16 (retro pixel font, OFL licence, resources/fonts),
 * made with sharp pixels (no smoothing). Used by the dialogue box, the title screen and the minigames,
 * so every screen looks the same.
 *
 *   GameApp.addFont("my_font", PixelFont.make(24));
 */
public final class PixelFont {
    private static final String FILE = "fonts/DotGothic16-Regular.ttf";

    private PixelFont() {
    }

    public static BitmapFont make(int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(FILE));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = size;
        parameter.mono = true;
        parameter.color = Color.WHITE;
        parameter.minFilter = Texture.TextureFilter.Nearest;
        parameter.magFilter = Texture.TextureFilter.Nearest;
        BitmapFont font = generator.generateFont(parameter);
        generator.dispose();
        return font;
    }
}
