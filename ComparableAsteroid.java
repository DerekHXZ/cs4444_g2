package pb.g2;

import pb.sim.Asteroid;

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
        double energy = 0;
        for (Asteroid other : asteroids) {
            Push push = Hohmann.generatePush(other, -1, this.asteroid, 0);
            energy += push.energy;
        }
        return energy;
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
