package nl.saxion.game.eclosion.explore;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import java.util.ArrayList;
import java.util.List;

/**
 * Makes characters blend into a room's lighting.
 *
 * A character's colour is made of three parts:
 *   1. baseTint     - the room's dark ambient colour (what you look like in a dark corner)
 *   2. floor light  - the colour of the floor picture around the feet (the red light from the window
 *                     is painted on the floor, so walking into it makes the character redder)
 *   3. point lights - lamps etc. added with addLight(); they glow on a character near them
 * The result changes smoothly, so walking from light into shadow is a soft fade.
 *
 * Works for every character in the room: give each one its own Color (newTint()), call
 * update(...) for it every frame and draw it with that colour on the sprite batch.
 * shadowAlpha(...) gives how dark the shadow under a character should be.
 */
public class RoomLight {
    // Tune these to change the look of all characters in the room.
    public Color baseTint = new Color(0.55f, 0.50f, 0.55f, 1f);   // colour in total darkness
    public Color floorGain = new Color(1.0f, 0.35f, 0.40f, 0f);   // how much the floor colour is added
    public float followSpeed = 5f;                               // how fast the colour follows the character
    public float sampleWidth = 44, sampleHeight = 22;            // area around the feet that is looked at

    /** A lamp or other light: warm glow on characters within the radius. */
    private static class PointLight {
        final float x, y, radius;
        final Color color;
        PointLight(float x, float y, float radius, Color color) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.color = color;
        }
    }

    private final Pixmap pixels;
    private final float scale;      // image pixels -> world units
    private final float roomY;      // world y of the bottom of the picture
    private final List<PointLight> lights = new ArrayList<>();
    private final Color target = new Color();
    private final Color sample = new Color();

    public RoomLight(String imagePath, float scale, float roomY) {
        this.pixels = new Pixmap(Gdx.files.internal(imagePath));
        this.scale = scale;
        this.roomY = roomY;
    }

    /**
     * Adds a light at a world position. color = how much it adds at the centre
     * (for example 0.35, 0.22, 0.08 for a warm lamp); it fades out softly towards the radius.
     */
    public void addLight(float x, float y, float radius, Color color) {
        lights.add(new PointLight(x, y, radius, color));
    }

    /** A fresh colour to start a character with. */
    public Color newTint() {
        return new Color(baseTint);
    }

    /** Moves the character's colour smoothly towards the light at its feet. */
    public void update(Color tint, float feetX, float feetY, float delta) {
        lightAt(feetX, feetY, target);
        float t = 1f - (float) Math.exp(-followSpeed * delta);   // same smoothness at any frame rate
        tint.r += (target.r - tint.r) * t;
        tint.g += (target.g - tint.g) * t;
        tint.b += (target.b - tint.b) * t;
        tint.a = 1f;
    }

    /** The colour a character standing at this spot should get. */
    public Color lightAt(float feetX, float feetY, Color out) {
        Color floor = floorColor(feetX, feetY);
        out.set(baseTint.r + floor.r * floorGain.r,
                baseTint.g + floor.g * floorGain.g,
                baseTint.b + floor.b * floorGain.b, 1f);
        for (PointLight light : lights) {
            float dx = feetX - light.x, dy = feetY - light.y;
            float d = (float) Math.sqrt(dx * dx + dy * dy) / light.radius;
            if (d >= 1f) continue;
            float strength = (1f - d) * (1f - d);   // soft falloff
            out.r += light.color.r * strength;
            out.g += light.color.g * strength;
            out.b += light.color.b * strength;
        }
        out.r = Math.min(1f, out.r);
        out.g = Math.min(1f, out.g);
        out.b = Math.min(1f, out.b);
        return out;
    }

    /** How dark the shadow under a character should be: a bit stronger where it's bright. */
    public float shadowAlpha(Color tint) {
        float brightness = (tint.r + tint.g + tint.b) / 3f;
        return 0.25f + 0.25f * brightness;
    }

    /** Average colour of the room picture in a small area around the feet (4 x 3 samples). */
    private Color floorColor(float feetX, float feetY) {
        float r = 0, g = 0, b = 0;
        int count = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 3; j++) {
                float wx = feetX - sampleWidth / 2 + sampleWidth * i / 3f;
                float wy = feetY - sampleHeight / 2 + sampleHeight * j / 2f;
                int ix = (int) (wx / scale);
                int iy = (int) (pixels.getHeight() - (wy - roomY) / scale);
                ix = Math.max(0, Math.min(pixels.getWidth() - 1, ix));
                iy = Math.max(0, Math.min(pixels.getHeight() - 1, iy));
                Color.rgba8888ToColor(sample, pixels.getPixel(ix, iy));
                r += sample.r;
                g += sample.g;
                b += sample.b;
                count++;
            }
        }
        return new Color(r / count, g / count, b / count, 1f);
    }

    public void dispose() {
        pixels.dispose();
    }
}
