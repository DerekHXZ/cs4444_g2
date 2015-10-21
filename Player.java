package pb.g2;

import pb.sim.Point;
import pb.sim.Orbit;
import pb.sim.Asteroid;

import java.util.*;

public class Player implements pb.sim.Player {

    // current time, time limit
    private long time = -1;
    private long time_limit = -1;
    private long min_time_limit = 36500;

    private int number_of_asteroids;

	private long period;
    private boolean progress;

    private static double EPSILON = 10e-6;
    private Asteroid nucleus;
    private long nucleus_id;
    private double total_mass = 0.0;

    private boolean finish_flag = false;
    private double target_mass;

    private double unlock_time = -1;

    private HashSet<Long> seenId;

    private ArrayList<Long> troublemakers = new ArrayList<Long>();
    private double orbit_multiplier;

    private Queue<Push> push_queue;

    private HashSet<Long> usedId;

    // print orbital information
	public void init(Asteroid[] asteroids, long time_limit)
	{
		if (Orbit.dt() != 24 * 60 * 60)
			throw new IllegalStateException("Time quantum is not a day");
		this.time_limit = time_limit;
		this.number_of_asteroids = asteroids.length;
		this.period = time_limit / this.number_of_asteroids;

        // Sort asteroids in order of how attractive they are to become nucleus
        int n = asteroids.length;
        ArrayList<ComparableAsteroid> sorted_asteroids = new ArrayList<ComparableAsteroid>();

        // Compute mean and stddev of asteroid masses
        double mean = Utils.mean(asteroids);
        double stddev = Utils.stddev(asteroids, mean);

        Point asteroid_position = new Point();
        Point sun = new Point(0, 0);

        double largest_mass = 0;

        for (int i = 0; i < n; i++) {
            asteroids[i].orbit.positionAt(time - asteroids[i].epoch, asteroid_position);
            total_mass += asteroids[i].mass;
            if (asteroids[i].mass > largest_mass) {
                largest_mass = asteroids[i].mass;
            }
            sorted_asteroids.add(new ComparableAsteroid(asteroids[i], i, Point.distance(sun, asteroid_position), asteroids[i].mass,
                    asteroids[i].orbit.velocityAt(0).magnitude(), mean, stddev, asteroids));
        }
        Collections.sort(sorted_asteroids);
        if (largest_mass >= total_mass / 2) {
            target_mass = total_mass;
        } else {
            target_mass = total_mass / 2;
        }

        System.out.println("Total mass is: " + total_mass);
        System.out.println("Target mass is: " + target_mass);

        // Get nucleus asteroid to which we will push all other asteroids
        int nucleus_index = sorted_asteroids.get(n - 1).index;
        nucleus = asteroids[nucleus_index];
        nucleus_id = nucleus.id;

        System.out.println("Predicted Energy: " + sorted_asteroids.get(n - 1).energy);
        nucleus_id = asteroids[nucleus_index].id;
        // System.out.println("Found nucleus id " + nucleus_id + ", mass " + asteroids[nucleus_id].mass);

        // Asteroids seen before
        seenId = new HashSet<Long>();
        for (Asteroid a : asteroids) {
            seenId.add(a.id);
        }

        usedId = new HashSet<>();
        usedId.add(nucleus_id);

        // Initialize Queue
        push_queue = new PriorityQueue<>(new Push.TimeComparator());

        if (nucleus.orbit.a < 3e11) {
            orbit_multiplier = 1.01;
        } else if (nucleus.orbit.a < 8e11) {
            if (time_limit > 2*min_time_limit) {
                orbit_multiplier = 1.02;
            } else {
                orbit_multiplier = 1.05;
            }
        } else {
            orbit_multiplier = 1.1;
        }
	}

