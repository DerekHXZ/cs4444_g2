package pb.g2;

import pb.sim.Point;
import pb.sim.Orbit;
import pb.sim.Asteroid;
import pb.sim.InvalidOrbitException;

import java.util.ArrayList;
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

		if (asteroids.length < number_of_asteroids) {
			System.out.println("A collision just occurred at time " + time);
			// Check for non-circular orbit
			for (int i = 0; i < asteroids.length; i++) {
				if (Math.abs(asteroids[i].orbit.a - asteroids[i].orbit.b) > 10e-6) {
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

			next_push = 0; // Void
			number_of_asteroids = asteroids.length;
			return;
		}

		if (time <= next_push) return;

        // Get ply results
        PlyResult result = simluatePly(asteroids, 2, 0);

        int i = result.asteroid1;
        int j = result.asteroid2;
        long time_of_collision = result.time_of_collision;

        // Try to push i into j
        Point v1 = asteroids[i].orbit.velocityAt(time - asteroids[i].epoch);

        // pick Asteroid 1
        double r1 = asteroids[i].orbit.a; // Assume circular
        double r2 = asteroids[j].orbit.a; // Assume circular

        // Transfer i to j orbit
        double push_velocity = Math.sqrt(pb.sim.Orbit.GM / r1) * (Math.sqrt(2 * r2 / (r1 + r2)) - 1);
        double new_energy = asteroids[i].mass * Math.pow(push_velocity, 2) / 2;
        double new_direction = v1.direction();

        if (push_velocity < 0) new_direction += Math.PI;

        if (time_of_collision != -1) {
            energy[i] = new_energy;
            direction[i] = new_direction;
            next_push = time_of_collision;
            return;
        }
	}

    private class PlyResult {
        double energy;
        int asteroid1, asteroid2;
        long time_of_collision;

        public PlyResult(double energy, int asteroid1, int asteroid2, long time_of_collision) {
            this.energy = energy;
            this.asteroid1 = asteroid1;
            this.asteroid2 = asteroid2;
            this.time_of_collision = time_of_collision;
        }
    }

    public PlyResult simluatePly(Asteroid[] asteroids, int plies_left, double energy_used_so_far) {
        if (plies_left <= 0) {
            return new PlyResult(energy_used_so_far, -1, -1, -1);
        }

        int n = asteroids.length;

        // Store energies of all possible look aheads
        double[][] ply_energies = new double[n][n];
        double lowest_energy = Long.MAX_VALUE;
        int asteroid1 = -1, asteroid2 = -1;
        long time_of_lowest_energy_collision = -1;

        for (int i = 0; i < n; i++) {
            for (int j = i+1; j < n; j++) {
                // Try to push i into j
                Point v1 = asteroids[i].orbit.velocityAt(time - asteroids[i].epoch);

                // pick Asteroid 1
                double r1 = asteroids[i].orbit.a; // Assume circular
                double r2 = asteroids[j].orbit.a; // Assume circular

                // Transfer i to j orbit
                double push_velocity = Math.sqrt(pb.sim.Orbit.GM / r1) * (Math.sqrt(2 * r2 / (r1 + r2)) - 1);
                double expected_collision_time = Math.PI * Math.sqrt(Math.pow(r1 + r2, 3) / (8 * Orbit.GM)) / Orbit.dt();
                double energy = asteroids[i].mass * Math.pow(push_velocity, 2) / 2;
                double direction = v1.direction();
                if (push_velocity < 0) direction += Math.PI;

                Asteroid asteroid_after_push = Asteroid.push(asteroids[i], time, energy, direction);

                long time_of_collision = CollisionChecker.checkCollision(asteroid_after_push, asteroids[j],
                        (long) Math.ceil(expected_collision_time + 10), time, time_limit); // TODO: Why +10?

                if (time_of_collision == -1) {
                    ply_energies[i][j] = Long.MAX_VALUE;
                } else {
                    ply_energies[i][j] = simluatePly(asteroids, plies_left - 1, energy_used_so_far + energy).energy;
                }

                // Set lowest energy collision option so far
                if (ply_energies[i][j] < lowest_energy) {
                    lowest_energy = ply_energies[i][j];
                    asteroid1 = i;
                    asteroid2 = j;
                    time_of_lowest_energy_collision = time_of_collision;
                }
            }
        }

        return new PlyResult(lowest_energy, asteroid1, asteroid2, time_of_lowest_energy_collision);
    }
}
