package hr.fer.tel.rassus.message;

import hr.fer.tel.rassus.config.Vector;

import java.io.*;
import java.util.Objects;

public abstract class Message implements Serializable {  //rad s bajtovima

    @Serial
    private static final long serialVersionUID = 7163353894428761969L;

    private static int counter;

    public enum MessageType {
        ACK, DATA
    }

    private final int messageId;
    private final int senderId;
    private final MessageType type;
    private long scalarTimestamp;
    private Vector vectorTimestamp;

    protected Message(int senderId, MessageType type, long scalarTimestamp, Vector vectorTimestamp) {
        messageId = counter++;
        this.senderId = senderId;
        this.type = type;
        this.scalarTimestamp = scalarTimestamp;
        this.vectorTimestamp = vectorTimestamp;
    }

    public int getMessageId() {
        return messageId;
    }

    public int getSenderId() {
        return senderId;
    }

    public MessageType getType() {
        return type;
    }

    public long getScalarTimestamp() {
        return scalarTimestamp;
    }

    public void setScalarTimestamp(long scalarTimestamp) {
        this.scalarTimestamp = scalarTimestamp;
    }

    public Vector getVectorTimestamp() {
        return vectorTimestamp;
    }

    public void setVectorTimestamp(Vector vectorTimestamp) {
        this.vectorTimestamp = vectorTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message that = (Message) o;
        return messageId == that.messageId && senderId == that.senderId;
    }


    @Override
    public int hashCode() {
        return Objects.hash(messageId, senderId);
    }

    public static byte[] serialize(Message m) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream objos = new ObjectOutputStream(bos)) {
            objos.writeObject(m);
            return bos.toByteArray();
        }
    }

    public static Message deserialize(byte[] buf) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(buf);
             ObjectInputStream objis = new ObjectInputStream(bis)) {
            return (Message) objis.readObject();
        }
    }

}
