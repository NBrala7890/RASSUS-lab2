package hr.fer.tel.rassus.clock;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleSimulatedDatagramSocket extends DatagramSocket {
    private final double lossRate;
    private final int averageDelay;
    private final Random random;
    private final AtomicBoolean running;

    // Konstruktor za server stranu (bez vremenskog ograničenja)
    public SimpleSimulatedDatagramSocket(int port, double lossRate, int averageDelay, AtomicBoolean running) throws SocketException, IllegalArgumentException {
        super(port);
        this.running = running;
        random = new Random();
        this.lossRate = lossRate;
        this.averageDelay = averageDelay;
        super.setSoTimeout(0);  // Postavljanje vremenskog ograničenja na nulu za server stranu
    }

    // Konstruktor za klijentsku stranu (vremensko ograničenje = 4 * prosječno kašnjenje)
    public SimpleSimulatedDatagramSocket(double lossRate, int averageDelay, AtomicBoolean running) throws SocketException, IllegalArgumentException {
        this.running = running;
        random = new Random();
        this.lossRate = lossRate;
        this.averageDelay = averageDelay;
        super.setSoTimeout(4 * averageDelay);  // Postavljanje vremenskog ograničenja za klijentsku stranu
    }

    // Metoda za slanje datagram paketa preko simulirane mreže
    @Override
    public void send(DatagramPacket packet) throws IOException {
        if (random.nextDouble() >= lossRate) {
            // Kašnjenje između 0 i 2 * prosječno kašnjenje
            new Thread(new OutgoingDatagramPacket(packet, (long) (2 * averageDelay * random.nextDouble()), running)).start();
        }
    }

    private class OutgoingDatagramPacket implements Runnable {
        private final DatagramPacket packet;
        private final long time;
        private final AtomicBoolean running;

        private OutgoingDatagramPacket(DatagramPacket packet, long time, AtomicBoolean running) {
            this.packet = packet;
            this.time = time;
            this.running = running;
        }

        @Override
        public void run() {
            try {
                // kašnjenja
                Thread.sleep(time);
                if (running.get()) {
                    // Ako je socket još uvijek aktivan, šalji paket
                    SimpleSimulatedDatagramSocket.super.send(packet);
                }
            } catch (InterruptedException e) {
                Thread.interrupted();
            } catch (IOException ex) {
                Logger.getLogger(SimpleSimulatedDatagramSocket.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}

