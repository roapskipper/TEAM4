package com.team4.network;

import java.io.Serializable;

public class NetworkMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String command;
    private String data;
    private long timestamp;

    public NetworkMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public NetworkMessage(String command, String data) {
        this.command = command;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
