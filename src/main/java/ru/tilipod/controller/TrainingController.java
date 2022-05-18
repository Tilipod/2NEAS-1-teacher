package ru.tilipod.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.tilipod.controller.dto.TrainingDto;
import ru.tilipod.service.TrainingService;

@RestController
@RequestMapping("/training")
@Api(description = "Контроллер для обучения нейронных сетей")
public class TrainingController {

    @Autowired
    @Qualifier("dl4jTrainingService")
    private TrainingService dl4jTrainingService;

    @Autowired
    @Qualifier("rl4jTrainingService")
    private TrainingService rl4jTrainingService;

    @PostMapping("/training/step")
    @ApiOperation(value = "Провести обучение нейронной сети")
    public ResponseEntity<Void> stepTraining(@RequestBody TrainingDto trainingDto,
                                             @RequestParam(defaultValue = "false") Boolean withMentoring) {
        if (withMentoring) {
            rl4jTrainingService.stepTraining(trainingDto);
        } else {
            dl4jTrainingService.stepTraining(trainingDto);
        }
        return ResponseEntity.noContent().build();
    }
}
