package ru.tilipod.amqp.message;

import lombok.Data;

@Data
public abstract class TeacherResultMessage {

    private Integer taskId;
}
