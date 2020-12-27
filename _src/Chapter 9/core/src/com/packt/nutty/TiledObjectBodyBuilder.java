package com.packt.nutty;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Ellipse;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

/**
 * Created by Cookie on 12/01/2015.
 */
public class TiledObjectBodyBuilder {

    private static final float PIXELS_PER_TILE = 32F;
    private static final float HALF = 0.5F;

    public static void buildBirdBodies(TiledMap tiledMap, World world) {
        MapObjects objects = tiledMap.getLayers().get("Physics_Birds").getObjects();

        for (MapObject object : objects) {
            EllipseMapObject ellipseMapObject = (EllipseMapObject) object;
            CircleShape circle = getCircle(ellipseMapObject);
            BodyDef bd = new BodyDef();
            bd.type = BodyDef.BodyType.DynamicBody;
            Body body = world.createBody(bd);
            Fixture fixture = body.createFixture(circle, 1);
            fixture.setUserData("enemy");
            body.setUserData("enemy");

            Ellipse ellipse = ellipseMapObject.getEllipse();
            body.setTransform(new Vector2((ellipse.x + ellipse.width * HALF) / PIXELS_PER_TILE, (ellipse.y + ellipse.height * HALF) / PIXELS_PER_TILE), 0);
            circle.dispose();
        }
    }

    public static void buildBuildingBodies(TiledMap tiledMap, World world) {
        MapObjects objects = tiledMap.getLayers().get("Physics_Buildings").getObjects();

        for (MapObject object : objects) {
            RectangleMapObject rectangleMapObject = (RectangleMapObject) object;
            PolygonShape rectangle = getRectangle(rectangleMapObject);
            BodyDef bd = new BodyDef();
            bd.type = BodyDef.BodyType.DynamicBody;
            Body body = world.createBody(bd);

            if (rectangleMapObject.getRectangle().width > rectangleMapObject.getRectangle().height) {
                body.setUserData("horizontal");
            } else {
                body.setUserData("vertical");
            }

            body.createFixture(rectangle, 1);
            body.setTransform(getTransformForRectangle(rectangleMapObject.getRectangle()), 0);
            rectangle.dispose();
        }
    }

    public static void buildFloorBodies(TiledMap tiledMap, World world) {
        MapObjects objects = tiledMap.getLayers().get("Physics_Floor").getObjects();

        for (MapObject object : objects) {
            RectangleMapObject rectangleMapObject = (RectangleMapObject) object;
            PolygonShape rectangle = getRectangle(rectangleMapObject);
            BodyDef bd = new BodyDef();
            bd.type = BodyDef.BodyType.StaticBody;
            Body body = world.createBody(bd);
            body.setUserData("floor");
            body.createFixture(rectangle, 1);
            body.setTransform(getTransformForRectangle(rectangleMapObject.getRectangle()), 0);
            rectangle.dispose();
        }
    }

    private static PolygonShape getRectangle(RectangleMapObject rectangleObject) {
        Rectangle rectangle = rectangleObject.getRectangle();
        PolygonShape polygon = new PolygonShape();
        polygon.setAsBox(rectangle.width * HALF / PIXELS_PER_TILE, rectangle.height * HALF / PIXELS_PER_TILE);
        return polygon;
    }

    private static CircleShape getCircle(EllipseMapObject ellipseObject) {
        Ellipse ellipse = ellipseObject.getEllipse();
        CircleShape circleShape = new CircleShape();
        circleShape.setRadius(ellipse.width * HALF / PIXELS_PER_TILE);
        return circleShape;
    }

    private static Vector2 getTransformForRectangle(Rectangle rectangle) {
        return new Vector2((rectangle.x + (rectangle.width * HALF)) / PIXELS_PER_TILE, (rectangle.y  + (rectangle.height * HALF)) / PIXELS_PER_TILE);
    }


}