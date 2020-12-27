package com.packt.nutty;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.Pool;

/**
 * Created by James on 15/06/2015.
 */
public class Acorn implements Pool.Poolable {

    private final Sprite sprite;

    public Acorn(AssetManager assetManager) {
        sprite = new Sprite(assetManager.get("acorn.png", Texture.class));
        sprite.setOrigin(sprite.getWidth() / 2, sprite.getHeight() / 2);
    }

    public void setPosition(float x, float y) {
        sprite.setPosition(x, y);
    }

    public void setRotation(float degrees) {
        sprite.setRotation(degrees);
    }

    public float getWidth() {
        return sprite.getWidth();
    }

    public float getHeight() {
        return sprite.getHeight();
    }

    public void draw(Batch batch) {
        sprite.draw(batch);
    }

    @Override
    public void reset() {
        sprite.setPosition(0,0);
        sprite.setRotation(0F);
    }
}
