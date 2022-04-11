package ru.tilipod.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tilipod.service.TrainingService;

@RestController
@RequestMapping("/training")
@RequiredArgsConstructor
@Api(description = "Контроллер для обучения нейронных сетей")
public class TrainingController {

    private final TrainingService trainingService;

    @PostMapping("/{taskId}/training/step")
    @ApiOperation(value = "Провести эпоху обучения нейронной сети")
    public ResponseEntity<Void> stepTraining(@PathVariable Integer taskId) {
        trainingService.stepTraining(taskId);
        return ResponseEntity.noContent().build();
    }
}
