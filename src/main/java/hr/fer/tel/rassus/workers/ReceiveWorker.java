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
                
                // Creating a DatagramPacket that will hold the received UDP packet of length buffer.length
                DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);

                // Receives a datagram packet from this socket.
                // This method blocks until a datagram is received.
                // When this method returns, the DatagramPacket's buffer is filled with the data received.
                socket.receive(receivedPacket);
                
                // Extracting the received message from the received packet
                Message receivedMessage = Message.deserialize(PacketUtils.dataFromDatagramPacket(receivedPacket));
                
                // Extracting the scalar timestamp from the received message
                long newScalarTimestamp = clock.currentTimeMillis(receivedMessage.getScalarTimestamp());

                // Updating the current vectorTimestamp with the vectorTimestamp from the received message
                Vector newVector = vectorTimestamp.update(nodeId, receivedMessage.getVectorTimestamp(), true);

                // Updating the scalarTimestamp of the received message with the new scalarTimestamp
                receivedMessage.setScalarTimestamp(newScalarTimestamp);

                // Updating the vectorTimestamp of the received message with the new vectorTimestamp
                receivedMessage.setVectorTimestamp(newVector);

                // Checking the type of the received messages
                switch (receivedMessage.getType()) {

                    // If the received message was carrying DATA, we're sending ACK
                    case DATA -> {

                        Message ackMessage = new AckMessage(
                                nodeId,
                                newScalarTimestamp,
                                newVector,
                                receivedMessage.getMessageId());

                        sendQueue.put(PacketUtils.createSendPacket(ackMessage, receivedPacket.getAddress(), receivedPacket.getPort()));

                        // Storing the received message amongst temporary messages (printing queue)
                        tempMessages.add((DataMessage) receivedMessage);

                        // An action has been completed - incrementing by 1...
                        vectorTimestamp.update(nodeId, null, false);

                    }

                    // If the received message was an ACK message, we're removing it from the unacknowledged messages
                    case ACK -> {

                        // An action has been completed - incrementing by 1...
                        unAckPackets.remove(receivedMessage.getMessageId());
                        vectorTimestamp.update(nodeId, null, false);

                    }

                    // Throwing an exception if the received message is nor DATA, nor ACK message
                    default -> throw new IllegalArgumentException(
                            String.format("Message type '%s' is invalid type!", receivedMessage.getType()));

                }

            }

            catch (SocketTimeoutException ignored) {}

            catch (Exception e) {
                logger.severe("An error has occurred while receiving a packet. " + e.getMessage());
                break;
            }

        }

    }

}
