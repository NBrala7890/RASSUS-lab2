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
                
                /* Retrieving and removing the head of this queue,
                waiting up to the specified wait time if necessary for an element to become available. */
                DatagramPacket sendPacket = sendQueue.poll(10, TimeUnit.MILLISECONDS);
                
                // Making sure the sendPacket is not null before handling the sending
                if (sendPacket != null) {

                    // Extracting the message (that will be sent) from the sendPacket
                    Message messageToSend = Message.deserialize(PacketUtils.dataFromDatagramPacket(sendPacket));

                    // Checking the type of the messages that will be sent
                    switch (messageToSend.getType()) {

                        case DATA -> {

                            /* Adding the message to the unacknowledged messages
                            (as a pair {DatagramPacket, sendingTimestamp}) if the message is going to send DATA */
                            unacknowledged.put(messageToSend.getMessageId(), new Pair<>(sendPacket, System.currentTimeMillis()));

                            // Sending the packet
                            socket.send(sendPacket);

                        }

                        // Sending the packet immediately if its only an ACK message
                        case ACK -> socket.send(sendPacket);

                        // Throwing an exception if the message is nor DATA, nor ACK message
                        default -> throw new IllegalArgumentException(
                                String.format("Message type '%s' is invalid type!", messageToSend.getType()));

                    }

                }

                // RETRANSMISSION of the unacknowledged packets
                for (Map.Entry<Integer, Pair<DatagramPacket, Long>> entry : unacknowledged.entrySet()) {

                    // Retrieving the current time
                    long currentTime = System.currentTimeMillis();

                    // Getting the pair {DatagramPacket, sentTimestamp}
                    Pair<DatagramPacket, Long> pair = entry.getValue();

                    // Extracting the sentTimestamp from the pair
                    long sentTime = pair.getV2();

                    /* If enough time (4 * AVERAGE_DELAY) has passed without getting the ACK
                    the packet is getting sent again */
                    if ((currentTime - sentTime) > 4 * ConfigProperties.PACKAGE_DELAY) {

                        // Sending the packet again
                        socket.send(pair.getV1());

                        // Setting the sentTimestamp of the packet to currentTime
                        pair.setV2(currentTime);

                    }

                }

            }

            catch (Exception e) {
                logger.severe("An error has occurred while sending a packet. " + e.getMessage());
                break;
            }
        }
    }

}
