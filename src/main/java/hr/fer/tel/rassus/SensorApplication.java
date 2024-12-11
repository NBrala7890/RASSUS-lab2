package hr.fer.tel.rassus;

import hr.fer.tel.rassus.models.Sensor;
import hr.fer.tel.rassus.models.SensorModel;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static hr.fer.tel.rassus.config.ConfigProperties.*;

public class SensorApplication {

    private static final Logger logger = Logger.getLogger(SensorApplication.class.getName());

    private static int id;

    private static int UDPPort;

    private static SensorModel sensorModel;

    private static HashSet<SensorModel> otherSensors;

//    private static void handleCommand(Consumer<String, String> consumer, String command) {
//
//
//        // logger.info("Waiting for a " + command + " command.");
//        System.out.println("Waiting for a " + command + " command.");
//
//        consumer.seekToEnd(consumer.assignment()); // Skip old messages and start from the latest
//
//        while (true) {
//
//            ConsumerRecords<String, String> consumerRecords = consumer.poll(CONSUMER_POLL_TIMEOUT);
//
//            consumerRecords.forEach(record -> {
//                System.out.printf("Consumer Record: (%s, %s, %d, %d)\n",
//                        record.key(), record.value(),
//                        record.partition(), record.offset());
//            });
//
//            consumer.commitAsync();
//
//        }
//
//    }

    private static void handleCommand(Consumer<String, String> consumer, String command) {
        logger.info("[TOPIC = Command] Waiting for " + command + " command...\n");
        boolean received = false;
        do {
            for (ConsumerRecord<String, String> record : consumer.poll(CONSUMER_POLL_TIMEOUT)) {
                if ("command".equalsIgnoreCase(record.topic())) {
                    if (command.equalsIgnoreCase(record.value())) {
                        logger.info("[TOPIC = Command] Command " + command + " received successfully!\n");
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
            handleCommand(consumer, "Start");

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
                System.out.println(sensorModel1.getId());

            // Creating a new sensor that will generate its own readings and exchange them with other nodes
            Sensor sensor = new Sensor(sensorModel, otherSensors);


        } catch (Exception e) {
            logger.severe("An error has occurred while initialising Kafka elements. " + e.getMessage());
        }

    }

}
