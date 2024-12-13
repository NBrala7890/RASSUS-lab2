package hr.fer.tel.rassus.clock;

import java.util.Random;

public class EmulatedSystemClock {

    private final long startTime;

    // Average deviation in 1 second
    private final double jitter;

    public EmulatedSystemClock() {
        startTime = System.currentTimeMillis();
        Random r = new Random();
        // Random jitter between 0 and 20, divided by 100
        jitter = (r.nextInt(20 )) / 100d;

    }

    public long currentTimeMillis() {
        long current = System.currentTimeMillis();
        long diff =current - startTime;
        double coefficient = (double) diff / 1000;
        // Return the time with added jitter, EXPONENTIAL growth
        return startTime + Math.round(diff * Math.pow((1+jitter), coefficient));
    }
}