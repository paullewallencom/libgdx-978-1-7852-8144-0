package com.packt.nutty;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.HashMap;
import java.util.Map;

public class GameScreen extends ScreenAdapter {

    private static final float WORLD_WIDTH = 960;
    private static final float WORLD_HEIGHT = 544;

    private static final float UNITS_PER_METER = 32F;
    private static final float UNIT_WIDTH = WORLD_WIDTH / UNITS_PER_METER;
    private static final float UNIT_HEIGHT = WORLD_HEIGHT / UNITS_PER_METER;

    private static final float MAX_STRENGTH = 15;
    private static final float MAX_DISTANCE = 100;
    private static final float UPPER_ANGLE = 3 * MathUtils.PI / 2f;
    private static final float LOWER_ANGLE = MathUtils.PI / 2f;

    private final Vector2 anchor = new Vector2(convertMetresToUnits(6.125f), convertMetresToUnits(5.75f));
    private final Vector2 firingPosition = anchor.cpy();

    private ShapeRenderer shapeRenderer;
    private Viewport viewport;
    private OrthographicCamera camera;

    private SpriteBatch batch;

    private World world;
    private OrthographicCamera box2dCam;
    private Box2DDebugRenderer debugRenderer;

    private ObjectMap<Body, Sprite> sprites = new ObjectMap<>();
    private Array<Body> toRemove = new Array<>();

    private TiledMap tiledMap;
    private OrthogonalTiledMapRenderer orthogonalTiledMapRenderer;

    private float distance;
    private float angle;

    private Sprite slingshot;
    private Sprite squirrel;
    private Sprite staticAcorn;

    private final NuttyGame nuttyGame;

    public GameScreen(NuttyGame nuttyGame) {
        this.nuttyGame = nuttyGame;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        viewport.update(width, height);
    }

    @Override
    public void show() {
        super.show();
        world = new World(new Vector2(0, -10F), true);
        debugRenderer = new Box2DDebugRenderer();
        box2dCam = new OrthographicCamera(UNIT_WIDTH, UNIT_HEIGHT);

        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        viewport.apply(true);

        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();

        tiledMap = nuttyGame.getAssetManager().get("nuttybirds.tmx");
        orthogonalTiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap, batch);
        orthogonalTiledMapRenderer.setView(camera);

