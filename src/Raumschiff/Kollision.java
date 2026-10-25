package Raumschiff;

import java.awt.*;

public class Kollision {

    //
    public static boolean checkCollisionBetweenCircles(GameObject object1, GameObject object2) { // Prüfen ob zwei runde Objekte kollidieren
        // Mittelpunkt und Radius des ersten Kreisobjekts berechnen
        double x1 = object1.getObjectPosition().getX() + object1.getWidth() / 2; // x- Koordinate des Objekts
        double y1 = object1.getObjectPosition().getY() + object1.getHeight() / 2; // y- Koordinate des Objekts
        double r1 = object1.getHeight() / 2; // Radius des Objekts

        // Mittelpunkt und Radius des zweiten Kreisobjekts berechnen
        double x2 = object2.getObjectPosition().getX() + object2.getWidth() / 2; // x- Koordinate des
        double y2 = object2.getObjectPosition().getY() + object2.getHeight() / 2; // x- Koordinate des
        double r2 = object2.getHeight() / 2; // asteroid-Radius

        // 1. Schritt: Schneller Test mit AABB(Axis Aligned Bounding Box) Kollisionsüberprüfung
        // um zu sehen, ob es zwichen den beiden Kreisobjekten überhaupt zu einer Kollision kommen könnte
        double maxDistance = r1 + r2;
        if (Math.abs(x1 - x2) > maxDistance || Math.abs(y1 - y2) > maxDistance) {
            return false;
        }
        // 2. Schritt: Ausführlicher Test ob beide Objekte wirklich kollidieren

        double dx = x1 - x2;
        double dy = y1 - y2;
        double rsum = r1 + r2;
        double hypote = (dx * dx) + (dy * dy);

        return hypote <= rsum * rsum;
    }

    public static boolean checkPlayerAsteroidCollision(Player player, Asteroid asteroid) {



        double mx = asteroid.getObjectPosition().getX() + asteroid.getWidth() / 2;
        double my = asteroid.getObjectPosition().getY() + asteroid.getHeight() / 2;
        double mr = asteroid.getHeight() / 2;

        //1. Schnelltest mithilfe von rechteckigen Bounding Boxen (AABB),
        // wenn sich diese nicht überschneiden, ist eine Kollision nicht möglich

        double px = player.getObjectPosition().getX();
        double py = player.getObjectPosition().getY() - 3.25;
        double pr = 18.75;

        double maxDistancePlayerAsteroid = pr + mr;
        if (Math.abs(px - mx) > maxDistancePlayerAsteroid || Math.abs(py - my) > maxDistancePlayerAsteroid) {
            return false;
        }

        Coordinate[] playerPoints = player.getPolgygonHitbox();

        Polygon poly = new Polygon(
                new int[]{(int)playerPoints[0].getX(), (int)playerPoints[1].getX(), (int)playerPoints[2].getX(), (int)playerPoints[3].getX()},
                new int[]{(int)playerPoints[0].getY(), (int)playerPoints[1].getY(), (int)playerPoints[2].getY(), (int)playerPoints[3].getY()},
                4
        );
        if (poly.contains(mx, my)) {
            return true;
        }

        double minDistanceSquared = Double.MAX_VALUE;
        for (int i = 0; i < playerPoints.length; i++) {
            double ax = playerPoints[i].getX();
            double bx = playerPoints[(i + 1) % playerPoints.length].getX();
            double ay = playerPoints[i].getY();
            double by = playerPoints[(i + 1) % playerPoints.length].getY();

            double baX = bx - ax;
            double baY = by - bx;
            double maX = mx - ax;
            double maY = my - ay;

            double baLenghtSquared = (baX * baX) + (baY * baY);
            double qx;
            double qy;

            if(baLenghtSquared == 0){
                qx = ax;
                qy = ay;
            }
            else {
                double dotProduct = (maX * bx) + (maY * by);
                double t = dotProduct/baLenghtSquared;

                qx = ax + Math.clamp(t,0,1) * baX;
                qy = ay + Math.clamp(t,0,1) * baY;
            }

            double qmDistanceSquared = (qx- mx) * (qx - mx) + (qy- my) * (qy - my);
            if (qmDistanceSquared < minDistanceSquared){
                minDistanceSquared = qmDistanceSquared;
            }

        }
        return minDistanceSquared <= (mr * mr);
    }
}


