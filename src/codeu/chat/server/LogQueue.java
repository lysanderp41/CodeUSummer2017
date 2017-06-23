package codeu.chat.server;



import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.TimerTask;
import java.util.ArrayDeque;
import java.util.Timer;

/**
 * Created by Lysander on 6/23/17.
 */
public class LogQueue {
    File transactionLog;
    ArrayDeque<String> transactions;
    Timer timer;
    FileOutputStream outputStream;
    PrintWriter out;

    //checks to see if there is a transaction waiting in the queue and writes it to the file
    TimerTask writeTransaction;

    public LogQueue () {
        transactions = new ArrayDeque<>();
        transactionLog = new File("transaction_log.txt");

        try {
            out = new PrintWriter(transactionLog);
        } catch (FileNotFoundException e) {
            System.out.println("FILE NOT FOUND");
        }
        timer = new Timer();

        writeTransaction = new TimerTask() {
            @Override
            public void run() {
                if (!transactions.isEmpty()) {
                    out.write(transactions.poll());
                    out.flush();
                }
            }
        };

        timer.scheduleAtFixedRate(writeTransaction, 1, 100);
    }

}
