package pb.g2;

import pb.sim.Point;
import pb.sim.Orbit;
import pb.sim.Asteroid;

import java.util.*;

public class Player implements pb.sim.Player {

    // current time, time limit
    private long time = -1;
    private long time_limit = -1;

    private int number_of_asteroids;

    private long next_push = -1;
    private Push push_info = null;

    private long period;

    private static double EPSILON = 10e-6;
    private long nucleus_id;
    private double total_mass = 0.0;

    private boolean finish_flag = false;

    private HashSet<Long> seenId;

    private Queue<Push> pushes = new LinkedList<Push>();

    // print orbital information
    public void init(Asteroid[] asteroids, long time_limit) {
        if (Orbit.dt() != 24 * 60 * 60)
            throw new IllegalStateException("Time quantum is not a day");
        this.time_limit = time_limit;
        this.number_of_asteroids = asteroids.length;
        this.period = time_limit / this.number_of_asteroids;

        // Sort asteroids in order of how attractive they are to become nucleus
        int n = asteroids.length;
        ArrayList<ComparableAsteroid> sorted_asteroids = new ArrayList<ComparableAsteroid>();

        // Compute mean and stddev of asteroid masses
        double mean = Utils.mean(asteroids);
        double stddev = Utils.stddev(asteroids, mean);

        Point asteroid_position = new Point();
        Point sun = new Point(0, 0);
        for (int i = 0; i < n; i++) {
            asteroids[i].orbit.positionAt(time - asteroids[i].epoch, asteroid_position);
            total_mass += asteroids[i].mass;
            sorted_asteroids.add(new ComparableAsteroid(asteroids[i], i, Point.distance(sun, asteroid_position), asteroids[i].mass,
                    asteroids[i].orbit.velocityAt(0).magnitude(), mean, stddev, asteroids));
        }
        Collections.sort(sorted_asteroids);

        // Get nucleus asteroid to which we will push all other asteroids
        int nucleus_index = sorted_asteroids.get(n - 1).index;
        nucleus_id = asteroids[nucleus_index].id;
        // System.out.println("Found nucleus id " + nucleus_id + ", mass " + asteroids[nucleus_id].mass);

        // Asteroids seen before
        seenId = new HashSet<Long>();
        for (Asteroid a : asteroids) {
            seenId.add(a.id);
        }
    }

    // try to push asteroid
    public void play(Asteroid[] asteroids,
                     double[] energy, double[] direction) {
        time++;

        int n = asteroids.length;

        if (asteroids.length < number_of_asteroids) {
            System.out.println("A collision just occurred at time " + time);
            // Check for non-circular orbit
            int new_asteroid_idx = 0;
            for (int i = 0; i < asteroids.length; i++) {
                if (Math.abs(asteroids[i].orbit.a - asteroids[i].orbit.b) > EPSILON) {
                    // Correct for non-circular orbit
                    Push push = Hohmann.generateCorrection(asteroids[i], i, time);
                    energy[i] = push.energy;
                    direction[i] = push.direction;
                }
                if (!seenId.contains(asteroids[i].id)) {
                    new_asteroid_idx = i;
                    seenId.add(asteroids[i].id);
                }
            }

            if (Utils.findAsteroidById(asteroids, nucleus_id) == null) {
                nucleus_id = asteroids[new_asteroid_idx].id;
                System.out.println("NUCLEUS ID CHANGED");
            }

            next_push = -1; // Void
            push_info = null;
            number_of_asteroids = asteroids.length;
            return;
        }

        Asteroid nucleus = Utils.findAsteroidById(asteroids, nucleus_id);

        if (time < next_push) return;
        if (time == next_push && push_info != null) {
            // Push
            int index;
            for (index = 0; index < asteroids.length; index++) {
                if (asteroids[index].id == push_info.asteroid.id) {
                    break;
                }
            }
            if (index != asteroids.length) {

                Asteroid a = Asteroid.push(asteroids[index], time, push_info.energy, push_info.direction);
                long collision_time =
                        CollisionChecker.checkCollision(a, nucleus, push_info.expected_collision_time, time, time_limit);
                if (collision_time != -1) {
                    energy[index] = push_info.energy;
                    direction[index] = push_info.direction;
                    next_push = collision_time + 1;
                    System.out.println("This is " + time + ". Collision will happen at " + next_push);

                    push_info = null;
                    return;
                } else {
                    System.out.println("WTF");
                }

            }
            System.out.println("WTF");
        }

        // Of all remaining asteroids, find the one with lowest energy push
        ArrayList<Push> pushes = new ArrayList<Push>();
        for (int i = 0; i < n; i++) {
            if (asteroids[i].id == nucleus_id) {
                continue;
            }
            int curr_asteroid_index = i;
            Asteroid curr_asteroid = asteroids[curr_asteroid_index];

            // Ignore asteroids with elliptical orbits
            if (Math.abs(curr_asteroid.orbit.a - curr_asteroid.orbit.b) > EPSILON) {
                continue;
            }

            long time_to_push = Hohmann.timeToPush(time, curr_asteroid, nucleus);
            if (time_to_push != -1) {
                Push push = Hohmann.generatePush(curr_asteroid, curr_asteroid_index, nucleus, time_to_push);
                if (time_to_push + push.expected_collision_time <= time_limit) {
                    pushes.add(push);
                }
            }
        }
        Collections.sort(pushes);
        if (!pushes.isEmpty()) {
            push_info = pushes.get(0);
            next_push = push_info.time;
            System.out.println("This is " + time + ". Push will happen at " + next_push);
            return;
        }

        /*
        if (time > 0.9 * time_limit) {
            finish_flag = true;
            finishGame(asteroids, nucleus, energy, direction);
        }
        */
    }


    /**
     * Worst case: If we could not collide anything into the nucleus,
     * find the biggest masses and try to collide them
     */
    public void finishGame(Asteroid[] asteroids, Asteroid nucleus, double[] energy, double[] direction) {
        int n = asteroids.length;
        ArrayList<Asteroid> largest_asteroids = new ArrayList<Asteroid>();

        for (int i = 0; i < n; i++) {
            largest_asteroids.add(asteroids[i]);
        }

        // Sort asteroids in decreasing order by mass
        Collections.sort(largest_asteroids,new Comparator<Asteroid>() {
            public int compare(Asteroid a1, Asteroid a2) {
                return -1*(Double.compare(a1.mass, a2.mass));
            }
        });

        double mass = nucleus.mass;
        ArrayList<Asteroid> asteroids_to_collide = new ArrayList<Asteroid>();
        for (int i = 0; mass < 0.5*total_mass; i++) {
            Asteroid large_asteroid = largest_asteroids.get(i);
            if (large_asteroid.id == nucleus_id) continue;
            asteroids_to_collide.add(large_asteroid);
            mass += large_asteroid.mass;
        }

        for (int i = 0; i < asteroids_to_collide.size(); i++) {
            Asteroid curr_asteroid = asteroids_to_collide.get(i);
            long time_to_push = Hohmann.timeToPush(time, curr_asteroid, nucleus);
            if (time_to_push != -1) {
                Push push = Hohmann.generatePush(curr_asteroid, i, nucleus, time_to_push);
                if (time_to_push + push.expected_collision_time < time_limit) {
                    pushes.add(push);
                }
            }
        }
    }
}
