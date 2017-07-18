package codeu.chat.server;

import java.io.*;
import java.util.TimerTask;
import java.util.ArrayDeque;
import java.util.Timer;

/**
 * Created by Lysander on 6/23/17.
 */
class LogQueue {
    private File transactionLog;
    private ArrayDeque<String> transactions;
    private Timer timer;
    private BufferedWriter out;

    //checks to see if there is a transaction waiting in the queue and writes it to the file
    private TimerTask writeTransaction;

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
                while (!transactions.isEmpty()) {
                    try {
                        out.write(transactions.poll() + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        timer.scheduleAtFixedRate(writeTransaction, 1, 100);
    }

    ArrayDeque<String> getTransactions() {
        return transactions;
    }
}
