package com.team4.dto.socket;

/**
 * Lớp cơ sở (Envelope) cho tất cả các thông điệp truyền qua Socket
 */
public class SocketMessageDTO<T> {
    private String command; // Loại lệnh/thông báo
    private T payload;      // Dữ liệu thực tế đi kèm

    public SocketMessageDTO() {
    }

    public SocketMessageDTO(String command, T payload) {
        this.command = command;
        this.payload = payload;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "SocketMessageDTO{" +
                "command='" + command + '\'' +
                ", payload=" + payload +
                '}';
    }
}
