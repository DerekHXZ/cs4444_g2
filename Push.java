package pb.g2;

import pb.sim.Asteroid;

import java.util.Comparator;

public class Push {
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

    public static class EnergyComparator implements Comparator<Push> {
        @Override
        public int compare(Push a, Push b) {
            return Double.compare(a.energy, b.energy);
        }
    }

    public static class TimeComparator implements Comparator<Push> {
        @Override
        public int compare(Push a, Push b) {
            return Long.compare(a.time, b.time);
        }
    }
}
