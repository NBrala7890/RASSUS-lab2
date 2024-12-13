package hr.fer.tel.rassus.workers;

import hr.fer.tel.rassus.message.DataMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

public class PrintWorker implements Runnable {

    private static final Logger logger = Logger.getLogger(PrintWorker.class.getName());

    private static int printingInvokedCounter;

    private final Collection<DataMessage> tempMessages;

    private final Collection<DataMessage> scalarTimestampSorted;

    private final Collection<DataMessage> vectorTimestampSorted;

    public PrintWorker(Collection<DataMessage> tempMessages, Collection<DataMessage> scalarTimestampSorted, Collection<DataMessage> vectorTimestampSorted) {
        this.tempMessages = tempMessages;
        this.scalarTimestampSorted = scalarTimestampSorted;
        this.vectorTimestampSorted = vectorTimestampSorted;
    }

    @Override
    public void run() {

        // Keeping the count of the total number of printings
        printingInvokedCounter++;

        // Copying the messages that are to be printed
        List<DataMessage> copied = new ArrayList<>(tempMessages);

        // Emptying the temporarily stored messages because they'll be printed now
        tempMessages.clear();

        // Preparing the StringBuilder for printing
        StringBuilder sb = new StringBuilder();

        sb.append("\nPrint counter: ").append(printingInvokedCounter);

        sb.append("\n\n");

        // Calculating the average value of readings
        double avg = copied.stream().mapToDouble(DataMessage::getData).sum() / copied.size();
        sb.append("Average reading value: ").append(avg);

        sb.append("\n\n");

        // Printing the messages sorted by the scalar values
        sb.append("Sorted by scalar:\n");
        sb.append("sensor id: NO2, scalar, vector\n");
        copied.sort(Comparator.comparingLong(DataMessage::getScalarTimestamp));
        copied.forEach(m -> sb.append(m).append("\n"));
        scalarTimestampSorted.addAll(copied);

        sb.append("\n\n");

        // Printing the messages sorted by the vector values
        sb.append("Sorted by vector:\n");
        sb.append("sensor id: NO2, scalar, vector\n");
        copied.sort(Comparator.comparing(DataMessage::getVectorTimestamp));
        copied.forEach(m -> sb.append(m).append("\n"));
        vectorTimestampSorted.addAll(copied);

        logger.info(sb.toString());

    }
}
