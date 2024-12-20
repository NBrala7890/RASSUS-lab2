package hr.fer.tel.rassus;

import hr.fer.tel.rassus.message.DataMessage;
import hr.fer.tel.rassus.models.Sensor;
import hr.fer.tel.rassus.models.SensorModel;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Collection;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static hr.fer.tel.rassus.config.ConfigProperties.*;

public class SensorApplication {

    private static final Logger logger = Logger.getLogger(SensorApplication.class.getName());

    private static int id;

    private static int UDPPort;

    private static SensorModel sensorModel;

    private static HashSet<SensorModel> otherSensors;

    private static final AtomicBoolean running = new AtomicBoolean();

    private static final Collection<DataMessage> tempMessages = ConcurrentHashMap.newKeySet();

    private static final Collection<DataMessage> scalarTimestampSorted = new ConcurrentLinkedQueue<>();

    private static final Collection<DataMessage> vectorTimestampSorted = new ConcurrentLinkedQueue<>();

    private static void handleCommand(Consumer<String, String> consumer,
                                      String command,
                                      java.util.function.Consumer<Void> action) {
        logger.info("[TOPIC = Command] Waiting for " + command + " command...\n");
        boolean received = false;
        do {
            for (ConsumerRecord<String, String> record : consumer.poll(CONSUMER_POLL_TIMEOUT)) {
                if ("command".equalsIgnoreCase(record.topic())) {
                    if (command.equalsIgnoreCase(record.value())) {
                        logger.info("[TOPIC = Command] Command " + command + " received successfully!\n");
                        action.accept(null);
                        received = true;
                        break;
                    }
                }
            }
            consumer.commitAsync();
        } while (!received);
    }

    public static void main(String[] args) {

        // id and UDP port input
        Scanner scanner = new Scanner(System.in);
        try {
            System.out.print("Enter id: ");
            id = Integer.parseInt(scanner.nextLine());
        } catch (Exception e) {
            logger.severe("An error has occurred while reading the given id. " + e.getMessage());
        }
        try {
            System.out.print("Enter UDP port: ");
            UDPPort = Integer.parseInt(scanner.nextLine());
        } catch (Exception e) {
            logger.severe("An error has occurred while reading the given UDP port. " + e.getMessage());
        }

        System.out.println();

        // Creating a new sensor
        sensorModel = new SensorModel(id, "localhost", UDPPort);

        // Kafka consumer and producer initialisation
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties(String.valueOf(sensorModel.getId())));
             KafkaProducer<String, String> producer = new KafkaProducer<>(producerProperties())) {

            // Subscribing the consumer
            consumer.subscribe(TOPICS);

            // Waiting for the "Start" control message
            handleCommand(consumer, "Start", n -> running.set(true));

            // Send registration
            logger.info("[TOPIC = Register] Sending a registration message...\n");
            producer.send(new ProducerRecord<>("Register", SensorModel.toJson(sensorModel)));
            logger.info("[TOPIC = Register] Registration completed successfully!\n");

            // Receive registrations
            logger.info("[TOPIC = Register] Receiving the registrations from the other sensors...\n");
            TimeUnit.SECONDS.sleep(1);
            otherSensors = new HashSet<>();
            while (true) {

                ConsumerRecords<String, String> consumerRecords = consumer.poll(CONSUMER_POLL_TIMEOUT);
                if (consumerRecords.isEmpty())
                    break;
                for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                    if (consumerRecord.topic().equalsIgnoreCase("register")) {
                        SensorModel otherSensor = SensorModel.fromJson(consumerRecord.value());
                        if (!sensorModel.equals(otherSensor))
                            otherSensors.add(otherSensor);
                    }
                }
                consumer.commitAsync();

            }
            logger.info("[TOPIC = Register] Other sensors' registrations received successfully!\n");

            // Printing the other sensors
            TimeUnit.SECONDS.sleep(1);
            for (SensorModel sensorModel1 : otherSensors)
                logger.info("Other sensor id: " + sensorModel1.getId() + "\n");

            /* Creating a new sensor (in a separate thread)
            that will generate its own readings and exchange them with other nodes */
            new Thread(new Sensor(
                    sensorModel,
                    running,
                    otherSensors,
                    tempMessages,
                    scalarTimestampSorted,
                    vectorTimestampSorted)).start();

            // Waiting for the "Stop" control message
            handleCommand(consumer, "Stop", n -> {

                running.set(false);

                try {
                    logger.info("Sleeping...\n");
                    Thread.sleep(2000);
                }
                catch (Exception e) {
                    logger.severe("Interrupted! " + e.getMessage());
                }

                StringBuilder sb = new StringBuilder();
                sb.append("Sorted by scalar:\n");
                sb.append("sensor id: NO2, scalar, vector\n");
                scalarTimestampSorted.forEach(m -> sb.append(m).append("\n"));
                sb.append("\n\n");
                sb.append("Sorted by vector:\n");
                sb.append("sensor id: NO2, scalar, vector\n");
                vectorTimestampSorted.forEach(m -> sb.append(m).append("\n"));
                logger.info(sb.toString());

            });


        } catch (Exception e) {
            logger.severe("An error has occurred while initialising Kafka elements. " + e.getMessage());
        }
        logger.info("Node stopped successfully!");

    }

}
