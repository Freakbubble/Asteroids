package Raumschiff;

import java.awt.*;
import java.awt.geom.Ellipse2D;

public class Ore extends GameObject{

    private int value;
    private final long createdAtMills;
    private static final long LifetimeMills = 5_000L;

    public Ore(Coordinate objectPosition, double width, double height) {
        super(objectPosition, width, height);
        createdAtMills = System.currentTimeMillis();
    }

    public void setValue(int newValue) {
        value = newValue;
    }

    public int getValue(){
        return value;
    }

    public boolean isExpired(){
        return System.currentTimeMillis() - createdAtMills > LifetimeMills;
    }

    @Override
    protected void paintMe(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(new Color(203, 91, 10));

        Ellipse2D.Double ore = new Ellipse2D.Double(getObjectPosition().getX(), getObjectPosition().getY(), getWidth(),
                getHeight());

        g2d.draw(ore);
        g2d.fill(ore);
    }
}
