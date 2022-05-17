package ru.tilipod.controller.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import ru.tilipod.controller.dto.enums.DatasetTypeEnum;

@Data
public class TrainingDto {

    @ApiModelProperty(value = "ID задачи", required = true)
    private Integer taskId;

    @ApiModelProperty(value = "Тип данных для обучения сети", required = true)
    private DatasetTypeEnum datasetType;

    @ApiModelProperty(value = "Процент данных для тренировки (остальный процент идет для тестирования)", required = true)
    private Double percentTrain;

    @ApiModelProperty(value = "Кол-во эпох для обучения", required = true)
    private Integer countEpoch;

    @ApiModelProperty(value = "Кол-во выходов нейронной сети", required = true)
    private Integer countOutput;

    @ApiModelProperty(value = "Путь к датасетам для обучения", required = true)
    private String pathToDataset;

    @ApiModelProperty(value = "Путь к модели сети", required = true)
    private String pathToModel;
}
