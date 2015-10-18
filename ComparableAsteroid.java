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

    private double getTotalEnergyToPushToAsteroid(Asteroid[] asteroids) {
        ArrayList<Double> energy = new ArrayList<>();
        for (Asteroid other : asteroids) {
            Push push = Hohmann.generatePush(other, -1, this.asteroid, 0);
            energy.add(push.energy);
        }
        Collections.sort(energy);
        double sum = 0;
        for (int i = 0; i < energy.size() / 2; i ++) {
            sum += energy.get(i);
        }
        return sum;
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
