package codeu.chat.server;

import javafx.concurrent.Task;

import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.TimerTask;

/**
 * Created by Lysander on 6/23/17.
 */
public class LogQueue extends ArrayDeque<String> {
    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            while (true) {
            }
        }
    };

    private void writeMessage() {

    }

    private void writeConversation() {

    }

    private void writeUser() {

    }

    private void writeInterest() {

    }
}
