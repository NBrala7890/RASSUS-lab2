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

            // Storing all the NO2 readings from the readings.csv file
            storeAllNO2Readings();

            // Initialising a custom datagram socket for UDP communication
            socket = new SimpleSimulatedDatagramSocket(sensorModel.getPort(), PACKAGE_LOSS_RATE, PACKAGE_DELAY, running);

            /* Telling the socket to wait for up to 10 milliseconds when calling receive()
            If no packet is received within this time, the method throws a SocketTimeoutException */
            socket.setSoTimeout(10);

            // Initialising a local clock that simulates time progression in the system
            clock = new ConcurrentEmulatedSystemClock(new EmulatedSystemClock());

            // Getting the sensor ids of all the other sensors
            Collection<Integer> allSensorIds = new ArrayList<>();
            for (SensorModel sensorModel1 : otherSensors)
                allSensorIds.add(sensorModel1.getId());

            // Adding this sensor's id to the vector
            allSensorIds.add(sensorModel.getId());

            // Creating an empty vector whose keys are sensor ids and all the values are initially set to 0
            // A data structure that maintains a mapping between sensor IDs and their respective timestamps
            vectorTimestamp = new Vector(allSensorIds.stream().mapToInt(i -> i).toArray());

            // A buffer where outgoing messages are queued before being sent over the network (via UDP)
            /* Sensors produce readings and add them to the queue sendQueue,
            and a separate thread will retrieve and send them */
            /* A blocking queue allows threads to block (wait) when trying to retrieve
            an item from the queue if it is empty, or when trying to add an item if it is full.
            This makes it easier to synchronize producers and consumers. */
            sendQueue = new LinkedBlockingQueue<>();

            // A map used for tracking the messages that have been sent but not yet acknowledged (confirmed)
            /* After sending a message, it’s added to this map.
            When an acknowledgment is received, the message can be removed from the map. */
            /* ConcurrentHashMap is a thread-safe version of the standard HashMap
            (allows multiple threads to read and write concurrently without the need for explicit synchronization) */
            unacknowledged = new ConcurrentHashMap<>();

            // Planned execution
            // ScheduledExecutorService provides a way to schedule tasks to run periodically or after a delay
            // A pool of size 1 because there is only one thread handling all scheduled tasks
            scheduledExecutorService = Executors.newScheduledThreadPool(1);

            // Starting the workers
            startWorkers();

            // Starting the loop
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
        long lifeInSeconds = TimeUnit.MILLISECONDS.toSeconds(clock.currentTimeMillis(null));

        // Returning the NO2 reading stored on the index based on the elapsed sensor life
        myCurrentNO2Reading = allNO2Readings.get((int) (lifeInSeconds % 100));

    }

    private void startWorkers() {

        // Creating a worker that will handle the RECEIVING of UDP packages
        Runnable receiveWorker = new ReceiveWorker(
                sensorModel.getId(), clock, vectorTimestamp, socket, running, sendQueue, unacknowledged, tempMessages);

        // Creating a worker that will handle the SENDING of UDP packages
        Runnable sendWorker = new SendWorker(socket, running, sendQueue, unacknowledged);

        // Creating a worker that will handle the PRINTING of UDP packages
        Runnable printWorker = new PrintWorker(tempMessages, scalarTimestampSorted, vectorTimestampSorted);

        // Starting the receiveWorker in a separate thread
        new Thread(receiveWorker).start();

        // Starting the sendWorker in a separate thread
        new Thread(sendWorker).start();

        // Setting the printing to be done periodically (every 5 seconds)
        scheduledExecutorService.scheduleAtFixedRate(printWorker, 5, 5, TimeUnit.SECONDS);

        logger.info("Workers started successfully!\n");

    }

    private void loop() throws IOException, InterruptedException {

        while (running.get()) {

            // Marking the start timestamp
            long start = System.currentTimeMillis();

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
