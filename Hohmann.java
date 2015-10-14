package pb.g2;

import pb.sim.Point;
import pb.sim.Orbit;
import pb.sim.Asteroid;
import pb.sim.InvalidOrbitException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

public class Hohmann {

	public static double[] genPush(Asteroid a1, Asteroid a2, long time) {
		double r1 = a1.orbit.a; // Assume circular
		double r2 = a2.orbit.a; // Assume circular
		double dv = Math.sqrt(pb.sim.Orbit.GM / r1) * (Math.sqrt(2 * r2 / (r1 + r2)) - 1);
		double t = Math.PI * Math.sqrt(Math.pow(r1 + r2, 3) / (8 * Orbit.GM)) / Orbit.dt();
		double e = a1.mass * Math.pow(dv, 2) / 2;
		double d = a1.orbit.velocityAt(time - a1.epoch).direction();
		if (dv < 0) 
			d += Math.PI;
		return new double[] {e, d, t};
	}

	public static double[] genCorrection(Asteroid asteroid, long time) {
		Point p = asteroid.orbit.positionAt(time - asteroid.epoch);

		Point v1 = new Orbit(p).velocityAt(0); // Velocity for round

		Point v = asteroid.orbit.velocityAt(time - asteroid.epoch);
		Point dv = new Point(v1.x - v.x, v1.y - v.y);

		double e = asteroid.mass * Math.pow(dv.magnitude(), 2) / 2;
		double d = dv.direction();
		return new double[] {e, d};
	}



}