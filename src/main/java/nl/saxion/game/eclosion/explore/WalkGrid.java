package nl.saxion.game.eclosion.explore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Finding a way around furniture and walls for click-to-walk.
 *
 * The room is cut into small squares; a square is free when Ageha can stand there. A* search finds
 * the shortest way over free squares, then the zig-zag is made into a few straight lines.
 * All points are "the middle of Ageha's feet" in world units.
 */
class WalkGrid {
    /** Can Ageha stand with the middle of his feet here? */
    interface Floor {
        boolean canStand(float middleX, float middleY);
    }

    private static final float CELL = 6;            // size of one square (world units)
    private static final float SEARCH_RADIUS = 240;  // nearestFreeSpot looks this far around a point

    private final Floor floor;
    private final float originY;
    private final int cols, rows;
    private final boolean[] free;

    /** width/height = the area of the room picture in the world, originY = its bottom. */
    WalkGrid(Floor floor, float width, float originY, float height) {
        this.floor = floor;
        this.originY = originY;
        cols = (int) Math.ceil(width / CELL);
        rows = (int) Math.ceil(height / CELL);
        free = new boolean[cols * rows];
        for (int j = 0; j < rows; j++) {
            for (int i = 0; i < cols; i++) {
                free[j * cols + i] = floor.canStand(cellX(i), cellY(j));
            }
        }
    }

    private float cellX(int i) {
        return (i + 0.5f) * CELL;
    }

    private float cellY(int j) {
        return originY + (j + 0.5f) * CELL;
    }

    /**
     * The spot closest to (cx, cy) where Ageha can stand. Looks in growing circles around the point.
     * Null if there is nothing within SEARCH_RADIUS.
     */
    float[] nearestFreeSpot(float cx, float cy) {
        for (float r = 0; r <= SEARCH_RADIUS; r += 4) {
            int samples = r == 0 ? 1 : 24;
            for (int i = 0; i < samples; i++) {
                double angle = Math.PI * 2 * i / samples;
                float sx = cx + (float) Math.cos(angle) * r;
                float sy = cy + (float) Math.sin(angle) * r;
                if (floor.canStand(sx, sy)) {
                    return new float[]{sx, sy};
                }
            }
        }
        return null;
    }

    /** True when Ageha can walk in a straight line from a to b without bumping into anything. */
    boolean lineFree(float ax, float ay, float bx, float by) {
        float length = (float) Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay));
        int steps = Math.max(1, (int) Math.ceil(length / 3));
        for (int s = 1; s <= steps; s++) {
            float t = (float) s / steps;
            if (!floor.canStand(ax + (bx - ax) * t, ay + (by - ay) * t)) return false;
        }
        return true;
    }

    /** The free square closest to this point (looks up to 8 squares around it), or -1. */
    private int nearestFreeCell(float wx, float wy) {
        int ci = (int) (wx / CELL), cj = (int) ((wy - originY) / CELL);
        int best = -1;
        float bestDistance = Float.MAX_VALUE;
        for (int j = cj - 8; j <= cj + 8; j++) {
            for (int i = ci - 8; i <= ci + 8; i++) {
                if (i < 0 || j < 0 || i >= cols || j >= rows || !free[j * cols + i]) continue;
                float dx = cellX(i) - wx, dy = cellY(j) - wy;
                float d = dx * dx + dy * dy;
                if (d < bestDistance) {
                    bestDistance = d;
                    best = j * cols + i;
                }
            }
        }
        return best;
    }

    private float cellDistance(int a, int b) {
        float dx = cellX(a % cols) - cellX(b % cols);
        float dy = cellY(a / cols) - cellY(b / cols);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * The way from (sx, sy) to (gx, gy) as a list of points to walk to (the last one is the end).
     * If the goal can't be reached, the way ends at the closest point that can be reached.
     */
    List<float[]> findPath(float sx, float sy, float gx, float gy) {
        List<float[]> result = new ArrayList<>();
        if (lineFree(sx, sy, gx, gy)) {
            result.add(new float[]{gx, gy});        // nothing in the way: just walk there
            return result;
        }
        int start = nearestFreeCell(sx, sy), goal = nearestFreeCell(gx, gy);
        if (start < 0 || goal < 0) {
            result.add(new float[]{gx, gy});
            return result;
        }

        // A* over the squares
        int n = free.length;
        float[] cost = new float[n];
        Arrays.fill(cost, Float.MAX_VALUE);
        int[] cameFrom = new int[n];
        Arrays.fill(cameFrom, -1);
        boolean[] done = new boolean[n];
        PriorityQueue<float[]> open = new PriorityQueue<>((a, b) -> Float.compare(a[0], b[0]));
        cost[start] = 0;
        open.add(new float[]{cellDistance(start, goal), start});
        int closest = start;                        // if the goal can't be reached: get as close as possible
        float closestDistance = cellDistance(start, goal);
        while (!open.isEmpty()) {
            int c = (int) open.poll()[1];
            if (done[c]) continue;
            done[c] = true;
            float d = cellDistance(c, goal);
            if (d < closestDistance) {
                closestDistance = d;
                closest = c;
            }
            if (c == goal) break;
            int ci = c % cols, cj = c / cols;
            for (int dj = -1; dj <= 1; dj++) {
                for (int di = -1; di <= 1; di++) {
                    if (di == 0 && dj == 0) continue;
                    int ni = ci + di, nj = cj + dj;
                    if (ni < 0 || nj < 0 || ni >= cols || nj >= rows) continue;
                    int next = nj * cols + ni;
                    if (!free[next] || done[next]) continue;
                    boolean diagonal = di != 0 && dj != 0;
                    // no cutting corners: going diagonally needs both squares beside it free
                    if (diagonal && (!free[cj * cols + ni] || !free[nj * cols + ci])) continue;
                    float newCost = cost[c] + (diagonal ? 1.4142f : 1f) * CELL;
                    if (newCost < cost[next]) {
                        cost[next] = newCost;
                        cameFrom[next] = c;
                        open.add(new float[]{newCost + cellDistance(next, goal), next});
                    }
                }
            }
        }
        int end = done[goal] ? goal : closest;
        List<float[]> points = new ArrayList<>();
        for (int c = end; c != -1; c = cameFrom[c]) {
            points.add(0, new float[]{cellX(c % cols), cellY(c / cols)});
        }
        if (end == goal) {
            points.add(new float[]{gx, gy});        // finally the exact spot that was clicked
        }

        // straighten: from where he is, go straight to the farthest point he can walk to directly
        float px = sx, py = sy;
        int k = 0;
        while (k < points.size()) {
            int far = k;
            while (far + 1 < points.size() && lineFree(px, py, points.get(far + 1)[0], points.get(far + 1)[1])) {
                far++;
            }
            float[] p = points.get(far);
            result.add(p);
            px = p[0];
            py = p[1];
            k = far + 1;
        }
        return result;
    }
}
