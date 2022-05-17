package ru.tilipod.amqp.message;

import lombok.Data;

@Data
public class TeacherResultSuccessMessage extends TeacherResultMessage {

    private String pathTo;

    private Double precision;

    public static TeacherResultSuccessMessage createMessage(Integer taskId, String pathTo, Double precision) {
        TeacherResultSuccessMessage model = new TeacherResultSuccessMessage();

        model.setTaskId(taskId);
        model.setPathTo(pathTo);
        model.setPrecision(precision);

        return model;
    }
}
