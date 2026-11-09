package Raumschiff;

import java.awt.*;
import java.awt.geom.Ellipse2D;

public class Ore extends GameObject{

    private int value = 1;
    private long createdAtMillis = -1;
    private static final long LifetimeMillis = 4_000L;

    public Ore(Coordinate objectPosition, double width, double height) {
        super(objectPosition, width, height);
    }


    public void setValue(int newValue) {
        value = newValue;
    }

    public int getValue(){
        return value;
    }


    // Das Erz verschwindet nach 4s.
    // Die Lebensdauer wird in aktiver Spielzeit gemessen und verschwindet nicht wenn das Spiel pausiert wird
    public boolean isExpired(long elapsedGameMillis) {
        if (createdAtMillis < 0){
            createdAtMillis = elapsedGameMillis;
        }
        return elapsedGameMillis - createdAtMillis >= LifetimeMillis;
    }

    @Override
    protected void paintMe(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.GREEN);

        Ellipse2D.Double ore = new Ellipse2D.Double(getObjectPosition().getX(), getObjectPosition().getY(), getWidth(),
                getHeight());

        g2d.draw(ore);
        g2d.fill(ore);
    }
}
