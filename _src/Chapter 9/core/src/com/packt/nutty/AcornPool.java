package com.packt.nutty;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.utils.Pool;

/**
 * Created by James on 15/06/2015.
 */
public class AcornPool extends Pool<Acorn> {

    public static final int ACORN_COUNT = 3;

    private final AssetManager assetManager;

    public AcornPool(AssetManager assetManager) {
        super(ACORN_COUNT,ACORN_COUNT);
        this.assetManager = assetManager;
    }

    @Override
    protected Acorn newObject() {
        return new Acorn(assetManager);
    }
}
