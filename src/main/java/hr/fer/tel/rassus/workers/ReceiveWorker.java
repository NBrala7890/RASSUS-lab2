package hr.fer.tel.rassus.workers;

import hr.fer.tel.rassus.clock.ConcurrentEmulatedSystemClock;
import hr.fer.tel.rassus.message.AckMessage;
import hr.fer.tel.rassus.message.DataMessage;
import hr.fer.tel.rassus.message.Message;
import hr.fer.tel.rassus.config.PacketUtils;
import hr.fer.tel.rassus.config.Pair;
import hr.fer.tel.rassus.config.Vector;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class ReceiveWorker implements Runnable {

    private static final Logger logger = Logger.getLogger(ReceiveWorker.class.getName());
    private static final int BUFF_SIZE = 1024;

    private final int nodeId;
    private final ConcurrentEmulatedSystemClock clock;
    private final Vector vectorTimestamp;
    private final DatagramSocket socket;
    private final AtomicBoolean running;
    private final BlockingQueue<DatagramPacket> sendQueue;
    private final Map<Integer, Pair<DatagramPacket, Long>> unAckPackets;
    private final Collection<DataMessage> tempMessages;
    private final byte[] buffer;

    public ReceiveWorker(int nodeId, ConcurrentEmulatedSystemClock clock, Vector vectorTimestamp, DatagramSocket socket, AtomicBoolean running, BlockingQueue<DatagramPacket> sendQueue, Map<Integer, Pair<DatagramPacket, Long>> unAckPackets, Collection<DataMessage> tempMessages) {
        this.nodeId = nodeId;
        this.clock = clock;
        this.vectorTimestamp = vectorTimestamp;
        this.socket = socket;
        this.running = running;
        this.sendQueue = sendQueue;
        this.unAckPackets = unAckPackets;
        this.tempMessages = tempMessages;
        buffer = new byte[BUFF_SIZE];
    }

    @Override
    public void run() {
        while (running.get()) {
            try {
                DatagramPacket recievedPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(recievedPacket);
                Message m = Message.deserialize(PacketUtils.dataFromDatagramPacket(recievedPacket));
                //azuriraj vremena
                long newScalar = clock.currentTimeMillis(m.getScalarTimestamp());
                Vector newVector = vectorTimestamp.update(nodeId, m.getVectorTimestamp(), true);
                m.setScalarTimestamp(newScalar);
                m.setVectorTimestamp(newVector);
                switch (m.getType()) {
                    case DATA -> {
                        //ako prima data onda salji ack
                        Message ackMessage = new AckMessage(
                                nodeId,
                                newScalar,
                                newVector,
                                m.getMessageId());
                        sendQueue.put(PacketUtils.createSendPacket(ackMessage, recievedPacket.getAddress(), recievedPacket.getPort()));
                        //spremi poruku u privremeno
                        tempMessages.add((DataMessage) m);
                        vectorTimestamp.update(nodeId, null, false);  //radnja zavrsena, povecaj za 1
                    }
                    case ACK -> {
                        //makni iz nepotvrdenih
                        unAckPackets.remove(m.getMessageId());
                        vectorTimestamp.update(nodeId, null, false);  //radnja zavrsena, povecaj za 1
                    }
                    default -> throw new IllegalArgumentException(
                            String.format("'%s' is invalid type!", m.getType()));
                }
            } catch (SocketTimeoutException ignored) {
            } catch (Exception e) {
                logger.severe(e.getMessage());
                break;
            }
        }
    }

}
