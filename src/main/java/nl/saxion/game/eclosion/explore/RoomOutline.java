package nl.saxion.game.eclosion.explore;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;

/**
 * The black area around a room picture (outside the pink outline). Clicks there are ignored.
 *
 * How: start at the edges of the picture and spread over every (almost) black pixel that touches it.
 * The pink outline stops it, so dark things INSIDE the room (the TV, shadows, the safe) don't count.
 * Done once per place.
 */
class RoomOutline {
    private static final int BLACK = 8;     // a pixel this dark (0..255) counts as black

    private final boolean[] outside;        // per image pixel: true = the black around the room
    private final int width, height;

    RoomOutline(String imagePath) {
        Pixmap picture = new Pixmap(Gdx.files.internal(imagePath));
        width = picture.getWidth();
        height = picture.getHeight();
        outside = new boolean[width * height];
        int[] queue = new int[width * height];
        int head = 0, tail = 0;
        for (int px = 0; px < width; px++) {
            tail = markIfBlack(picture, queue, tail, px, 0);
            tail = markIfBlack(picture, queue, tail, px, height - 1);
        }
        for (int py = 0; py < height; py++) {
            tail = markIfBlack(picture, queue, tail, 0, py);
            tail = markIfBlack(picture, queue, tail, width - 1, py);
        }
        while (head < tail) {
            int p = queue[head++];
            int px = p % width, py = p / width;
            if (px > 0) tail = markIfBlack(picture, queue, tail, px - 1, py);
            if (px < width - 1) tail = markIfBlack(picture, queue, tail, px + 1, py);
            if (py > 0) tail = markIfBlack(picture, queue, tail, px, py - 1);
            if (py < height - 1) tail = markIfBlack(picture, queue, tail, px, py + 1);
        }
        picture.dispose();
    }

    private int markIfBlack(Pixmap picture, int[] queue, int tail, int px, int py) {
        int i = py * width + px;
        if (outside[i]) return tail;
        int c = picture.getPixel(px, py);   // RGBA8888
        int r = (c >>> 24) & 0xff, g = (c >>> 16) & 0xff, b = (c >>> 8) & 0xff;
        if (Math.max(r, Math.max(g, b)) > BLACK) return tail;   // not black: the room (or its outline)
        outside[i] = true;
        queue[tail] = i;
        return tail + 1;
    }

    /** True for an image pixel on the black around the room (or off the picture). */
    boolean isOutside(int px, int py) {
        if (px < 0 || py < 0 || px >= width || py >= height) {
            return true;
        }
        return outside[py * width + px];
    }
}
