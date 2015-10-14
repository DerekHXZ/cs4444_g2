package pb.g2;

public class ComparableAsteroid implements Comparable<ComparableAsteroid> {
    public int index;
    public double radius;
    public double mass;

    public ComparableAsteroid(int index, double radius, double mass) {
        this.index = index;
        this.radius = radius;
        this.mass = mass;
    }

    private double getScore() {
        return radius*radius*mass;
    }

    public int compareTo(ComparableAsteroid other) {
        return Double.valueOf(this.getScore()).compareTo(other.getScore());
    }
}
