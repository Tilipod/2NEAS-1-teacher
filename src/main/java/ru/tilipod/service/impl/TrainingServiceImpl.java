package ru.tilipod.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.split.FileSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.BatchNormalization;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.tilipod.service.TrainingService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingServiceImpl implements TrainingService {

    //private final MultiLayerNetwork TEST_NETWORK = loadNetwork();
    private final MultiLayerNetwork TEST_NETWORK = createNetwork();
    private static final String PATH_TO_TRAINING = "C:/training/";
    private static final String PATH_TO_TEST = "C:/test/";
    private static final String PATH_TO_CLASSIFY = "C:/temp/";
    private static final String PATH_TO_MODEL = "C:/model";
    private static final int COUNT_OUTPUTS = 3;

    private static MultiLayerNetwork createNetwork() {
        // Входящие изображения размером 384x384
        MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
                .iterations(1)
                .seed(123)
                .learningRate(0.000001) // Норма обучения
                .regularization(true).l2(0.00005) // Наказывает за большие веса и предотвращает переобучение
                .weightInit(WeightInit.XAVIER) // Инициализация весов по гауссовому распределению
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT) // Алгоритм обучения
                .updater(new Nesterovs(0.9)) // Оптимизирует скорость обучения импульсом Нестерова
                .list()
                .layer(0, new ConvolutionLayer.Builder()
                        .nIn(3) // 3 входных канала (RGB)
                        .stride(2, 2) // Шаг в порядке (глубина, высота, ширина)
                        .nOut(12) // Кол-во выходных каналов
                        .weightInit(WeightInit.XAVIER)
                        .activation(Activation.RELU)
                        .build()
                )
                .layer(1, new BatchNormalization.Builder()
                        .nIn(12)
                        .nOut(12)
                        .build()
                )
                .layer(2, new ConvolutionLayer.Builder()
                        .nIn(12)
                        .stride(2, 2)
                        .nOut(24)
                        .weightInit(WeightInit.XAVIER)
                        .activation(Activation.RELU)
                        .build()
                )
                .layer(3, new BatchNormalization.Builder()
                        .nIn(24)
                        .nOut(24)
                        .build()
                )
                .layer(4, new ConvolutionLayer.Builder()
                        .nIn(24)
                        .stride(2, 2)
                        .nOut(48)
                        .weightInit(WeightInit.XAVIER)
                        .activation(Activation.RELU)
                        .build()
                )
                .layer(5, new BatchNormalization.Builder()
                        .nIn(48)
                        .nOut(48)
                        .build()
                )
                .layer(6, new DenseLayer.Builder() // Полносвязный слой
                        .activation(Activation.RELU)
                        .nOut(128)
                        .build()
                )
                .layer(7, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .activation(Activation.SOFTMAX)
                        .nOut(COUNT_OUTPUTS) // Кол-во классов изображений
                        .build()
                )
                .setInputType(InputType.convolutionalFlat(384, 384, 3))
                .pretrain(false)
                .backprop(true) // Алгоритм обратного распространения ошибки
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(config);

        net.init();

        return net;
    }

    private DataSetIterator createDatasetIter(String path) {
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

        DataSetIterator datasetIter = new RecordReaderDataSetIterator(datasetRR, 2, 1, COUNT_OUTPUTS);

        // Масштабируем пиксель от 0-255 до 0-1
        DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
        scaler.fit(datasetIter);
        datasetIter.setPreProcessor(scaler);
        datasetIter.reset();

        return datasetIter;
    }

    private File saveImageToTempFile(MultipartFile image) {
        File directory = new File(PATH_TO_CLASSIFY);

        try {
            if (directory.exists()) {
                FileUtils.cleanDirectory(directory);
            } else if (!directory.mkdir()) {
                log.warn("Не удалось создать временную директорию");
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.warn("Не удалось удалить временную директорию");
        }

        File file = new File(PATH_TO_CLASSIFY.concat(Objects.requireNonNull(image.getOriginalFilename())));
        try {
            if (!file.exists() && !file.createNewFile()) {
                log.warn("Невозможно создать файл для классификации изображения");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (OutputStream out = new FileOutputStream(file)) {
            IOUtils.copy(image.getInputStream(), out);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return directory;
    }

    private DataSetIterator createClassifyDatasetIter(MultipartFile image) {
        // Векторизация данных
        File file = saveImageToTempFile(image);
        FileSplit datasetSplit = new FileSplit(file, NativeImageLoader.ALLOWED_FORMATS, new Random(1));

        ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator();
        ImageRecordReader datasetRR = new ImageRecordReader(384, 384, 3, labelMaker);

        try {
            datasetRR.initialize(datasetSplit);
        } catch (IOException e) {
            e.printStackTrace();
        }

        DataSetIterator datasetIter = new RecordReaderDataSetIterator(datasetRR, 1, 1, COUNT_OUTPUTS);

        // Масштабируем пиксель от 0-255 до 0-1
        DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
        scaler.fit(datasetIter);
        datasetIter.setPreProcessor(scaler);
        datasetIter.reset();

        return datasetIter;
    }

    private MultiLayerNetwork loadNetwork() {
        try {
            return ModelSerializer.restoreMultiLayerNetwork(PATH_TO_MODEL);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Ошибка загрузки нейронной сети");
            return null;
        }
    }

    private void saveNetwork() {
        try {
            ModelSerializer.writeModel(TEST_NETWORK, PATH_TO_MODEL, true);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Ошибка сохранения нейронной сети");
        }
    }

    @Override
    @Async
    public void stepTraining(Integer taskId, Integer countEpoch, Boolean saveResult) {
        DataSetIterator trainIter = createDatasetIter(PATH_TO_TRAINING);
        DataSetIterator testIter = createDatasetIter(PATH_TO_TEST);

        log.info("Подготовка данных завершена, начинаем обучение нейросети");

        double precision = 0.0;
        for (int i = 1; i <= countEpoch; i++) {
            TEST_NETWORK.fit(trainIter);
            TEST_NETWORK.setLabels(null);
            log.info("Закончен {} шаг обучения нейронной сети. Проверка точности", i);

            Evaluation eval = TEST_NETWORK.evaluate(testIter);
            log.info(eval.stats());
            log.info("Изменение точности: {}", eval.precision() - precision);

            precision = eval.precision();
            trainIter.reset();
            testIter.reset();
        }

        if (saveResult) {
            saveNetwork();
            log.info("Нейросеть сохранена");
        }
    }

    @Override
    public String classifyImage(MultipartFile image) {
        DataSetIterator classifyIter = createClassifyDatasetIter(image);

        INDArray out = TEST_NETWORK.output(classifyIter.next().getFeatures());
        log.info("Результат: {}", out);

        return "Бяка";
    }
}
