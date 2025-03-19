package com.maua.check.mauacheck.rest.controller;

import com.maua.check.mauacheck.Service.GoogleVisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/vision")
public class GoogleVisionController {

    private final GoogleVisionService googleVisionService;

    @Value("${gcp.bucket.name}")
    private String bucketName;

    @Autowired
    public GoogleVisionController(GoogleVisionService googleVisionService) {
        this.googleVisionService = googleVisionService;
    }

    @GetMapping("/analyze-image") //tem que ser GetMapping
    public ResponseEntity<String> analyzeImage(@RequestParam("file") MultipartFile file) {
        try {
            String bucketName = this.bucketName;
            String result = googleVisionService.analyzeImage(file, bucketName);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error processing image.");
        }
    }
}