package hr.fer.tel.rassus.message;

import hr.fer.tel.rassus.config.Vector;

import java.io.Serial;

// An answer/acknowledge message to a received message

public class AckMessage extends Message {

    @Serial
    private static final long serialVersionUID = 1337962199126122096L;

    private final int messageId;

    public AckMessage(int senderId, long scalarTimestamp, Vector vectorTimestamp, int messageId) {
        super(senderId, MessageType.ACK, scalarTimestamp, vectorTimestamp);
        this.messageId = messageId;
    }

    public int getMessageId() {
        return messageId;
    }

}
