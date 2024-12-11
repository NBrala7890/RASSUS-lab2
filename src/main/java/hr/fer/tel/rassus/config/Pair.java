package hr.fer.tel.rassus.config;

import java.util.Objects;

public class Pair<T1, T2> {

    private T1 v1;
    private T2 v2;

    public Pair(T1 v1, T2 v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

    public T1 getV1() {
        return v1;
    }

    public T2 getV2() {
        return v2;
    }

    public void setV2(T2 v2) {
        this.v2 = v2;
    }

    @Override
    public boolean equals(Object o) {
        return (o != null && o.getClass() == this.getClass() && equalsPair((Pair<?, ?>) o));
    }

    private boolean equalsPair(Pair<?, ?> other) {
        return v1.equals(other.v1) && v2.equals(other.v2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(v1, v2);
    }

}
