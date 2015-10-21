package pb.g2;

import pb.sim.Point;
import pb.sim.Orbit;
import pb.sim.Asteroid;

public class Hohmann {

	/**
	 * Generate a Hohmann Transfer push of asteroid a1 into a2.
	 * Assumes both are currently in circular orbits.
	 */
	public static Push generatePush(Asteroid a1, int asteroid_to_push_index, Asteroid a2, long time) {
		double r1 = a1.orbit.a;
		double r2 = a2.orbit.a;
		double dv = Math.sqrt(pb.sim.Orbit.GM / r1) * (Math.sqrt(2 * r2 / (r1 + r2)) - 1);
		long expected_collision_time = (long) Math.ceil(Math.PI * Math.sqrt(Math.pow(r1 + r2, 3) / (8 * Orbit.GM)) / Orbit.dt());
		double energy = a1.mass * Math.pow(dv, 2) / 2;
		double direction = a1.orbit.velocityAt(time - a1.epoch).direction();
		if (dv < 0)
			direction += Math.PI;
        //System.out.println("expected collision time: " + expected_collision_time);
		return new Push(a1, asteroid_to_push_index, energy, direction, time, expected_collision_time);
	}

	/**
	 * Generate a Hohmann Transfer push to correct an elliptical orbit
	 * to a circular one with radius = semi-major axis length of ellipse.
	 */
	public static Push generateCorrection(Asteroid asteroid, int asteroid_to_push_index, long time) {
		Point p = asteroid.orbit.positionAt(time - asteroid.epoch);

		Point v1 = new Orbit(p).velocityAt(0); // Velocity for round

		Point v = asteroid.orbit.velocityAt(time - asteroid.epoch);
		Point dv = new Point(v1.x - v.x, v1.y - v.y);

		double energy = asteroid.mass * Math.pow(dv.magnitude(), 2) / 2;
		double direction = dv.direction();
		return new Push(asteroid, asteroid_to_push_index, energy, direction, -1, -1);
	}

	/**
	 * Return the time after current time when asteroid a can be pushed to b with Hohmann Transfer.
 	 */
	public static long timeToPush(long time, Asteroid a, Asteroid b) {
		double ra = a.orbit.a;
		double rb = b.orbit.a;

		double angle = Math.PI * (1 - Math.pow(1 + ra / rb, 1.5) / Math.sqrt(8)) % ( 2 * Math.PI );
		if (angle < 0) angle += 2 * Math.PI;

		double aa = a.orbit.positionAt(time - a.epoch).direction();
		double ba = b.orbit.positionAt(time - b.epoch).direction();

		double angle_now = ba - aa;
		if (angle_now < 0) angle_now += Math.PI * 2;

		double alphaa = a.orbit.velocityAt(time - a.epoch).magnitude() / ra;
		double alphab = b.orbit.velocityAt(time - b.epoch).magnitude() / rb;

		double raw_time;

		if (Math.abs(alphaa - alphab) < 1e-15) {
			System.out.println("Too close");
			return -1; // Cannot transfer
		}

		raw_time = (angle_now - angle) / (alphaa - alphab);
		if (raw_time < 0) {
			raw_time = (angle_now - angle + 2 * Math.PI) / (alphaa - alphab);
			if (raw_time < 0) {
				raw_time = (angle_now - angle - 2 * Math.PI) / (alphaa - alphab);
			}
		}

		if (raw_time == Double.NaN || raw_time > Long.MAX_VALUE) {
			// Overflow
			System.out.println("Time wrong");
			return -1;
		}
		// Check actual time to push
		long wait_time = (long)(raw_time / Orbit.dt());

		for (long push_time = time + wait_time - 10; push_time <= time + wait_time + 10; push_time++) {
			if (push_time < time+1) continue;
			Push p = generatePush(a, 0, b, push_time);
			Asteroid pushed_asteroid = Asteroid.push(p.asteroid, push_time, p.energy, p.direction);
			if (CollisionChecker.checkCollision(pushed_asteroid, b, p.expected_collision_time, push_time, -1) != -1) {
				return push_time;
			}
		}
		System.out.println("Unknown error");
		return -1;
	}
}