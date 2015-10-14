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


	public double timeToPush(long time, Asteroid a, Asteroid b) {
		// Return the time after current when a can be pushed to b with Hohmann Transfer
		double ra = a.orbit.a;
		double rb = b.orbit.a;

		double angle = Math.PI * (1 - Math.pow(1 + ra / rb, 1.5) / Math.sqrt(8));

		double aa = a.orbit.positionAt(time - a.epoch).direction();
		double ba = b.orbit.positionAt(time - b.epoch).direction();

		double angle_now = ba - aa;
		if (angle_now < 0) angle_now += Math.PI * 2;

		double alphaa = a.orbit.velocityAt(time - a.epoch).magnitude() / ra;
		double alphab = a.orbit.velocityAt(time - b.epoch).magnitude() / rb;

		if (angle_now >= angle) {
			if (alphaa >= alphab) {
				return (angle_now - angle) / (alphaa - alphab);
			} else {
				return (Math.PI * 2 - angle_now + angle) / (alphab - alphaa);
			}
		} else {
			if (alphab >= alphaa) {
				return (angle - angle_now) / (alphaa - alphab);
			} else {
				return (Math.PI * 2 - angle + angle_now) / (alphab - alphaa);
			}
		}

	}



}