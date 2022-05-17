package ru.tilipod.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.split.FileSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.tilipod.amqp.message.TeacherResultErrorMessage;
import ru.tilipod.amqp.message.TeacherResultSuccessMessage;
import ru.tilipod.controller.dto.TrainingDto;
import ru.tilipod.exception.InvalidRequestException;
import ru.tilipod.exception.SystemError;
import ru.tilipod.service.RabbitSender;
import ru.tilipod.service.TrainingService;

import java.io.File;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingServiceImpl implements TrainingService {

    private final RabbitSender rabbitSender;

    private SplitTestAndTrain createImageDatasetIter(String path, Double percentTrain, Integer countOutput) {
        // Векторизация данных
        File file = new File(path);
        FileSplit datasetSplit = new FileSplit(file, NativeImageLoader.ALLOWED_FORMATS);

        // Маркируем классы изображений по названиям директорий. Приводим к размеру 1000x1000, 3 канала (RGB)
        ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator();
        ImageRecordReader datasetRR = new ImageRecordReader(384, 384, 3, labelMaker);

        try {
            datasetRR.initialize(datasetSplit);
        } catch (IOException e) {
            e.printStackTrace();
        }

        DataSetIterator datasetIter = new RecordReaderDataSetIterator(datasetRR, 2, 1, countOutput);

        // Масштабируем пиксель от 0-255 до 0-1
        DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
        scaler.fit(datasetIter);
        datasetIter.setPreProcessor(scaler);
        datasetIter.reset();

        return datasetIter.next().splitTestAndTrain(percentTrain);
    }

    private MultiLayerNetwork loadNetwork(String pathToModel, Integer taskId) {
        try {
            return ModelSerializer.restoreMultiLayerNetwork(pathToModel);
        } catch (IOException e) {
            e.printStackTrace();
            throw new SystemError(String.format("Ошибка загрузки нейронной сети. Задача с id = %d", taskId), taskId);
        }
    }

    private void saveNetwork(MultiLayerNetwork net, String pathToSave, Integer taskId) {
        try {
            ModelSerializer.writeModel(net, pathToSave, true);
        } catch (IOException e) {
            e.printStackTrace();
            throw new SystemError(String.format("Ошибка сохранения нейронной сети. Задача с id = %d", taskId), taskId);
        }
    }

    private SplitTestAndTrain prepareDataset(TrainingDto trainingDto) {
        return switch (trainingDto.getDatasetType()) {
            case IMAGE -> createImageDatasetIter(trainingDto.getPathToDataset(), trainingDto.getPercentTrain(), trainingDto.getCountOutput());
            default -> throw new InvalidRequestException(String.format("Неподдерживаемый тип данных для обучения: %s. Задача с id = %d",
                    trainingDto.getDatasetType(), trainingDto.getTaskId()), trainingDto.getTaskId());
        };
    }

    @Override
    @Async
    public void stepTraining(TrainingDto trainingDto) {
        try {
            log.info("Начинаем обучение нейронной сети по задаче {}", trainingDto.getTaskId());

            MultiLayerNetwork net = loadNetwork(trainingDto.getPathToModel(), trainingDto.getTaskId());
            SplitTestAndTrain splitTestAndTrain = prepareDataset(trainingDto);

            log.info("Подготовка данных по задаче {} завершена, начинаем обучение", trainingDto.getTaskId());

            for (int i = 1; i <= trainingDto.getCountEpoch(); i++) {
                net.fit(splitTestAndTrain.getTrain());
                log.info("Закончен {} шаг обучения нейронной сети по задаче {}", i, trainingDto.getTaskId());
            }

            log.info("Завершено обучение нейронной сети по задаче {}. Начинаем оценку точности", trainingDto.getTaskId());

            INDArray output = net.output(splitTestAndTrain.getTest().getFeatureMatrix());
            Evaluation eval = new Evaluation(3);
            eval.eval(splitTestAndTrain.getTest().getLabels(), output);

            log.info("Текущая точность обучения сети по задаче {} составляет {}%", trainingDto.getTaskId(), eval.precision() * 100);

            saveNetwork(net, trainingDto.getPathToModel(), trainingDto.getTaskId());
            rabbitSender.sendSuccessToScheduler(TeacherResultSuccessMessage.createMessage(trainingDto.getTaskId(),
                    trainingDto.getPathToModel(), eval.precision()));
        } catch (Exception e) {
            log.error("Ошибка обучения сети. Задача: {}, ошибка: {}", trainingDto.getTaskId(), e.getMessage());
            rabbitSender.sendErrorToScheduler(TeacherResultErrorMessage.createMessage(trainingDto.getTaskId(), e));
        }
    }
}
