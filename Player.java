package pb.g2;

import pb.sim.Point;
import pb.sim.Orbit;
import pb.sim.Asteroid;
import pb.sim.InvalidOrbitException;

import java.util.Random;

public class Player implements pb.sim.Player {

	// used to pick asteroid and velocity boost randomly
	private Random random = new Random();

	// current time, time limit
	private long time = -1;
	private long time_limit = -1;

	// next push
	private int second_push_id = -1; // Transfer
	private double second_push_time;
	private double second_push_v;
	private boolean third_push = false;

	private boolean temp_stop = false;

	// number of retries
	private int retries_per_turn = 1;
	private int turns_per_retry = 3;

	private int number_of_asteroids;

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

		if (temp_stop) return;

		if (third_push) {

			System.out.println("Third push");
			Asteroid a = asteroids[second_push_id];
			Point v = a.orbit.velocityAt(time);

			double dv = 2 * Math.sqrt(v.x * v.x + v.y * v.y);
			energy[second_push_id] = a.mass * Math.pow(dv, 2) / 2;
			direction[second_push_id] = Math.atan2(v.y, v.x) + Math.PI;

			third_push = false;
			second_push_id = -1;

			temp_stop = true;

			return;
		}

		if (second_push_id > -1) {

			if (second_push_time > time) return;

			System.out.println(time);

			Asteroid a = asteroids[second_push_id];
			Point v = a.orbit.velocityAt(time);

			energy[second_push_id] = a.mass * Math.pow(second_push_v, 2) / 2;
			direction[second_push_id] = Math.atan2(v.y, v.x);

			if (second_push_v < 0) direction[second_push_id] += Math.PI;

			// end push
			third_push = true;
			return;

		}

		for (int retry = 1 ; retry <= retries_per_turn ; ++retry) {
			// pick Asteroid 0
			for (int i = 0; i < asteroids.length; i++) {

				Point v1 = asteroids[i].orbit.velocityAt(time);

				int j = 1;
				/*
				for (j = i; j < asteroids.length; j++) {
					// pick a possible Asteroid to push to
					if (Math.abs(asteroids[j].mass - asteroids[i].mass) > asteroids[i].mass * 0.1) {
						// Mass differ by enough
						break;
					}
				}
				if (j == 0) continue;
				*/

				double r1 = asteroids[i].orbit.a; // Assume circular
				double r2 = asteroids[j].orbit.a; // Assume circular

				// Transfer i to j orbit
				double dv = Math.sqrt(pb.sim.Orbit.GM / r1) * (Math.sqrt(2 * r2 / (r1 + r2)) - 1);

				energy[i] = asteroids[i].mass * Math.pow(dv, 2) / 2;
				direction[i] = Math.atan2(v1.y, v1.x);
				if (dv < 0) direction[i] = direction[i] + Math.PI;

				second_push_v = Math.sqrt(pb.sim.Orbit.GM / r2) * (1 - Math.sqrt(2 * r1 / (r1 + r2)));
				second_push_id = i;
				second_push_time = time + Math.PI * Math.sqrt(Math.pow(r1 + r2, 3) / (8 * Orbit.GM)) / Orbit.dt();

				System.out.println("Target time: " + second_push_time);
				return;
			}

		}
	}
}
