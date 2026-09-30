package com.example.pulsechat.exception;

public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException(Long roomId) {
        super("Chat room not found: " + roomId);
    }
}
