package pb.g2;

import pb.sim.Point;
import pb.sim.Orbit;
import pb.sim.Asteroid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class Player implements pb.sim.Player {

	// current time, time limit
	private long time = -1;
	private long time_limit = -1;

	private int number_of_asteroids;

	private long next_push = -1;
    private Push push_info = null;

	private long period;
    private boolean progress;

    private static double EPSILON = 10e-6;
    private long nucleus_id;
    private double total_mass = 0.0;

    private HashSet<Long> seenId;

    private ArrayList<Long> troublemakers = new ArrayList<Long>();

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


        progress = false;

        seenId = new HashSet<>();
        for (Asteroid a : asteroids) {
            seenId.add(a.id);
        }
	}

	// try to push asteroid
	public void play(Asteroid[] asteroids,
	                 double[] energy, double[] direction) {
        time++;

        if (time % this.period == 0)
            progress = false;

        /* fix orbits of troublemaker asteroids that intially shared the same orbit with nucleus */
        if (!troublemakers.isEmpty()) {
            ArrayList<Long> temp = new ArrayList<Long>();
            for (long id : troublemakers) {
                int t = Utils.findAsteroidIndexById(asteroids, id);
                Point p1 = asteroids[t].orbit.positionAt(time);
                Point p2 = asteroids[t].orbit.positionAt(time+1);
                if (Math.hypot(p1.x, p1.y) > Math.hypot(p2.x, p2.y)) {
                    Push push = Hohmann.generateCorrection(asteroids[t], t, time);
                    energy[t] = push.energy;
                    direction[t] = push.direction;
                } else {
                    temp.add(id);
                }
            }
            troublemakers = temp;
        }

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

        /* push out asteroids that share the same orbit with nucleus */
        for (int i = 0; i < asteroids.length; i++) {
            Asteroid ast = asteroids[i];
            if (nucleus == ast) {
                continue;
            }
            if (Math.abs(ast.orbit.a - nucleus.orbit.a) <= EPSILON
                && Math.abs(ast.orbit.b - nucleus.orbit.b) <= EPSILON
                ) {
                troublemakers.add(ast.id);
                Push push = Hohmann.generatePushToRadius(ast, i, ast.orbit.a*1.05, time);
                energy[i] = push.energy;
                direction[i] = push.direction;
            }
        }

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
                    next_push = collision_time+1;
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
        ArrayList<Push> pushes = new ArrayList<>();
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
        if (time > 0.9*time_limit || !progress) {
            // ¯\_(ツ)_/¯
            giveUpAndFinish(asteroids, energy, direction);
        }
        */
    }


    /**
     * Worst case: If we could not collide anything into the nucleus,
     * find the biggest masses and try to collide them
     */
    /*
    public void giveUpAndFinish(Asteroid[] asteroids, double[] energy, double[] direction) {
        int n = asteroids.length;
        ArrayList<Long> largest_asteroids = new ArrayList<>();
        largest_asteroids.add(nucleus_id);
        double mass = asteroids[nucleus_id].mass;
        while (mass / total_mass < 0.5) {
            int largest = 0;
            double largest_m = 0;
            for (int i = 0; i < n; i++) {
                if (!largest_asteroids.contains(i) && asteroids[i].mass > largest_m) {
                    largest = i;
                    largest_m = asteroids[i].mass;
                }
            }
            largest_asteroids.add(largest);
            mass += largest_m;
        }

        for (int a=0; a<largest_asteroids.size(); a++) {
            int i = a;
            if (i == nucleus_id)
                continue;
            Push push = Hohmann.generatePush(asteroids[i], i, asteroids[nucleus_id], time);
            Asteroid pushed_asteroid = Asteroid.push(asteroids[i], time, push.energy, push.direction);
            long time_of_collision = CollisionChecker.checkCollision(pushed_asteroid, asteroids[nucleus_id], push.expected_collision_time,
                        time, time_limit);
            if (time_of_collision != -1) {
                System.out.println("Found a collision in give up");
                energy[i] = push.energy;
                direction[i] = push.direction;
                next_push = time_of_collision;
                nucleus_id = Math.min(i, nucleus_id);
                progress = true;
                return;
            }
        }

        for (int a=0; a<n; a++) {
            int i = a;
            if (i == nucleus_id)
                continue;
            Push push = Hohmann.generatePush(asteroids[i], i, asteroids[nucleus_id], time);
            Asteroid pushed_asteroid = Asteroid.push(asteroids[i], time, push.energy, push.direction);
            long time_of_collision = CollisionChecker.checkCollision(pushed_asteroid, asteroids[nucleus_id], push.expected_collision_time,
                        time, time_limit);
            if (time_of_collision != -1) {
                System.out.println("Found a collision in give up");
                energy[i] = push.energy;
                direction[i] = push.direction;
                next_push = time_of_collision;
                nucleus_id = Math.min(i, nucleus_id);
                progress = true;
                return;
            }
        }
        */
/*
        for (int a=0; a<largest_asteroids.size(); a++) {
            int i = a;
            for (int b=a+1; b<largest_asteroids.size(); b++) {
                int j = b;
                if (asteroids[i].mass > asteroids[j].mass) {
                    int t = i;
                    i = j;
                    j = t;
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
                    nucleus_id = Math.min(i, j);
                    return;
                }
            }
        }

        for (int a=0; a<n; a++) {
            int i = a;
            for (int b=a+1; b<n; b++) {
                int j = b;
                if (asteroids[i].mass > asteroids[j].mass) {
                    int t = i;
                    i = j;
                    j = t;
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
                    nucleus_id = Math.min(i, j);
                    return;
                }
            }
        }

    }*/
}
