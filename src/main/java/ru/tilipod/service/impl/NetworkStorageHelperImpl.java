package ru.tilipod.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.split.FileSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.springframework.stereotype.Service;
import ru.tilipod.controller.dto.TrainingDto;
import ru.tilipod.exception.InvalidRequestException;
import ru.tilipod.exception.SystemError;
import ru.tilipod.service.NetworkStorageHelper;
import ru.tilipod.util.Constants;

import java.io.File;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkStorageHelperImpl implements NetworkStorageHelper {

    private DataSetIterator createImageDatasetIter(String path, Integer countOutput) {
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

        return datasetIter;
    }

    @Override
    public MultiLayerNetwork loadNetwork(String pathToModel, Integer taskId) {
        try {
            return ModelSerializer.restoreMultiLayerNetwork(pathToModel);
        } catch (IOException e) {
            e.printStackTrace();
            throw new SystemError(String.format("Ошибка загрузки нейронной сети. Задача с id = %d", taskId), taskId);
        }
    }

    @Override
    public void saveNetwork(MultiLayerNetwork net, String pathToSave, Integer taskId) {
        try {
            ModelSerializer.writeModel(net, pathToSave, true);
        } catch (IOException e) {
            e.printStackTrace();
            throw new SystemError(String.format("Ошибка сохранения нейронной сети. Задача с id = %d", taskId), taskId);
        }
    }

    @Override
    public DataSetIterator prepareTrainDataset(TrainingDto trainingDto) {
        return switch (trainingDto.getDatasetType()) {
            case IMAGE -> createImageDatasetIter(trainingDto.getPathToDataset().concat(Constants.PATH_TO_TRAIN), trainingDto.getCountOutput());
            default -> throw new InvalidRequestException(String.format("Неподдерживаемый тип данных для обучения: %s. Задача с id = %d",
                    trainingDto.getDatasetType(), trainingDto.getTaskId()), trainingDto.getTaskId());
        };
    }

    @Override
    public DataSetIterator prepareTestDataset(TrainingDto trainingDto) {
        return switch (trainingDto.getDatasetType()) {
            case IMAGE -> createImageDatasetIter(trainingDto.getPathToDataset().concat(Constants.PATH_TO_TEST), trainingDto.getCountOutput());
            default -> throw new InvalidRequestException(String.format("Неподдерживаемый тип данных для обучения: %s. Задача с id = %d",
                    trainingDto.getDatasetType(), trainingDto.getTaskId()), trainingDto.getTaskId());
        };
    }
}
