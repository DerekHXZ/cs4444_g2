package pb.g2;

public class ComparableAsteroid implements Comparable<ComparableAsteroid> {
    public int index;
    public double radius;

    public ComparableAsteroid(int index, double radius) {
        this.index = index;
        this.radius = radius;
    }

    public int compareTo(ComparableAsteroid other) {
        if (radius > other.radius) {
            return 1;
        } else if (radius < other.radius) {
            return -1;
        } else {
            return 0;
        }
    }
}
