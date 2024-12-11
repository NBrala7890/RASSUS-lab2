package hr.fer.tel.rassus.clock;

import java.util.Random;

public class EmulatedSystemClock {

    private long startTime;
    private double jitter; //postotak odstupanja u jednoj sekudni

    public EmulatedSystemClock() {
        startTime = System.currentTimeMillis();
        Random r = new Random();
        jitter = (r.nextInt(20 )) / 100d;// slučajni jitter između 0 i 20, podijeljen s 100

    }

    public long currentTimeMillis() {
        long current = System.currentTimeMillis();
        long diff =current - startTime;
        double coef = diff / 1000;
        return startTime + Math.round(diff * Math.pow((1+jitter), coef)); //Vrati vrijeme s dodanim jitterom, eksponencijalni rast
    }
}