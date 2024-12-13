package hr.fer.tel.rassus.config;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

public class Vector implements Comparable<Vector>, Serializable {

    @Serial
    private static final long serialVersionUID = 6324089846823106778L;

    private final Map<Integer, Integer> mapper;

    public Vector(int[] ids) {

        mapper = new TreeMap<>();

        // Setting the values for all the ids to 0
        for (int id : ids)
            mapper.put(id, 0);

    }

    private Vector(Map<Integer, Integer> mapper) {
        this.mapper = new TreeMap<>(mapper);
    }

    public synchronized Vector update(int sensorIdToIncrement, Vector otherVectorTimestamp, boolean returnNew) {

        // Only incrementing the value of the given sensor by 1
        if (otherVectorTimestamp == null)
            mapper.merge(sensorIdToIncrement, 1, Integer::sum);

        // If the otherVectorTimestamp is present we merge the two vector timestamps
        else {

            // Extracting the mapper from otherVectorTimestamp
            Map<Integer, Integer> otherMapper = otherVectorTimestamp.mapper;

            // Incrementing the value of the given sensor by 1
            // Merging the values of every sensorId by always keeping the larger value of the two values
            for (int id : mapper.keySet()) {

                mapper.merge(id, 1, (oldVal, newVal) -> id == sensorIdToIncrement
                        ? oldVal + 1
                        : Math.max(oldVal, otherMapper.get(id))
                );

            }
        }

        // Returning the new vectorTimeStamp if required by the parameter returnNew
        return returnNew ? new Vector(mapper) : this;

    }

    @Override
    public int compareTo(Vector other) {

        return Arrays.compare(mapper.values().toArray(new Integer[0]), other.mapper.values().toArray(new Integer[0]));

    }

    @Override
    public String toString() {

        StringJoiner sj = new StringJoiner(", ", "[", "]");
        mapper.forEach((key, value) -> sj.add("%d->%3d".formatted(key, value)));
        return sj.toString();

    }

}
