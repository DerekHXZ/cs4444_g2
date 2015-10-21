package pb.g2;

import pb.sim.Asteroid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class ComparableAsteroid implements Comparable<ComparableAsteroid> {
    public Asteroid asteroid;
    public int index;
    public double radius;
    public double mass;
    public double velocity;

    private double mean;
    private double stddev;
    private double energy;

    public ComparableAsteroid(Asteroid asteroid, int index, double radius, double mass, double velocity, double mean, double stddev, Asteroid[] asteroids) {
        this.asteroid = asteroid;
        this.index = index;
        this.radius = radius;
        this.mass = mass;
        this.velocity = velocity;

        this.mean = mean;
        this.stddev = stddev;
        this.energy = getTotalEnergyToPushToAsteroid(asteroids);
    }

    private double getScore() {
        return energy;
    }

    private final int DISCRETE_ENERGY_LVLS = 100000;

    private double getTotalEnergyToPushToAsteroid(Asteroid[] asteroids) {

        ArrayList<Double> energy = new ArrayList<Double>();
        ArrayList<Double> mass   = new ArrayList<Double>();
        ArrayList<Integer> energy_d = new ArrayList<Integer>();
        double total_mass = 0, total_energy = 0;

        for (Asteroid other : asteroids) {
            if (other != this.asteroid) {
                Push push = Hohmann.generatePush(other, -1, this.asteroid, 0);
                energy.add(push.energy);
                mass.add(other.mass);

                total_energy += push.energy;
            }

            total_mass += other.mass;
        }

        for (Double e : energy) {
            energy_d.add((int) (e / total_energy * DISCRETE_ENERGY_LVLS));
        }

        double[][] dp = new double[DISCRETE_ENERGY_LVLS][mass.size()];
        for (int i = 0; i < DISCRETE_ENERGY_LVLS; i++) {
            if (i >= energy_d.get(0)) dp[i][0] = mass.get(0); else dp[i][0] = 0;
            for (int j = 1; j < mass.size(); j++) {
                dp[i][j] = dp[i][j-1];
                if (i > mass.get(j)) {
                    dp[i][j] = Math.max(dp[i][j], dp[i-energy_d.get(j)][j] + mass.get(j));
                }
            }
            if (this.mass + dp[i][mass.size()-1] >= total_mass / 2) {
                return total_energy * i / DISCRETE_ENERGY_LVLS;
            }
        }

        return total_energy;
    }

    private double getTotalEnergy() {
        final double G = 6.67*10e-11;
        return 0.5*mass*Math.pow(velocity, 2) - G*mass/radius;
    }

    private double getGaussian() {
        Random rand = new Random();
        return rand.nextGaussian()*stddev + mean;
    }

    public int compareTo(ComparableAsteroid other) {
        return -1*Double.valueOf(this.getScore()).compareTo(other.getScore());
    }
}
