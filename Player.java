package pb.g2;

import pb.g7.asteroid_index;
import pb.sim.Point;
import pb.sim.Orbit;
import pb.sim.Asteroid;
import pb.sim.InvalidOrbitException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Random;

public class Player implements pb.sim.Player {

	// used to pick asteroid and velocity boost randomly
	private Random random = new Random();

	// current time, time limit
	private long time = -1;
	private long time_limit = -1;

	private int number_of_asteroids;

	private long next_push = 0;

	// print orbital information
	public void init(Asteroid[] asteroids, long time_limit)
	{
		if (Orbit.dt() != 24 * 60 * 60)
			throw new IllegalStateException("Time quantum is not a day");
		this.time_limit = time_limit;
		this.number_of_asteroids = asteroids.length;
	}

	// try to push asteroid
	public void play(Asteroid[] asteroids,
	                 double[] energy, double[] direction)
	{
		time++;
		int n = asteroids.length;

		if (asteroids.length < number_of_asteroids) {
			System.out.println("A collision just occurred at time " + time);
			// Check for non-circular orbit
			for (int i = 0; i < asteroids.length; i++) {
				if (Math.abs(asteroids[i].orbit.a - asteroids[i].orbit.b) > 10e-6) {
					// // Correct for non-circular orbit
					Push push = Hohmann.generateCorrection(asteroids[i], time);
					energy[i] = push.energy;
					direction[i] = push.direction;
				}
			}

			next_push = 0; // Void
			number_of_asteroids = asteroids.length;
			return;
		}

		if (time <= next_push) return;

		// Get largest radius asteroid
		int largestMass = Utils.largestMass(asteroids);

		// Pick asteroid to push to

		// Sort asteroids in order of how attractive they are to become nucleus
		ArrayList<ComparableAsteroid> sorted_asteroids = new ArrayList<ComparableAsteroid>();
		Point asteroid_position = new Point();
		Point sun = new Point(0, 0);
		for (int i = 0; i<n; i++) {
			asteroids[i].orbit.positionAt(time - asteroids[i].epoch, asteroid_position);
			sorted_asteroids.add(new ComparableAsteroid(i, Point.distance(sun, asteroid_position), asteroids[i].mass));
		}
		Collections.sort(sorted_asteroids);



		for (int i = 0; i < asteroids.length; i++) {
			if (i == largestMass)
				continue;
			// /*
			// Hashtable<Long, ArrayList<CollisionChecker.CollisionPair>> collisions =
			// 		CollisionChecker.checkCollision(asteroids, (long) Math.ceil(t), time, time_limit);
			// */

			Push push = Hohmann.generatePush(asteroids[i], asteroids[largestMass], time);
			Asteroid a1 = Asteroid.push(asteroids[i], time, push.energy, push.direction);

			long nt = CollisionChecker.checkCollision(a1, asteroids[largestMass], push.expected_collision_time, time, time_limit);
			if (nt != -1) {
				energy[i] = push.energy;
				direction[i] = push.direction;
				next_push = nt;
				return;
			}
		}

		for (int i = 0; i < asteroids.length; i++) {
			if (i == largestMass)
				continue;
			for (int j = 0; j < asteroids.length; j++) {
				if (j == largestMass)
					continue;

				Push push = Hohmann.generatePush(asteroids[i], asteroids[j], time);
				Asteroid a1 = Asteroid.push(asteroids[i], time, push.energy, push.direction);

				long nt = CollisionChecker.checkCollision(a1, asteroids[j], push.expected_collision_time, time, time_limit);
				if (nt != -1) {
					energy[i] = push.energy;
					direction[i] = push.direction;
					next_push = nt;
					return;
				}
			}
		}


	}
}
