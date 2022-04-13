package ru.tilipod.service;

import org.springframework.web.multipart.MultipartFile;

public interface TrainingService {

    void stepTraining(Integer taskId, Integer countEpoch, Boolean saveResult);

    String classifyImage(MultipartFile image);
}
