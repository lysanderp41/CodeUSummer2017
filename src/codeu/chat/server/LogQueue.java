package codeu.chat.server;



import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;

import java.io.*;
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
    BufferedWriter out;

    //checks to see if there is a transaction waiting in the queue and writes it to the file
    TimerTask writeTransaction;

    public LogQueue () {
        transactions = new ArrayDeque<>();
        transactionLog = new File("transaction_log.txt");

        try {
            if(!transactionLog.exists())
                transactionLog.createNewFile();
            //ensures the writer is appending and not overwriting what is in the file
            out = new BufferedWriter(new FileWriter(transactionLog,true));
        } catch (IOException e) {
            e.printStackTrace();
        }
        timer = new Timer();
        writeTransaction = new TimerTask() {
            @Override
            public void run() {
                if (!transactions.isEmpty()) {
                    try {
                        out.write(transactions.poll() + "\n");
                        out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        timer.scheduleAtFixedRate(writeTransaction, 1, 100);
    }
}
