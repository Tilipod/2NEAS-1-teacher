package ru.tilipod.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.rl4j.learning.IHistoryProcessor;
import org.deeplearning4j.rl4j.learning.sync.qlearning.discrete.QLearningDiscreteConv;
import org.deeplearning4j.rl4j.util.DataManager;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.tilipod.amqp.message.TeacherResultErrorMessage;
import ru.tilipod.amqp.message.TeacherResultSuccessMessage;
import ru.tilipod.controller.dto.TrainingDto;
import ru.tilipod.reforcement.DQNWithAllowNetwork;
import ru.tilipod.reforcement.TableMdp;
import ru.tilipod.service.NetworkStorageHelper;
import ru.tilipod.service.RabbitSender;
import ru.tilipod.service.TrainingService;
import ru.tilipod.util.Constants;
import ru.tilipod.util.RandomUtil;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class Rl4jTrainingService implements TrainingService {

    private final RabbitSender rabbitSender;

    private final NetworkStorageHelper networkStorageHelper;

    private QLearningDiscreteConv<?> prepareQLearningNetwork(TrainingDto trainingDto) throws IOException {
        MultiLayerNetwork net = networkStorageHelper.loadNetwork(trainingDto.getPathToModel(), trainingDto.getTaskId());
        return new QLearningDiscreteConv<>(new TableMdp(RandomUtil.randomArray3(trainingDto.getCountStates(), trainingDto.getCountStates(),
                Constants.DEFAULT_COUNT_ACTIONS), net.conf().getIterationCount(), trainingDto.getCountStates()),
                new DQNWithAllowNetwork<>(net), new IHistoryProcessor.Configuration(), trainingDto.getReforcementConf(), new DataManager());
    }

    @Override
    @Async
    public void stepTraining(TrainingDto trainingDto) {
        try {
            log.info("Начинаем обучение нейронной сети по задаче {}", trainingDto.getTaskId());

            QLearningDiscreteConv<?> qNet = prepareQLearningNetwork(trainingDto);
            DataSetIterator testIter = networkStorageHelper.prepareTestDataset(trainingDto);

            log.info("Подготовка данных по задаче {} завершена, начинаем обучение", trainingDto.getTaskId());

            qNet.train();

            log.info("Завершено обучение нейронной сети по задаче {}. Начинаем оценку точности", trainingDto.getTaskId());

            MultiLayerNetwork net = ((DQNWithAllowNetwork<?>) qNet.getCurrentDQN()).getNetwork();
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