        TiledObjectBodyBuilder.buildFloorBodies(tiledMap, world);
        TiledObjectBodyBuilder.buildBuildingBodies(tiledMap, world);
        TiledObjectBodyBuilder.buildBirdBodies(tiledMap, world);

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                calculateAngleAndDistanceForBullet(screenX, screenY);
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                createBullet();
                firingPosition.set(anchor.cpy());
                return true;
            }

        });

        world.setContactListener(new NuttyContactListener());

        Array<Body> bodies = new Array<>();
        world.getBodies(bodies);
        for (Body body : bodies) {
            Sprite sprite = SpriteGenerator.generateSpriteForBody(nuttyGame.getAssetManager(), body);
            if (sprite != null) sprites.put(body, sprite);
        }

        slingshot = new Sprite(nuttyGame.getAssetManager().get("slingshot.png", Texture.class));
        slingshot.setPosition(170, 64);
        squirrel = new Sprite(nuttyGame.getAssetManager().get("squirrel.png", Texture.class));
        squirrel.setPosition(32, 64);
        staticAcorn = new Sprite(nuttyGame.getAssetManager().get("acorn.png", Texture.class));
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
        clearDeadBodies();
        world.step(delta, 6, 2);
        box2dCam.position.set(UNIT_WIDTH / 2, UNIT_HEIGHT / 2, 0);
        box2dCam.update();
        updateSpritePositions();
    }

    private void clearDeadBodies() {
        for (Body body : toRemove) {
            sprites.remove(body);
            world.destroyBody(body);
        }
        toRemove.clear();
    }

    private void updateSpritePositions() {
        for (Body body : sprites.keys()) {
            Sprite sprite = sprites.get(body);
            sprite.setPosition(
                    convertMetresToUnits(body.getPosition().x) - sprite.getWidth() / 2f,
                    convertMetresToUnits(body.getPosition().y) - sprite.getHeight() / 2f);
            sprite.setRotation(MathUtils.radiansToDegrees * body.getAngle());
        }
        staticAcorn.setPosition(firingPosition.x - staticAcorn.getWidth() / 2f, firingPosition.y - staticAcorn.getHeight() / 2f);
    }

    private void calculateAngleAndDistanceForBullet(int screenX, int screenY) {
        firingPosition.set(screenX, screenY);
        viewport.unproject(firingPosition);
        distance = distanceBetweenTwoPoints();
        angle = angleBetweenTwoPoints();

        if (distance > MAX_DISTANCE) {
            distance = MAX_DISTANCE;
        }
        if (angle > LOWER_ANGLE) {
            if (angle > UPPER_ANGLE) {
                angle = 0;
            } else {
                angle = LOWER_ANGLE;
            }
        }
        firingPosition.set(anchor.x + (distance * -MathUtils.cos(angle)), anchor.y + (distance * -MathUtils.sin(angle)));
    }

    private float angleBetweenTwoPoints() {
        float angle = MathUtils.atan2(anchor.y - firingPosition.y, anchor.x - firingPosition.x);
        angle %= 2 * MathUtils.PI;
        if (angle < 0) angle += MathUtils.PI2;
        return angle;
    }

    private float distanceBetweenTwoPoints() {
        return (float) Math.sqrt(((anchor.x - firingPosition.x) * (anchor.x - firingPosition.x)) + ((anchor.y - firingPosition.y) * (anchor.y - firingPosition.y)));
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
        for (Sprite sprite : sprites.values()) {
            sprite.draw(batch);
        }
        squirrel.draw(batch);
        staticAcorn.draw(batch);
        slingshot.draw(batch);
        batch.end();
    }

    private void drawDebug() {
        debugRenderer.render(world, box2dCam.combined);
        shapeRenderer.setProjectionMatrix(camera.projection);
        shapeRenderer.setTransformMatrix(camera.view);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.rect(anchor.x - 5, anchor.y - 5, 10, 10);
        shapeRenderer.rect(firingPosition.x - 5, firingPosition.y - 5, 10, 10);
        shapeRenderer.line(anchor.x, anchor.y, firingPosition.x, firingPosition.y);
        shapeRenderer.end();
    }

    private void createBullet() {
        CircleShape circleShape = new CircleShape();
        circleShape.setRadius(0.5f);
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.DynamicBody;
        Body bullet = world.createBody(bd);
        bullet.setUserData("acorn");
        bullet.createFixture(circleShape, 1);
        bullet.setTransform(new Vector2(convertUnitsToMetres(firingPosition.x), convertUnitsToMetres(firingPosition.y)), 0);

        Sprite sprite = new Sprite(nuttyGame.getAssetManager().get("acorn.png", Texture.class));
        sprite.setOrigin(sprite.getWidth() / 2, sprite.getHeight() / 2);
        sprites.put(bullet, sprite);

        circleShape.dispose();

        float velX = Math.abs((MAX_STRENGTH * -MathUtils.cos(angle) * (distance / 100f)));
        float velY = Math.abs((MAX_STRENGTH * -MathUtils.sin(angle) * (distance / 100f)));

        bullet.setLinearVelocity(velX, velY);
    }


    private float convertUnitsToMetres(float pixels) {
        return pixels / UNITS_PER_METER;
    }

    private float convertMetresToUnits(float metres) {
        return metres * UNITS_PER_METER;
    }

    private class NuttyContactListener implements ContactListener {

        @Override
        public void beginContact(Contact contact) {
            if (contact.isTouching()) {
                Fixture attacker = contact.getFixtureA();
                Fixture defender = contact.getFixtureB();
                WorldManifold worldManifold = contact.getWorldManifold();
                if ("enemy".equals(defender.getUserData())) {
                    Vector2 vel1 = attacker.getBody().getLinearVelocityFromWorldPoint(worldManifold.getPoints()[0]);
                    Vector2 vel2 = defender.getBody().getLinearVelocityFromWorldPoint(worldManifold.getPoints()[0]);
                    Vector2 impactVelocity = vel1.sub(vel2);
                    if (Math.abs(impactVelocity.x) > 1 || Math.abs(impactVelocity.y) > 1) {
                        toRemove.add(defender.getBody());
                    }
                }
            }
        }

        @Override
        public void endContact(Contact contact) {

        }

        @Override
        public void preSolve(Contact contact, Manifold oldManifold) {

        }

        @Override
        public void postSolve(Contact contact, ContactImpulse impulse) {

        }
    }
}
