package com.packt.pete;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;

/**
 * Created by James on 29/04/2015.
 */
public class Acorn {

    public static final int WIDTH = 16;
    public static final int HEIGHT = 16;

    private final Rectangle collision;

    private final Texture texture;
    private final float x;
    private final float y;

    public Acorn(Texture texture, float x, float y) {
        this.texture = texture;
        this.x = x;
        this.y = y;
        this.collision = new Rectangle(x,y, WIDTH,HEIGHT);
    }

    public Rectangle getCollisionRectangle() {
        return collision;
    }

    public void draw(Batch batch) {
        batch.draw(texture, x, y);
    }

}
