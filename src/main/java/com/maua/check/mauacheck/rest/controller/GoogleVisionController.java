package com.maua.check.mauacheck.rest.controller;

import com.maua.check.mauacheck.Service.GoogleVisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class GoogleVisionController {

    private final GoogleVisionService googleVisionService;

    @Autowired
    public GoogleVisionController(GoogleVisionService googleVisionService) {
        this.googleVisionService = googleVisionService;
    }

    @PostMapping("/extract-text")
    public ResponseEntity<String> extractText(@RequestParam("file") MultipartFile file) {
        try {
            String extractedText = googleVisionService.extractTextFromImage(file);
            return ResponseEntity.ok(extractedText);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Erro ao processar a imagem.");
        }
    }
}
