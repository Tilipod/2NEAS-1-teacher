package ru.tilipod.exception;

import lombok.Getter;

@Getter
public class SystemError extends RuntimeException {

    private final Integer taskId;

    public SystemError(String message, Integer taskId) {
        super(message);
        this.taskId = taskId;
    }
}
