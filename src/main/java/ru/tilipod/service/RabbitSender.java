package ru.tilipod.service;

import ru.tilipod.amqp.message.TeacherResultErrorMessage;
import ru.tilipod.amqp.message.TeacherResultSuccessMessage;

public interface RabbitSender {

    void sendErrorToScheduler(TeacherResultErrorMessage model);

    void sendSuccessToScheduler(TeacherResultSuccessMessage model);

}
