package hr.fer.tel.rassus.clock;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class SimpleSimulatedDatagramSocket extends DatagramSocket {

    private static final Logger logger = Logger.getLogger(SimpleSimulatedDatagramSocket.class.getName());
    private final double lossRate;
    private final int averageDelay;
    private final Random random;
    private final AtomicBoolean running;

    // Constructor for the server side (without the time limit)
    public SimpleSimulatedDatagramSocket(int port, double lossRate, int averageDelay, AtomicBoolean running) throws SocketException, IllegalArgumentException {
        super(port);
        this.running = running;
        random = new Random();
        this.lossRate = lossRate;
        this.averageDelay = averageDelay;
        // Setting the time delay to 0
        super.setSoTimeout(0);
    }

    // Constructor for the client side (time limit = 4 * average delay)
    public SimpleSimulatedDatagramSocket(double lossRate, int averageDelay, AtomicBoolean running) throws SocketException, IllegalArgumentException {
        this.running = running;
        random = new Random();
        this.lossRate = lossRate;
        this.averageDelay = averageDelay;
        // Setting the time limit for the client side
        super.setSoTimeout(4 * averageDelay);
    }

    // Method for sending datagram packets across the network
    @Override
    public void send(DatagramPacket packet) {
        if (random.nextDouble() >= lossRate) {
            // Delay between 0 and 2 * average_delay
            new Thread(new OutgoingDatagramPacket(packet, (long) (2 * averageDelay * random.nextDouble()), running)).start();
        }
    }

    private class OutgoingDatagramPacket implements Runnable {
        private final DatagramPacket packet;
        private final long delayTime;
        private final AtomicBoolean running;

        private OutgoingDatagramPacket(DatagramPacket packet, long delayTime, AtomicBoolean running) {
            this.packet = packet;
            this.delayTime = delayTime;
            this.running = running;
        }

        @Override
        public void run() {
            try {

                // The outgoing package is delayed by the given delayTime
                Thread.sleep(delayTime);

                // Sending the package if the socket is still active (running = true)
                if (running.get())
                    SimpleSimulatedDatagramSocket.super.send(packet);
                
            }
            catch (Exception e) {
                logger.severe("An error has occurred while sending the UDP package. " + e.getMessage());
            }
        }
    }
}

