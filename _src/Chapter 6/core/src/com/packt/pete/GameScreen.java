package com.packt.pete;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by James on 17/03/2015.
 */
public class GameScreen extends ScreenAdapter {

    private static final float CELL_SIZE = 16;

    private static final float WORLD_WIDTH = 640;
    private static final float WORLD_HEIGHT = 480;

    private ShapeRenderer shapeRenderer;
    private Viewport viewport;
    private OrthographicCamera camera;

    private SpriteBatch batch;

    private TiledMap tiledMap;
    private OrthogonalTiledMapRenderer orthogonalTiledMapRenderer;

    private Pete pete;
    private Array<Acorn> acorns = new Array<Acorn>();

    private final PeteGame peteGame;

    public GameScreen(PeteGame peteGame) {
        this.peteGame = peteGame;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        viewport.update(width, height);
    }

    @Override
    public void show() {
        super.show();
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        viewport.apply(true);

        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();

        tiledMap = peteGame.getAssetManager().get("pete.tmx");
        orthogonalTiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap, batch);
        orthogonalTiledMapRenderer.setView(camera);

        pete = new Pete(peteGame.getAssetManager().get("pete.png", Texture.class));
        pete.setPosition(0, WORLD_HEIGHT / 2);

        populateAcorns();
    }

    private void populateAcorns() {

        MapLayer mapLayer = tiledMap.getLayers().get("Collectables");
        for (MapObject mapObject : mapLayer.getObjects()) {
            acorns.add(
                    new Acorn(
                            peteGame.getAssetManager().get("acorn.png", Texture.class),
                            mapObject.getProperties().get("x", Float.class),
                            mapObject.getProperties().get("y", Float.class)
                    )
            );
        }
    }

    private Array<CollisionCell> whichCellsDoesPeteCover() {
        float x = pete.getX();
        float y = pete.getY();

        Array<CollisionCell> cellsCovered = new Array<CollisionCell>();

        float cellX = x / CELL_SIZE;
        float cellY = y / CELL_SIZE;

        int bottomLeftCellX = MathUtils.floor(cellX);
        int bottomLeftCellY = MathUtils.floor(cellY);

        TiledMapTileLayer tiledMapTileLayer = (TiledMapTileLayer) tiledMap.getLayers().get(0);

        cellsCovered.add(new CollisionCell(tiledMapTileLayer.getCell(bottomLeftCellX, bottomLeftCellY), bottomLeftCellX, bottomLeftCellY));

        if (cellX % 1 != 0 && cellY % 1 != 0) {
            int topRightCellX = bottomLeftCellX + 1;
            int topRightCellY = bottomLeftCellY + 1;
            cellsCovered.add(new CollisionCell(tiledMapTileLayer.getCell(topRightCellX, topRightCellY), topRightCellX, topRightCellY));
        }

        if (cellX % 1 != 0) {
            int bottomRightCellX = bottomLeftCellX + 1;
            int bottomRightCellY = bottomLeftCellY;
            cellsCovered.add(new CollisionCell(tiledMapTileLayer.getCell(bottomRightCellX, bottomRightCellY), bottomRightCellX, bottomRightCellY));
        }

        if (cellY % 1 != 0) {
            int topLeftCellX = bottomLeftCellX;
            int topLeftCellY = bottomLeftCellY + 1;
            cellsCovered.add(new CollisionCell(tiledMapTileLayer.getCell(topLeftCellX, topLeftCellY), topLeftCellX, topLeftCellY));
        }

        return cellsCovered;
    }

    private Array<CollisionCell> filterOutNonTiledCells(Array<CollisionCell> cells) {
        for (Iterator<CollisionCell> iter = cells.iterator(); iter.hasNext(); ) {
            CollisionCell collisionCell = iter.next();
            if (collisionCell.isEmpty()) {
                iter.remove();
            }
        }
        return cells;
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        update(delta);
        clearScreen();
        draw();
//        drawDebug();
    }

    private void update(float delta) {
        pete.update(delta);
        stopPeteLeavingTheScreen();
        handlePeteCollision();
        handlePeteCollisionWithAcorn();
    }

    private void handlePeteCollision() {
        Array<CollisionCell> peteCells = whichCellsDoesPeteCover();
        peteCells = filterOutNonTiledCells(peteCells);

        for (CollisionCell cell : peteCells) {
            float cellLevelX = cell.cellX * CELL_SIZE;
            float cellLevelY = cell.cellY * CELL_SIZE;

            Rectangle intersection = new Rectangle();
            Intersector.intersectRectangles(pete.getCollisionRectangle(), new Rectangle(cellLevelX, cellLevelY, CELL_SIZE, CELL_SIZE), intersection);

            if (intersection.getHeight() < intersection.getWidth()) {
                pete.setPosition(pete.getX(), intersection.getY() + intersection.getHeight());
                pete.landed();
            } else if (intersection.getWidth() < intersection.getHeight()) {
                if (intersection.getX() == pete.getX()) {
                    pete.setPosition(intersection.getX() + intersection.getWidth(), pete.getY());
                }
                if (intersection.getX() > pete.getX()) {
                    pete.setPosition(intersection.getX() - Pete.WIDTH, pete.getY());
                }
            }
        }
    }

    private void stopPeteLeavingTheScreen() {
        if (pete.getY() < 0) {
            pete.setPosition(pete.getX(), 0);
            pete.landed();
        }
        if (pete.getX() < 0) {
            pete.setPosition(0, pete.getY());
        }
        if (pete.getX() + Pete.WIDTH > WORLD_WIDTH) {
            pete.setPosition(WORLD_WIDTH - Pete.WIDTH, pete.getY());
        }
    }

    private void handlePeteCollisionWithAcorn() {
        for (Iterator<Acorn> iter = acorns.iterator(); iter.hasNext(); ) {
            Acorn acorn = iter.next();
            if (pete.getCollisionRectangle().overlaps(acorn.getCollisionRectangle())){
                iter.remove();
            }
        }
    }

    private void clearScreen() {
        Gdx.gl.glClearColor(Color.TEAL.r, Color.TEAL.g, Color.TEAL.b, Color.TEAL.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    private void draw() {
        batch.setProjectionMatrix(camera.projection);
        batch.setTransformMatrix(camera.view);
        orthogonalTiledMapRenderer.render();
        batch.begin();
        for (Acorn acorn : acorns) {
            acorn.draw(batch);
        }
        pete.draw(batch);
        batch.end();
    }

    private void drawDebug() {
        shapeRenderer.setProjectionMatrix(camera.projection);
        shapeRenderer.setTransformMatrix(camera.view);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        pete.drawDebug(shapeRenderer);
        shapeRenderer.end();
    }

    private class CollisionCell {

        private final TiledMapTileLayer.Cell cell;
        private final int cellX;
        private final int cellY;

        public CollisionCell(TiledMapTileLayer.Cell cell, int cellX, int cellY) {
            this.cell = cell;
            this.cellX = cellX;
            this.cellY = cellY;
        }

        public boolean isEmpty() {
            return cell == null;
        }

    }

}
