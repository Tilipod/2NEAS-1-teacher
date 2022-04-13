package ru.tilipod.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.tilipod.service.TrainingService;

@RestController
@RequestMapping("/training")
@RequiredArgsConstructor
@Api(description = "Контроллер для обучения нейронных сетей")
public class TrainingController {

    private final TrainingService trainingService;

    @PostMapping("/{taskId}/training/step")
    @ApiOperation(value = "Провести эпоху обучения нейронной сети")
    public ResponseEntity<Void> stepTraining(@PathVariable Integer taskId,
                                             @RequestParam Integer countEpoch,
                                             @RequestParam(defaultValue = "false") Boolean saveResult) {
        trainingService.stepTraining(taskId, countEpoch, saveResult);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/classify")
    @ApiOperation(value = "Определить класс изображения")
    public ResponseEntity<String> classifyImage(@RequestParam MultipartFile image) {
        return ResponseEntity.ok(trainingService.classifyImage(image));
    }
}
