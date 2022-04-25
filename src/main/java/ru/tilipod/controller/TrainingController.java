package ru.tilipod.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tilipod.controller.dto.TrainingDto;
import ru.tilipod.service.TrainingService;

@RestController
@RequestMapping("/training")
@RequiredArgsConstructor
@Api(description = "Контроллер для обучения нейронных сетей")
public class TrainingController {

    private final TrainingService trainingService;

    @PostMapping("/training/step")
    @ApiOperation(value = "Провести обучение нейронной сети")
    public ResponseEntity<Void> stepTraining(@RequestBody TrainingDto trainingDto) {
        trainingService.stepTraining(trainingDto);
        return ResponseEntity.noContent().build();
    }
}
