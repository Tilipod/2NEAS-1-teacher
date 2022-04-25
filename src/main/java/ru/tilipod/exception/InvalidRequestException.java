package ru.tilipod.exception;

import lombok.Getter;

@Getter
public class InvalidRequestException extends RuntimeException {

    private final Integer taskId;

    public InvalidRequestException(String message, Integer taskId) {
        super(message);
        this.taskId = taskId;
    }
}
