package codeu.chat.common;

import codeu.chat.util.Time;

/**
 * Created by Lysander on 5/20/17.
 */
public final class ServerInfo {
    public final Time startTime;
    public ServerInfo() {
        this.startTime = Time.now();
    }
    public ServerInfo(Time startTime) {
        this.startTime = startTime;
    }
}
