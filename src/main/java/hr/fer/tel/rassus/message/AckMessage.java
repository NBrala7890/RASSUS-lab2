package hr.fer.tel.rassus.message;

import hr.fer.tel.rassus.config.Vector;

import java.io.Serial;

//odgovor na primjljenu poruku
public class AckMessage extends Message {

    @Serial
    private static final long serialVersionUID = 1337962199126122096L;

    private final int messageId;  //id pouke koa se potvrduje

    public AckMessage(int senderId, long scalarTimestamp, Vector vectorTimestamp, int messageId) {
        super(senderId, MessageType.ACK, scalarTimestamp, vectorTimestamp);
        this.messageId = messageId;
    }

    public int getMessageId() {
        return messageId;
    }

}
