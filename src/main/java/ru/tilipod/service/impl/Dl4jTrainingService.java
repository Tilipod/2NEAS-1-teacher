package ru.tilipod.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.tilipod.amqp.message.TeacherResultErrorMessage;
import ru.tilipod.amqp.message.TeacherResultSuccessMessage;
import ru.tilipod.controller.dto.TrainingDto;
import ru.tilipod.service.NetworkStorageHelper;
import ru.tilipod.service.RabbitSender;
import ru.tilipod.service.TrainingService;

@Slf4j
@Service
@RequiredArgsConstructor
public class Dl4jTrainingService implements TrainingService {

    private final RabbitSender rabbitSender;

    private final NetworkStorageHelper networkStorageHelper;

    @Override
    @Async
    public void stepTraining(TrainingDto trainingDto) {
        try {
            log.info("Начинаем обучение нейронной сети по задаче {}", trainingDto.getTaskId());

            MultiLayerNetwork net = networkStorageHelper.loadNetwork(trainingDto.getPathToModel(), trainingDto.getTaskId());
            DataSetIterator trainIter = networkStorageHelper.prepareTrainDataset(trainingDto);
            DataSetIterator testIter = networkStorageHelper.prepareTestDataset(trainingDto);

            log.info("Подготовка данных по задаче {} завершена, начинаем обучение", trainingDto.getTaskId());

            for (int i = 1; i <= trainingDto.getCountEpoch(); i++) {
                net.fit(trainIter);
                trainIter.reset();
                log.info("Закончен {} шаг обучения нейронной сети по задаче {}", i, trainingDto.getTaskId());
            }

            log.info("Завершено обучение нейронной сети по задаче {}. Начинаем оценку точности", trainingDto.getTaskId());

            Evaluation eval = net.evaluate(testIter);

            log.info("Текущая точность обучения сети по задаче {} составляет {}%", trainingDto.getTaskId(), eval.precision() * 100);

            networkStorageHelper.saveNetwork(net, trainingDto.getPathToModel(), trainingDto.getTaskId());
            rabbitSender.sendSuccessToScheduler(TeacherResultSuccessMessage.createMessage(trainingDto.getTaskId(),
                    trainingDto.getPathToModel(), eval.precision()));
        } catch (Exception e) {
            log.error("Ошибка обучения сети. Задача: {}, ошибка: {}", trainingDto.getTaskId(), e.getMessage());
            rabbitSender.sendErrorToScheduler(TeacherResultErrorMessage.createMessage(trainingDto.getTaskId(), e));
        }
    }
}
