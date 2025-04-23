package com.maua.check.mauacheck.rest.controller;

import com.maua.check.mauacheck.Service.RealTimePlateProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/video")
public class VideoProcessingController {

    private final RealTimePlateProcessor plateProcessor;

    @Autowired
    public VideoProcessingController(RealTimePlateProcessor plateProcessor) {
        this.plateProcessor = plateProcessor;
    }

    @PostMapping("/start")
    public String startVideo() {
        plateProcessor.startProcessing();
        return "Processamento de vídeo iniciado.";
    }

    @PostMapping("/stop")
    public String stopVideo() {
        plateProcessor.stopProcessing();
        return "Processamento de vídeo encerrado.";
    }
}
