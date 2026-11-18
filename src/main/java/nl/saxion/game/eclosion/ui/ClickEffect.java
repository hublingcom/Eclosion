package nl.saxion.game.eclosion.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import java.util.ArrayList;
import java.util.List;
import nl.saxion.gameapp.GameApp;

/**
 * A small bright white circle where you click with the mouse. It grows a little and fades away.
 *
 * Usage: at the very END of a screen's render (so it is drawn on top of everything):
 *   ClickEffect.updateAndDraw(getMouseX(), getMouseY());
 */
public class ClickEffect {
    private static final float LIFE = 0.45f;          // seconds until it is gone
    private static final float START_RADIUS = 4;
    private static final float END_RADIUS = 12;
    private static final Color COLOR = new Color(1f, 1f, 1f, 1f);

    private static final List<float[]> clicks = new ArrayList<>();   // {x, y, age}

    public static void updateAndDraw(float mouseX, float mouseY) {
        if (GameApp.isButtonJustPressed(Input.Buttons.LEFT)) {
            clicks.add(new float[] { mouseX, mouseY, 0 });
        }
        if (clicks.isEmpty()) {
            return;
        }
        float delta = Gdx.graphics.getDeltaTime();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        GameApp.startShapeRenderingFilled();
        ShapeRenderer shapes = GameApp.getShapeRenderer();
        for (int i = clicks.size() - 1; i >= 0; i--) {
            float[] c = clicks.get(i);
            c[2] += delta;
            float t = c[2] / LIFE;                         // 0 -> 1
            if (t >= 1) {
                clicks.remove(i);
                continue;
            }
            float ease = 1 - (1 - t) * (1 - t);            // fast at first, then slow
            float radius = START_RADIUS + (END_RADIUS - START_RADIUS) * ease;
            float alpha = (1 - t) * (1 - t);               // fades out smoothly
            // soft glow around it, then the bright core
            shapes.setColor(COLOR.r, COLOR.g, COLOR.b, alpha * 0.25f);
            shapes.circle(c[0], c[1], radius * 1.8f, 24);
            shapes.setColor(COLOR.r, COLOR.g, COLOR.b, alpha * 0.9f);
            shapes.circle(c[0], c[1], radius, 24);
        }
        GameApp.endShapeRendering();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}
