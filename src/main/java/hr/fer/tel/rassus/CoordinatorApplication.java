package hr.fer.tel.rassus;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static hr.fer.tel.rassus.config.ConfigProperties.producerProperties;

public class CoordinatorApplication {

    private static final Logger logger = Logger.getLogger(CoordinatorApplication.class.getName());

    public static void main(String[] args) {

        try (Producer<String, String> producer = new KafkaProducer<>(producerProperties())) {

            Scanner scanner = new Scanner(System.in);

            System.out.print("Press ENTER to start the Kafka coordinator.");
            scanner.nextLine();
            System.out.println();
            logger.info("Starting the coordinator...\n");
            producer.send(new ProducerRecord<>("Command", "Start"));

            TimeUnit.SECONDS.sleep(1);
            System.out.print("Press ENTER to stop the Kafka coordinator.");
            scanner.nextLine();
            logger.info("Stopping the coordinator...\n");
            producer.send(new ProducerRecord<>("Command", "Stop"));

            System.out.println("The coordinator has finished.");


        } catch (Exception e) {
            logger.severe("An error has occurred with the Kafka producer. " + e.getMessage());
        }

    }

}
