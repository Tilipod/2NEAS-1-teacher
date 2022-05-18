package ru.tilipod.service;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import ru.tilipod.controller.dto.TrainingDto;

public interface NetworkStorageHelper {

    DataSetIterator prepareTestDataset(TrainingDto trainingDto);

    DataSetIterator prepareTrainDataset(TrainingDto trainingDto);

    void saveNetwork(MultiLayerNetwork net, String pathToSave, Integer taskId);

    MultiLayerNetwork loadNetwork(String pathToModel, Integer taskId);
}
