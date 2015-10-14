package pb.g2;

public class Push {
    long id;
    double energy;
    double direction;
    long expected_collision_time;

    Push(long id, double energy, double direction, long expected_collision_time) {
        this.id = id;
        this.energy = energy;
        this.direction = direction;
        this.expected_collision_time = expected_collision_time;
    }
}
