package hr.fer.tel.rassus.message;

import hr.fer.tel.rassus.config.Vector;

import java.io.Serial;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

// A message that contains data

public class DataMessage extends Message {

    @Serial
    private static final long serialVersionUID = 769634279608093284L;

    private final double data;

    public DataMessage(int senderId, long scalarTimestamp, Vector vectorTimestamp, double data) {
        super(senderId, MessageType.DATA, scalarTimestamp, vectorTimestamp);
        this.data = data;
    }

    public double getData() {
        return data;
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
                "%d: %7.1f, %d, %s",
                getSenderId(), data, TimeUnit.MILLISECONDS.toSeconds(getScalarTimestamp()), getVectorTimestamp());
    }

}
