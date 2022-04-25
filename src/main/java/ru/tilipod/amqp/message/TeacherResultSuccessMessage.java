package ru.tilipod.amqp.message;

import lombok.Data;

@Data
public class TeacherResultSuccessMessage extends TeacherResultMessage {

    private String pathTo;

    public static TeacherResultSuccessMessage createMessage(Integer taskId, String pathTo) {
        TeacherResultSuccessMessage model = new TeacherResultSuccessMessage();

        model.setTaskId(taskId);
        model.setPathTo(pathTo);

        return model;
    }
}
