package com.maua.check.mauacheck.rest.controller;

import com.maua.check.mauacheck.Service.GoogleVisionService;
import com.maua.check.mauacheck.Service.LicensePlateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class LicensePlateController {

    @Value("${gcp.bucket.name}")
    private String bucketName;

    private final GoogleVisionService googleVisionService;
    private final LicensePlateService licensePlateService;

    @Autowired
    public LicensePlateController(LicensePlateService licensePlateService, GoogleVisionService googleVisionService) {
        this.licensePlateService = licensePlateService;
        this.googleVisionService = googleVisionService;
    }

    //n funcionando
    @PostMapping("/check-and-store")
    public String checkAndStore(@RequestParam("file") MultipartFile file) {
        try {
            String jsonResponse = googleVisionService.analyzeImage(file, bucketName);

            String licensePlate = licensePlateService.extractLicensePlate(jsonResponse);

            if (licensePlate != null) {
                boolean exists = licensePlateService.checkIfLicensePlateExists(licensePlate);
                if (!exists) {
                    licensePlateService.storeResponseInBucket(jsonResponse, licensePlate);
                    return "License plate stored successfully.";
                } else {
                    return "License plate already exists.";
                }
            } else {
                return "License plate not found in the response.";
            }
        } catch (IOException e) {
            return "Error processing the image.";
        }
    }
}