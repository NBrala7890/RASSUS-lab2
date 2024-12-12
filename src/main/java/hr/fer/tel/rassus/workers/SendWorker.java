package hr.fer.tel.rassus.workers;

import hr.fer.tel.rassus.message.Message;
import hr.fer.tel.rassus.config.ConfigProperties;
import hr.fer.tel.rassus.config.PacketUtils;
import hr.fer.tel.rassus.config.Pair;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class SendWorker implements Runnable {

    private static final Logger logger = Logger.getLogger(SendWorker.class.getName());

    private final DatagramSocket socket;
    private final AtomicBoolean running;
    private final BlockingQueue<DatagramPacket> sendQueue;
    private final Map<Integer, Pair<DatagramPacket, Long>> unacknowledged;

    public SendWorker(
            DatagramSocket socket,
            AtomicBoolean running,
            BlockingQueue<DatagramPacket> sendQueue,
            Map<Integer, Pair<DatagramPacket, Long>> unacknowledged) {
        this.socket = socket;
        this.running = running;
        this.sendQueue = sendQueue;
        this.unacknowledged = unacknowledged;
    }

    @Override
    public void run() {
        while (running.get()) {
            try {
                //izvuci poruku iz sendQueue
                DatagramPacket sendPacket = sendQueue.poll(10, TimeUnit.MILLISECONDS);
                if (sendPacket != null) {
                    Message m = Message.deserialize(PacketUtils.dataFromDatagramPacket(sendPacket));
                    switch (m.getType()) {
                        case DATA -> {
                            // dodavanje poruke u nepotvrdene
                            unacknowledged.put(m.getMessageId(), new Pair<>(sendPacket, System.currentTimeMillis()));
                            socket.send(sendPacket);
                        }
                        //ako je potvrda samo salji
                        case ACK -> socket.send(sendPacket);
                        default -> throw new IllegalArgumentException(
                                "'%s' is invalid message type!".formatted(m.getType()));
                    }
                }
                // retransmisija nepotvrdenih
                for (Map.Entry<Integer, Pair<DatagramPacket, Long>> entry : unacknowledged.entrySet()) {
                    Pair<DatagramPacket, Long> pair = entry.getValue();
                    long currentTime = System.currentTimeMillis();
                    long sentTime = pair.getV2();
                    // 4 * AVERAGE_DELAY -> jeli proslo dovoljno vremena
                    if ((currentTime - sentTime) > 4 * ConfigProperties.PACKAGE_DELAY) {
                        socket.send(pair.getV1());
                        pair.setV2(currentTime);
                    }
                }
            } catch (Exception e) {
                logger.severe(e.getMessage());
                break;
            }
        }
    }

}
