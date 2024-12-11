package hr.fer.tel.rassus.models;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

public class Sensor {

    private static final Logger logger = Logger.getLogger(Sensor.class.getName());

    private final SensorModel sensorModel;

    private final HashSet<SensorModel> otherSensors;

    private final long lifeStart;

    private static ArrayList<Double> allNO2Readings;

    private double myCurrentNO2Reading;

    public Sensor(SensorModel sensorModel, HashSet<SensorModel> otherSensors) {

        this.sensorModel = sensorModel;
        this.otherSensors = otherSensors;

        // Marking the start of the sensor lifetime
        this.lifeStart = System.currentTimeMillis();

        // Storing all the NO2 readings from the readings.csv file
        storeAllNO2Readings();

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

}
