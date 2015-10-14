package pb.g2;

import pb.sim.Point;
import pb.sim.Orbit;
import pb.sim.Asteroid;

public class Hohmann {

	/**
	 * Generate a Hohmann transfer push of asteroid a1 into a2.
	 * Assumes both are currently in circular orbits.
	 */
	public static Push generatePush(Asteroid a1, Asteroid a2, long time) {
		double r1 = a1.orbit.a;
		double r2 = a2.orbit.a;
		double dv = Math.sqrt(pb.sim.Orbit.GM / r1) * (Math.sqrt(2 * r2 / (r1 + r2)) - 1);
		long expected_collision_time = (long) Math.ceil(Math.PI * Math.sqrt(Math.pow(r1 + r2, 3) / (8 * Orbit.GM)) / Orbit.dt());
		double energy = a1.mass * Math.pow(dv, 2) / 2;
		double direction = a1.orbit.velocityAt(time - a1.epoch).direction();
		if (dv < 0)
			direction += Math.PI;
		return new Push(a1.id, energy, direction, expected_collision_time);
	}

	/**
	 * Generate a Hohmann transfer push to correct an elliptical orbit
	 * to a circular one with radius = semi-major axis length of ellipse.
	 */
	public static Push generateCorrection(Asteroid asteroid, long time) {
		Point p = asteroid.orbit.positionAt(time - asteroid.epoch);

		Point v1 = new Orbit(p).velocityAt(0); // Velocity for round

		Point v = asteroid.orbit.velocityAt(time - asteroid.epoch);
		Point dv = new Point(v1.x - v.x, v1.y - v.y);

		double energy = asteroid.mass * Math.pow(dv.magnitude(), 2) / 2;
		double direction = dv.direction();
		return new Push(asteroid.id, energy, direction, -1);
	}
}