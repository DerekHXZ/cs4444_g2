package pb.g2;

import pb.sim.Asteroid;

public class Push implements Comparable<Push> {
    Asteroid asteroid;
    int index;
    double energy;
    double direction;
    long time;
    long expected_collision_time;

    Push(Asteroid asteroid, int index, double energy, double direction, long time, long expected_collision_time) {
        this.asteroid = asteroid;
        this.index = index;
        this.energy = energy;
        this.direction = direction;
        this.time = time;
        this.expected_collision_time = expected_collision_time;
    }

    @Override
    public int compareTo(Push o) {
        return Double.valueOf(energy).compareTo(o.energy);
    }
}
