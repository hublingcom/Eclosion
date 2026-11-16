
package nl.saxion.game.yourgamename;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Rectangle;
import nl.saxion.gameapp.GameApp;

public class Ageha {

    // Player position
    private float playerX;
    private float playerY;

    public float centerX(){
        return playerX + 40;
    }
    public float feetY(){
        return playerY + 5;
    }

    private final int speed = 5;

    // Collision rectangle
    private Rectangle playerBounds = new Rectangle();

    // Walking animation
    private float animationTimer = 0;
    private float animationSpeed = 0.14f;

    private boolean walking = false;

    private String currentDirection = "down";
    private String currentFrame = "idle";

    // Constructor: determines where Ageha starts
    public Ageha(float startX, float startY) {
        playerX = startX;
        playerY = startY;
    }
    public void setPosition(float x, float y) {
        playerX = x;
        playerY = y;

        playerBounds.set(playerX + 20, playerY + 5, 40, 30);
    }

    // Checks whether Ageha can move to a position
    private boolean canMoveTo(float x, float y,
                              Rectangle[] walkableAreas,
                              Rectangle[] obstacles) {

        playerBounds.set(x + 20, y + 5, 40, 30);

        // Check whether Ageha is inside at least one walkable area
        boolean insideWalkableArea = false;

        for (Rectangle area : walkableAreas) {
            if (area.overlaps(playerBounds) || area.contains(playerBounds)) {
                insideWalkableArea = true;
                break;
            }
        }

        if (!insideWalkableArea) {
            return false;
        }

        // Check furniture and other obstacles
        for (Rectangle obstacle : obstacles) {
            if (playerBounds.overlaps(obstacle)) {
                return false;
            }
        }

        return true;
    }

    // Handles keyboard movement and animation

    public void update(float delta,
                       Rectangle[] walkableAreas,
                       Rectangle... obstacles) {

        float oldX = playerX;
        float oldY = playerY;

        float nextX = playerX;
        float nextY = playerY;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            nextY += speed;
            currentDirection = "up";
        }

        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            nextY -= speed;
            currentDirection = "down";
        }

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            nextX -= speed;
            currentDirection = "left";
        }

        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            nextX += speed;
            currentDirection = "right";
        }

        if (canMoveTo(nextX, playerY, walkableAreas, obstacles)) {
            playerX = nextX;
        }

        if (canMoveTo(playerX, nextY, walkableAreas, obstacles)) {
            playerY = nextY;
        }

        playerBounds.set(playerX + 20, playerY + 5, 40, 30);

        walking = playerX != oldX || playerY != oldY;
    }



    // Draws Ageha using the correct animation frame
    public void draw(float delta) {

        if (walking) {
            animationTimer += delta;

            if (animationTimer >= animationSpeed) {
                animationTimer = 0;

                if (currentFrame.equals("a")) {
                    currentFrame = "idle";
                } else if(currentFrame.equals("idle")){
                    currentFrame = "b";
                }else{
                    currentFrame = "a";
                }
            }
        } else {
            currentFrame = "idle";
            animationTimer = 0;
        }

        String texture = "ageha_" + currentDirection + "_" + currentFrame;

        GameApp.drawTexture(texture, playerX, playerY, 80, 100);
    }
    public boolean isNearDoor(Rectangle doorBounds) {
        return playerBounds.overlaps(doorBounds);
    }
}

