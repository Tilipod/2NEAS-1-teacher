package ru.tilipod.amqp.message;

import lombok.Data;

@Data
public class TeacherResultErrorMessage extends TeacherResultMessage {

    private String className;

    private String message;

    public static TeacherResultErrorMessage createMessage(Integer taskId, Exception e) {
        TeacherResultErrorMessage model = new TeacherResultErrorMessage();

        model.setMessage(e.getMessage());
        model.setClassName(e.getClass().getName());
        model.setTaskId(taskId);

        return model;
    }
}
