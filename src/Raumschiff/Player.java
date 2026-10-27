package Raumschiff;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;

public class Player extends GameObject {
	// Variablen
	int shotsize = 3;
	int shotspeed = -8;
	private double acceleration = -0.25;
	private double maxSpeed = -5;

	public Player(Coordinate objectPosition, double width, double height, double movingAngle, double movingDistance) {

		// TODO Auto-generated constructor stub
		super(objectPosition, width, height);
		setMovingAngle(movingAngle); // Achtung der Winkel wird im Bogenma� angegeben!!
		setMovingDistance(movingDistance);
	}

	@Override
	public void makeMove() {
		// TODO Auto-generated method stub
		super.makeMove();
	}

	public void setAcceleration(double new_acceleration) {
		acceleration = new_acceleration;
	}
	
	public double getAcceleration(){
		return acceleration;
	}
	
	public void setMaxSpeed(double new_maxSpeed) {
		maxSpeed = new_maxSpeed;
	}

	public double getMaxSpeed() {
		return maxSpeed;
	}

	public Shot generateShot(double movingAngle_Raumschiff) {
		Shot shot = new Shot(
				new Coordinate(this.getObjectPosition().getX() - shotsize / 2 - Math.cos(movingAngle_Raumschiff) *10,
						this.getObjectPosition().getY() - Math.sin(movingAngle_Raumschiff) * 10),
				shotsize, shotsize, movingAngle_Raumschiff, shotspeed);
		return shot;
	}

	public Coordinate[] getPolgygonHitbox(){

		double px = this.getObjectPosition().getX();
		double py = this.getObjectPosition().getY();

		double cos = Math.cos(movingAngle - Math.PI/2);
		double sin = Math.sin(movingAngle - Math.PI/2);

		// Definition der Punkte P0 (Spitze), P1 (links), P2 (Einbuchtung), P3 (rechts)
		Coordinate p0 = new Coordinate(px + 22 * sin, py - 22 * cos);
		Coordinate p1 = new Coordinate(px - 15 * cos - 8 * sin, py - 15 * sin + 8 * cos);
		Coordinate p2 = new Coordinate(px, py);
		Coordinate p3 = new Coordinate( px + 15 * cos - 8 * sin, py + 15 * sin + 8 * cos);
		return new Coordinate[]{p0, p1, p2, p3};

	}

	public void paintMe(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setColor(Color.RED);
		int x_pos = (int) this.getObjectPosition().getX();
		int y_pos = (int) this.getObjectPosition().getY();
		int[] x_poly = { x_pos, x_pos - 15, x_pos, x_pos + 15 };
		int[] y_poly = { y_pos - 22, y_pos + 8, y_pos, y_pos + 8 };
		g2d.fillPolygon(x_poly, y_poly, 4); // Zeichnung an der unteren Seite des Raumschiffs.
	}

}