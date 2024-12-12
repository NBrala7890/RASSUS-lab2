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
        for (int id : ids)
            mapper.put(id, 0);
    }

    private Vector(Map<Integer, Integer> mapper) {
        this.mapper = new TreeMap<>(mapper);
    }

    public synchronized Vector update(int idToIncrement, Vector other, boolean returnNew) {
        if (other == null) {
            mapper.merge(idToIncrement, 1, Integer::sum); //povecaj za 1
        } else {
            Map<Integer, Integer> otherMapper = other.mapper;
            for (int id : mapper.keySet()) {  //za svaki id odaberi vecu vrijednost
                mapper.merge(id, 1, (oldVal, newVal) -> id == idToIncrement
                        ? oldVal + 1
                        : Math.max(oldVal, otherMapper.get(id))
                );
            }
        }
        return returnNew ? new Vector(mapper) : this;
    }

    @Override
    public int compareTo(Vector other) {
        return Arrays.compare(mapper.values().toArray(new Integer[0]), other.mapper.values().toArray(new Integer[0]));
        // Replace Arrays.compare with custom logic for compatibility
//        Integer[] thisValues = mapper.values().toArray(new Integer[0]);
//        Integer[] otherValues = other.mapper.values().toArray(new Integer[0]);
//        for (int i = 0; i < Math.min(thisValues.length, otherValues.length); i++) {
//            int cmp = Integer.compare(thisValues[i], otherValues[i]);
//            if (cmp != 0) {
//                return cmp;
//            }
//        }
//        return Integer.compare(thisValues.length, otherValues.length);
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ", "[", "]");
        mapper.forEach((key, value) -> sj.add("%d->%3d".formatted(key, value)));
        return sj.toString();
//        StringJoiner sj = new StringJoiner(", ", "[", "]");
//        mapper.forEach((key, value) -> sj.add(String.format("%d->%3d", key, value)));
//        return sj.toString();
    }

}
