package pb.g2;

import pb.sim.Point;
import pb.sim.Orbit;
import pb.sim.Asteroid;

import java.util.HashSet;
import java.util.Random;

public class Player implements pb.sim.Player {

	// used to pick asteroid and velocity boost randomly
	private Random random = new Random();

	// current time, time limit
	private long time = -1;
	private long time_limit = -1;

	private int number_of_asteroids;
	private boolean after_collision;

	private long next_push = 0;

	private HashSet<Asteroid> online;
	private HashSet<Asteroid> exist;

	// print orbital information
	public void init(Asteroid[] asteroids, long time_limit)
	{
		if (Orbit.dt() != 24 * 60 * 60)
			throw new IllegalStateException("Time quantum is not a day");
		this.time_limit = time_limit;
		this.number_of_asteroids = asteroids.length;
		this.online = new HashSet<>();
		this.exist = new HashSet<>();
		for (Asteroid a: asteroids) {
			this.online.add(a);
			this.exist.add(a);
		}
		this.after_collision = false;
	}

	private long checkCollision(Asteroid a1, Asteroid a2, long max_time) {
		// avoid allocating a new Point object for every position
		Point p1 = new Point(), p2 = new Point();
		// search for collision with other asteroids
		double r = a1.radius() + a2.radius();
		for (long ft = max_time - 10 ; ft != max_time + 10 ; ++ft) { // HACK
			long t = time + ft;
			if (t >= time_limit) break;
			a1.orbit.positionAt(t - a1.epoch, p1);
			a2.orbit.positionAt(t - a2.epoch, p2);
			// if collision, return push to the simulator
			if (Point.distance(p1, p2) < r) {
				return t;
			}
		}
		return -1; // No collision within deadline
	}

	// try to push asteroid
	public void play(Asteroid[] asteroids,
	                 double[] energy, double[] direction)
	{
		time++;

		if (asteroids.length < number_of_asteroids) {
			System.out.println("A collision just occurred at time " + time);
			// Check for non-circular orbit
			for (int i = 0; i < asteroids.length; i++) {
				if (Math.abs(asteroids[i].orbit.a - asteroids[i].orbit.b) > 10e-6 &&
						!exist.contains(asteroids[i])) {
					// Correct for non-circular orbit
					Point p = asteroids[i].orbit.positionAt(time - asteroids[i].epoch);

					Point v1 = new Orbit(p).velocityAt(0); // Velocity for round

					Point v = asteroids[i].orbit.velocityAt(time - asteroids[i].epoch);
					Point dv = new Point(v1.x - v.x, v1.y - v.y);

					System.out.println("v1: " + v1);
					System.out.println("v: " + v);
					System.out.println("dv: " + dv);

					energy[i] = asteroids[i].mass * Math.pow(dv.magnitude(), 2) / 2;
					direction[i] = dv.direction();
				}
			}
			after_collision = true;
			number_of_asteroids = asteroids.length;
			return;
		}

		for (Asteroid a: asteroids) {
			if (after_collision && !exist.contains(a)) {
				this.online.add(a);
			}
			this.exist.add(a);
		}

		after_collision = false;

		// Get largest
		int largest = -1; double radius = 0;
		for (int i = 0; i < asteroids.length; i++) {
			if (online.contains(asteroids[i]) && asteroids[i].radius() > radius) {
				largest = i;
				radius = asteroids[i].radius();
			}
		}

		for (int i = 0; i < asteroids.length; i++) {
			if (largest == i || !online.contains(asteroids[i])) {
				continue;
			}
			// Try to push into largest
			Point v1 = asteroids[i].orbit.velocityAt(time - asteroids[i].epoch);

			double r1 = asteroids[i].orbit.a; // Assume circular
			double r2 = asteroids[largest].orbit.a; // Assume circular

			// Transfer i to j orbit
			double dv = Math.sqrt(pb.sim.Orbit.GM / r1) * (Math.sqrt(2 * r2 / (r1 + r2)) - 1);
			double t = Math.PI * Math.sqrt(Math.pow(r1 + r2, 3) / (8 * Orbit.GM)) / Orbit.dt();
			double e = asteroids[i].mass * Math.pow(dv, 2) / 2;
			double d = v1.direction();
			if (dv < 0) d += Math.PI;

			Asteroid a1 = Asteroid.push(asteroids[i], time, e, d);
			long nt = checkCollision(a1, asteroids[largest], (long)Math.ceil(t));
			if (nt != -1) {
				energy[i] = e; direction[i] = d;
				// next_push = nt;
				online.remove(asteroids[i]);
				online.remove(asteroids[largest]);
				return;
			}
		}


	}
}