    // try to push asteroid
    public void play(Asteroid[] asteroids,
                     double[] energy, double[] direction) {
        time++;

        if (time > 0.9 * time_limit) {
            finish_flag = true;
        }

        int n = asteroids.length;

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

        if (asteroids.length < number_of_asteroids) {
            System.out.println("A collision just occurred at time " + time);
            // Check for non-circular orbit
            int new_asteroid_idx = 0;
            for (int i = 0; i < asteroids.length; i++) {
                if (!seenId.contains(asteroids[i].id)) {
                    new_asteroid_idx = i;
                    seenId.add(asteroids[i].id);

                    if (Math.abs(asteroids[i].orbit.a - asteroids[i].orbit.b) > EPSILON) {
                        // Correct for non-circular orbit
                        System.out.println("CORRECTION");
                        Push push = Hohmann.generateCorrection(asteroids[i], i, time);
                        energy[i] = push.energy;
                        direction[i] = push.direction;

                        asteroids[i] = Asteroid.push(asteroids[i], time, energy[i], direction[i]);

                        // Redo the queue

                        Queue<Push> new_queue = new PriorityQueue<>(new Push.TimeComparator());
                        for (Push p: push_queue) {
                            Asteroid a = p.asteroid;
                            long time_to_push = Hohmann.timeToPush(time, a, asteroids[i]);
                            if (time_to_push != -1) {
                                Push np = Hohmann.generatePush(a, -1, asteroids[i], time_to_push);
                                if (time_to_push + np.expected_collision_time <= time_limit) {
                                    new_queue.add(np);
                                } else {
                                    usedId.remove(a.id);
                                }
                            } else {
                                System.out.println("WTF");
                                usedId.remove(a.id);
                            }
                        }
                        push_queue = new_queue;

                    }
                }
            }

            if (Utils.findAsteroidById(asteroids, nucleus_id) == null) {
                nucleus_id = asteroids[new_asteroid_idx].id;
                System.out.println("NUCLEUS ID CHANGED");
                usedId.add(nucleus_id);

            }
            number_of_asteroids = asteroids.length;

            unlock_time = -1;
        }

        nucleus = Utils.findAsteroidById(asteroids, nucleus_id);

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
                Push push = Hohmann.generatePushToRadius(ast, i, ast.orbit.a*orbit_multiplier, time);
                energy[i] = push.energy;
                direction[i] = push.direction;
            }
        }


        if (finish_flag) {
            finishGame(asteroids, nucleus, energy, direction);
            return;
        }

        if (time < unlock_time) return;

        while (!push_queue.isEmpty() && time == push_queue.peek().time) {
            Push push_info = push_queue.remove();

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
                    asteroids[index] = Asteroid.push(asteroids[index], time, push_info.energy, push_info.direction);
                    System.out.println("This is " + time + ". Collision will happen at " + time + push_info.expected_collision_time);

                    unlock_time = time + push_info.expected_collision_time;
                    while (!push_queue.isEmpty() && push_queue.peek().time < time + push_info.expected_collision_time) {
                        usedId.remove(push_queue.remove().asteroid.id);
                    }
                } else {
                    System.out.println("Didn't happen");
                    usedId.remove(push_info.asteroid.id);
                }
            }
        }

        if (time < unlock_time) return;


        double used_mass = 0;
        for (Asteroid a: asteroids) {
            if (usedId.contains(a.id)) {
                used_mass += a.mass;
            }
        }

        if (used_mass < target_mass) {
            // Of all remaining asteroids, find the one with lowest energy push
            System.out.println("There are " + n + " asteroids to be considered.");
            ArrayList<Push> pushes = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (asteroids[i].id == nucleus_id) {
                    continue;
                }
                int curr_asteroid_index = i;
                Asteroid curr_asteroid = asteroids[curr_asteroid_index];
                // Ignore asteroids with elliptical orbits
                if (usedId.contains(curr_asteroid.id)) {
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

            Collections.sort(pushes, new Push.EnergyComparator());

            double mass_considered = 0;
            ArrayList<Push> valid_pushes = new ArrayList<>();
            for (Push p: pushes) {
                if (mass_considered < target_mass - used_mass) {
                    mass_considered += p.asteroid.mass;
                    valid_pushes.add(p);
                    System.out.println("Time: " + p.time / 365.0 + " Energy: " + p.energy + " Mass: " + target_mass);
                    push_queue.add(p);
                    usedId.add(p.asteroid.id);
                }
            }

        }

    }

    Push push_info = null;
    long next_push = -1;

    /**
     * Worst case: If we could not collide anything into the nucleus,
     * find the biggest masses and try to collide them
     */
    public void finishGame(Asteroid[] asteroids, Asteroid nucleus, double[] energy, double[] direction) {

        if (time < next_push) return;
        if (time == next_push) {
            int index = 0;
            for (index = 0; index < asteroids.length; index++) {
                if (asteroids[index].id == push_info.asteroid.id)
                    break;
            }
            if (index < asteroids.length) {
                energy[index] = push_info.energy;
                direction[index] = push_info.direction;
                asteroids[index] = Asteroid.push(asteroids[index], time, push_info.energy, push_info.direction);
            }
        }

        int n = asteroids.length;
        ArrayList<Asteroid> largest_asteroids = new ArrayList<Asteroid>();
        ArrayList<Push> pushes = new ArrayList<Push>();

        for (int i = 0; i < n; i++) {
            largest_asteroids.add(asteroids[i]);
        }

        // Sort asteroids in decreasing order by mass
        Collections.sort(largest_asteroids, new Comparator<Asteroid>() {
            public int compare(Asteroid a1, Asteroid a2) {
                return -1*(Double.compare(a1.mass, a2.mass));
            }
        });

        for (int i = 0; i < largest_asteroids.size(); i++) {
            Asteroid curr_asteroid = largest_asteroids.get(i);
            long time_to_push = Hohmann.timeToPush(time, curr_asteroid, nucleus);
            if (time_to_push != -1) {
                Push push = Hohmann.generatePush(curr_asteroid, i, nucleus, time_to_push);
                if (time_to_push + push.expected_collision_time < time_limit) {
                    pushes.add(push);
                }
            }
        }
        Collections.sort(pushes, new Push.TimeComparator());
        if (!pushes.isEmpty()) {
            push_info = pushes.get(0);
            next_push = push_info.time;
            System.out.println("This is " + time + ". Push will happen at " + next_push);
        }
    }
}
