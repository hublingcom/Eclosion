package nl.saxion.game.eclosion.ui;

import com.badlogic.gdx.graphics.Color;
import nl.saxion.gameapp.GameApp;

/**
 * Fades the whole screen to black and back.
 *   fadeOut()   : screen goes to black, stays black for a moment, then isDoneFadingOut() is true
 *   fadeIn()    : starts black and slowly shows the screen again (call it in show())
 * While busy() is true the player shouldn't be able to walk or interact.
 */
public class ScreenFader {
    public float fadeOutTime = 0.45f;   // seconds to go black
    public float blackTime = 0.30f;     // seconds fully black (before switching screen / before showing)
    public float fadeInTime = 0.60f;    // seconds to come back

    private enum State { NONE, OUT, BLACK_OUT, BLACK_IN, IN }
    private State state = State.NONE;
    private float timer = 0;
    private float alpha = 0;            // 0 = clear, 1 = fully black

    public void fadeOut() {
        state = State.OUT;
        timer = 0;
    }

    public void fadeIn() {
        state = State.BLACK_IN;
        timer = 0;
        alpha = 1;
    }

    public boolean busy() {
        return state != State.NONE;
    }

    /** True once the screen has gone black after fadeOut() (the moment to switch screens). */
    public boolean isDoneFadingOut() {
        return state == State.BLACK_OUT && timer >= blackTime;
    }

    public void update(float delta) {
        timer += delta;
        switch (state) {
            case OUT -> {
                alpha = Math.min(1f, timer / fadeOutTime);
                if (timer >= fadeOutTime) { state = State.BLACK_OUT; timer = 0; }
            }
            case BLACK_OUT -> alpha = 1;    // stays black until the screen is switched
            case BLACK_IN -> {
                alpha = 1;
                if (timer >= blackTime) { state = State.IN; timer = 0; }
            }
            case IN -> {
                float t = Math.min(1f, timer / fadeInTime);
                alpha = 1f - t * t * (3 - 2 * t);   // smooth start and end
                if (timer >= fadeInTime) { state = State.NONE; alpha = 0; }
            }
            default -> alpha = 0;
        }
    }

    /** Draw last, on top of everything. */
    public void render(float worldW, float worldH) {
        if (alpha <= 0) return;
        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        GameApp.drawRect(-10, -10, worldW + 20, worldH + 20, new Color(0, 0, 0, alpha));
        GameApp.endShapeRendering();
        GameApp.disableTransparency();
    }
}
