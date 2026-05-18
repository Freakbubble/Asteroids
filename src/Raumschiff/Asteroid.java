package Raumschiff;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

public class Asteroid extends GameObject {

	private Color color;

	// Attribute der Klasse Enemy
	public Asteroid(Coordinate objectPosition, double width, double height, double movingAngle, double movingDistance,
			Color c) {

		// TODO Auto-generated constructor stub
		super(objectPosition, width, height);
		setMovingAngle(movingAngle); // Achtung der Winkel wird im Bogenmass angegeben!!
		setMovingDistance(movingDistance);

		color = c;
	}

	@Override
	public void makeMove() {
		// TODO Auto-generated method stub
		super.makeMove();
	}

	public void bounce() {
		this.setMovingDistance(-movingDistance);
	}

	@Override
	protected void paintMe(Graphics g) {
		// TODO Auto-generated method stub

		Graphics2D g2d = (Graphics2D) g;
		g2d.setColor(color);

		Ellipse2D.Double asteroid = new Ellipse2D.Double(getObjectPosition().getX(), getObjectPosition().getY(),
				getWidth(), getHeight());

		g2d.draw(asteroid);
		g2d.fill(asteroid);
	}
}