package hr.fer.tel.rassus.clock;

public class ConcurrentEmulatedSystemClock {

    private final EmulatedSystemClock clock;
    private long offset;

    public ConcurrentEmulatedSystemClock(EmulatedSystemClock clock) {
        this.clock = clock;
        offset = 0;
    }

    public synchronized long currentTimeMillis(Long otherTimestamp) {
        long thisTimestamp = clock.currentTimeMillis();
        if (otherTimestamp != null && otherTimestamp > thisTimestamp) {
            offset += (otherTimestamp - thisTimestamp);
        }
        return thisTimestamp + offset;
    }

}