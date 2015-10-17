package pb.g2;

import pb.sim.Point;
import pb.sim.Orbit;
import pb.sim.Asteroid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Player implements pb.sim.Player {

	// current time, time limit
	private long time = -1;
	private long time_limit = -1;

	private int number_of_asteroids;

	private long next_push = 0;

	private long period;

    private static double EPSILON = 10e-6;
    private Asteroid nucleus;
    private int nucleus_index;

	// print orbital information
	public void init(Asteroid[] asteroids, long time_limit)
	{
		if (Orbit.dt() != 24 * 60 * 60)
			throw new IllegalStateException("Time quantum is not a day");
		this.time_limit = time_limit;
		this.number_of_asteroids = asteroids.length;
		this.period = time_limit / this.number_of_asteroids;

        // Pick asteroid to push to
        // Sort asteroids in order of how attractive they are to become nucleus
        int n = asteroids.length;
        ArrayList<ComparableAsteroid> sorted_asteroids = new ArrayList<ComparableAsteroid>();
        Point asteroid_position = new Point();
        Point sun = new Point(0, 0);
        for (int i = 0; i < n; i++) {
            asteroids[i].orbit.positionAt(time - asteroids[i].epoch, asteroid_position);
            sorted_asteroids.add(new ComparableAsteroid(i, Point.distance(sun, asteroid_position), asteroids[i].mass));
        }
        Collections.sort(sorted_asteroids);

        // Get nucleus asteroid to which we will push all other asteroids
        nucleus_index = sorted_asteroids.get(n - 1).index;
        // System.out.println("Found nucleus id " + nucleus_index + ", mass " + asteroids[nucleus_index].mass);
        nucleus = asteroids[nucleus_index];
	}

	// try to push asteroid
	public void play(Asteroid[] asteroids,
	                 double[] energy, double[] direction) {
        time++;

        int n = asteroids.length;

        if (asteroids.length < number_of_asteroids) {
            System.out.println("A collision just occurred at time " + time);
            // Check for non-circular orbit
            for (int i = 0; i < asteroids.length; i++) {
                if (Math.abs(asteroids[i].orbit.a - asteroids[i].orbit.b) > EPSILON) {
                    // Correct for non-circular orbit
                    Push push = Hohmann.generateCorrection(asteroids[i], i, time);
                    energy[i] = push.energy;
                    direction[i] = push.direction;
                }
            }

            next_push = 0; // Void
            number_of_asteroids = asteroids.length;
            return;
        }

        if (time <= next_push) return;

        // Of all remaining asteroids, find the one with lowest energy push
        long SEARCH_TIME = time_limit - time;
        Push min_push = null;
        long min_push_time_of_collision = -1;
        for (int i = 0; i < n; i++) {
            if (i == nucleus_index) {
                continue;
            }
            int curr_asteroid_index = i;
            Asteroid curr_asteroid = asteroids[curr_asteroid_index];

            // Ignore asteroids with elliptical orbits
            if (Math.abs(curr_asteroid.orbit.a - curr_asteroid.orbit.b) > EPSILON) {
                continue;
            }

            //long time_to_push = (long) Hohmann.timeToPush(time, curr_asteroid, nucleus); // TODO: Get rid of long conversion
            Push push = Hohmann.generatePush(curr_asteroid, curr_asteroid_index, nucleus, time);
            if (push.expected_collision_time < SEARCH_TIME) {
                Asteroid pushed_asteroid = Asteroid.push(push.asteroid, time, push.energy, push.direction);

                long time_of_collision = CollisionChecker.checkCollision(
                        pushed_asteroid, nucleus, push.expected_collision_time, time, time_limit);
                if (time_of_collision == -1) {
                    continue;
                }
                if (min_push == null || push.energy < min_push.energy) {
                    min_push = push;
                    min_push_time_of_collision = time_of_collision;
                }
            }
        }
        if (min_push != null) {
            System.out.println("Found a push with id " + min_push.asteroid.id);
            energy[min_push.index] = min_push.energy;
            direction[min_push.index] = min_push.direction;
            next_push = min_push_time_of_collision;
            return;
        }

        if (time > 0.9*time_limit) {
            // ¯\_(ツ)_/¯
            giveUpAndFinish(nucleus_index, asteroids, energy, direction);
        }
    }


    /**
     * Worst case: If we could not collide anything into the nucleus,
     * find the biggest masses and try to collide them
     */
    public void giveUpAndFinish(int nucleus_index, Asteroid[] asteroids, double[] energy, double[] direction) {
        int n = asteroids.length;

        for (int i = 0; i < n; i++) {
            if (i == nucleus_index) {
                continue;
            }
            for (int j = 0; j < n; j++) {
                if (j == nucleus_index) {
                    continue;
                }

                Push push = Hohmann.generatePush(asteroids[i], i, asteroids[j], time);
                Asteroid pushed_asteroid = Asteroid.push(asteroids[i], time, push.energy, push.direction);

                long time_of_collision = CollisionChecker.checkCollision(pushed_asteroid, asteroids[j], push.expected_collision_time,
                        time, time_limit);
                if (time_of_collision != -1) {
                    System.out.println("Found a collision in give up");
                    energy[i] = push.energy;
                    direction[i] = push.direction;
                    next_push = time_of_collision;
                    return;
                }
            }
        }
    }
}
