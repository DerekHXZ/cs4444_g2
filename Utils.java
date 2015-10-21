package pb.g2;

import pb.sim.Asteroid;

public class Utils {
    /**
     * Returns index of asteroid of largest radius.
     */
    public static int largestOrbitRadius(Asteroid[] asteroids) {
        int largest = 0;
        for (int i = 1; i < asteroids.length; i++) {
            if (asteroids[i].orbit.a > asteroids[largest].orbit.a) {
                largest = i;
            }
        }
        return largest;
    }

    public static int largestMass(Asteroid[] asteroids) {
        int largest = 0;
        for (int i = 1; i < asteroids.length; i++) {
            if (asteroids[i].mass > asteroids[largest].mass) {
                largest = i;
            }
        }
        return largest;
    }

    public static double mean(Asteroid[] asteroids) {
        double mean = 0;
        for (int i = 0; i < asteroids.length; i++) {
            mean += asteroids[i].mass;
        }
        return mean/asteroids.length;
    }

    public static double stddev(Asteroid[] asteroids, double mean) {
        double stddev = 0;
        for (int i = 0; i < asteroids.length; i++) {
            stddev += Math.pow((mean - asteroids[i].mass), 2);
        }
        return Math.sqrt(mean/asteroids.length);
    }

    public static Asteroid findAsteroidById(Asteroid[] asteroids, long id) {
        for (Asteroid a : asteroids) {
            if (a.id == id) {
                return a;
            }
        }
        return null;
    }

    public static int getAsteroidIndex(Asteroid[] asteroids, long id) {
        for (int i = 0; i < asteroids.length; i++) {
            if (asteroids[i].id == id) {
                return i;
            }
        }
        return -1;
    }
}
