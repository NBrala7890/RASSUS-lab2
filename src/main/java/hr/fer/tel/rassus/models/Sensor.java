package hr.fer.tel.rassus.models;

import hr.fer.tel.rassus.clock.ConcurrentEmulatedSystemClock;
import hr.fer.tel.rassus.clock.EmulatedSystemClock;
import hr.fer.tel.rassus.config.PacketUtils;
import hr.fer.tel.rassus.config.Pair;
import hr.fer.tel.rassus.config.Vector;
import hr.fer.tel.rassus.message.DataMessage;
import hr.fer.tel.rassus.message.Message;
import hr.fer.tel.rassus.clock.SimpleSimulatedDatagramSocket;
import hr.fer.tel.rassus.workers.PrintWorker;
import hr.fer.tel.rassus.workers.ReceiveWorker;
import hr.fer.tel.rassus.workers.SendWorker;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static hr.fer.tel.rassus.config.ConfigProperties.PACKAGE_DELAY;
import static hr.fer.tel.rassus.config.ConfigProperties.PACKAGE_LOSS_RATE;

public class Sensor implements Runnable {

    private static final Logger logger = Logger.getLogger(Sensor.class.getName());

    private final SensorModel sensorModel;

    private final HashSet<SensorModel> otherSensors;

    private final Collection<DataMessage> tempMessages;

    private final Collection<DataMessage> scalarTimestampSorted;

    private final Collection<DataMessage> vectorTimestampSorted;

    private final AtomicBoolean running;

    private long lifeStart;

    private static ArrayList<Double> allNO2Readings;

    private double myCurrentNO2Reading;

    private DatagramSocket socket;

    private ConcurrentEmulatedSystemClock clock;

    private Vector vectorTimestamp;

    private BlockingQueue<DatagramPacket> sendQueue;

    private Map<Integer, Pair<DatagramPacket, Long>> unacknowledged;

    private ScheduledExecutorService scheduledExecutorService;

    public Sensor(SensorModel sensorModel,
                  AtomicBoolean running,
                  HashSet<SensorModel> otherSensors,
                  Collection<DataMessage> tempMessages,
                  Collection<DataMessage> scalarTimestampSorted,
                  Collection<DataMessage> vectorTimestampSorted) {

        this.sensorModel = sensorModel;
        this.running = running;
        this.otherSensors = otherSensors;
        this.tempMessages = tempMessages;
        this.scalarTimestampSorted = scalarTimestampSorted;
        this.vectorTimestampSorted = vectorTimestampSorted;

    }

    @Override
    public void run() {

        try {

            logger.info("Starting the sensor...\n");

            // Marking the start of the sensor lifetime
            this.lifeStart = System.currentTimeMillis();

            // Storing all the NO2 readings from the readings.csv file
            storeAllNO2Readings();

            socket = new SimpleSimulatedDatagramSocket(sensorModel.getPort(), PACKAGE_LOSS_RATE, PACKAGE_DELAY, running);
            socket.setSoTimeout(10);
            clock = new ConcurrentEmulatedSystemClock(new EmulatedSystemClock());

            // Adding the other sensors' ids to the vector
            Collection<Integer> allSensorIds = new ArrayList<>();
            for (SensorModel sensorModel1 : otherSensors)
                allSensorIds.add(sensorModel1.getId());

            // Adding this sensor's id to the vector
            allSensorIds.add(sensorModel.getId());
            vectorTimestamp = new Vector(allSensorIds.stream().mapToInt(i -> i).toArray());

            // Queue for sending
            sendQueue = new LinkedBlockingQueue<>();

            // Sent, but not confirmed
            unacknowledged = new ConcurrentHashMap<>();

            // Planned execution
            scheduledExecutorService = Executors.newScheduledThreadPool(1);

            startWorkers();

            loop();

        }
        catch (Exception e) {
            logger.severe(e.getMessage());
        }
        finally {
            scheduledExecutorService.shutdown();
            socket.close();
        }

    }

    public void storeAllNO2Readings() {

        allNO2Readings = new ArrayList<>();

        try {

            // Reading all the lines from the readings.csv file
            List<String> allLines = Files.readAllLines(Paths.get("readings.csv"));

            // Removing the first line from the readings.csv file
            allLines.remove(0);

            String NO2ReadingString;

            for (String line : allLines) {

                // Extracting the NO2 reading
                NO2ReadingString = line.split(",", -1)[4];

                // Setting the NO2 reading to 0 if the reading is empty
                if (NO2ReadingString.isEmpty())
                    NO2ReadingString = "0";

                allNO2Readings.add(Double.parseDouble(NO2ReadingString));

            }

        }
        catch (Exception e) {
            logger.severe("An error has occurred while reading from CSV file. " + e.getMessage() + "\n");
        }

    }

    public void generateReading() {

        // Calculating the duration (in seconds) of this sensor's life
        long lifeInSeconds = ((System.currentTimeMillis() - lifeStart) / 1000);

        // Returning the NO2 reading stored on the index based on the elapsed sensor life
        myCurrentNO2Reading = allNO2Readings.get((int) (lifeInSeconds % 100 + 1));

    }

    private void startWorkers() {

        Runnable receiveWorker = new ReceiveWorker(
                sensorModel.getId(), clock, vectorTimestamp, socket, running, sendQueue, unacknowledged, tempMessages);

        Runnable sendWorker = new SendWorker(socket, running, sendQueue, unacknowledged);

        Runnable printWorker = new PrintWorker(tempMessages, scalarTimestampSorted, vectorTimestampSorted);

        new Thread(receiveWorker).start();

        new Thread(sendWorker).start();

        // Print periodically
        scheduledExecutorService.scheduleAtFixedRate(printWorker, 5, 5, TimeUnit.SECONDS);

        logger.info("Workers started successfully!\n");

    }

    private void loop() throws IOException, InterruptedException {

        long start;

        while (running.get()) {

            start = System.currentTimeMillis();

            // Generates a new reading and stores it to the myCurrentNO2Reading variable
            generateReading();

            // Storing the new reading as a temporary data
            tempMessages.add(new DataMessage(
                    sensorModel.getId(),
                    clock.currentTimeMillis(null),
                    vectorTimestamp.update(sensorModel.getId(), null, true),
                    myCurrentNO2Reading
            ));

            // Sending the new reading to other nodes
            for (SensorModel otherSensor : otherSensors) {

                // Creating a new message
                Message message = new DataMessage(
                        sensorModel.getId(),
                        clock.currentTimeMillis(null),
                        vectorTimestamp.update(sensorModel.getId(), null, true),
                        myCurrentNO2Reading
                );

                sendQueue.put(PacketUtils.createSendPacket(
                        message,
                        InetAddress.getByName(otherSensor.getAddress()),
                        otherSensor.getPort()));

            }

            Thread.sleep(Math.max(0, 1000 - (System.currentTimeMillis() - start)));

        }

    }

}
