package Raumschiff;

public abstract class GameObject {
	private Coordinate objectPosition;
	private double width;
	private double height;
	protected double movingAngle;   //Achtung: Winkel ist im Bogenma� angegeben!
	protected double movingDistance;
	
	
	public GameObject(Coordinate objectPosition, double width, double height) {
		this.objectPosition = objectPosition;
		this.width = width;
		this.height = height;
		movingAngle = 0;	//das Objekt wird statisch erzeugt
		movingDistance = 0; //das Objekt wird statisch erzeugt
	}

	public Coordinate getObjectPosition() {
		return objectPosition;
	}

	public void setObjectPosition(Coordinate objectPosition) {
		this.objectPosition = objectPosition;
	}

	public double getWidth() {
		return width;
	}

	public void setWidth(double width) {
		this.width = width;
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}

	public double getMovingAngle() {
		return movingAngle;
	}

	public void setMovingAngle(double movingAngle) {
		this.movingAngle = movingAngle;
	}

	public double getMovingDistance() {
		return movingDistance;
	}

	public void setMovingDistance(double movingDistance) {
		this.movingDistance = movingDistance;
	}
		
	//Durch die Angabe "static" kann die Methode �ber den Klassennamen auch von anderen Klassen aus aufgerufen werden.
	public static Coordinate polarToCartesianCoordinates(double angle) {
		//X-Achse zeigt nach Osten, Y-Achse zeigt nach Sueden beim Zeichnen
		double x = Math.cos(angle);
		double y = Math.sin(angle);
		
		return new Coordinate(x,y);
	}
	
	public void moveGameObject() {
		Coordinate direction = polarToCartesianCoordinates(movingAngle);
		
		objectPosition.setX(objectPosition.getX() + direction.getX()*movingDistance);
		objectPosition.setY(objectPosition.getY() + direction.getY()*movingDistance);
	}

	public void makeMove() {
		moveGameObject();
	}

	public boolean checkCollision(double x1, double x2, double y1, double y2, double r1, double r2) { // Prüfen ob zwei Objekte kollidieren

		double dx = x1 - x2;
		double dy = y1 - y2;
		double rsum = r1 + r2;

		double hypote = (dx * dx) + (dy * dy);
		return hypote <= rsum * rsum;
	}
	
	protected abstract void paintMe(java.awt.Graphics g);
}
