package com.maua.check.mauacheck.rest.controller;

import com.maua.check.mauacheck.Service.GoogleVisionService;
import com.maua.check.mauacheck.Service.LicensePlateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@RestController
@RequestMapping("/vision")
public class GoogleVisionController {

    private final GoogleVisionService googleVisionService;
    private final LicensePlateService licensePlateService;

    @Value("${gcp.bucket.name}")
    private String bucketName;

    @Autowired
    public GoogleVisionController(GoogleVisionService googleVisionService, LicensePlateService licensePlateService) {
        this.googleVisionService = googleVisionService;
        this.licensePlateService = licensePlateService;
    }

    @GetMapping("/analyze-image")
    public ResponseEntity<String> analyzeImage(@RequestParam("file") MultipartFile file) {
        try {
            // Gera um UUID para substituir o nome do arquivo
            String uuid = UUID.randomUUID().toString();
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            String newFilename = uuid + extension; //arquivo que recebe um uuid novo, nao como enviado no request

            String result = googleVisionService.analyzeImage(file, bucketName, newFilename);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error processing image.");
        }
    }

    @PostMapping("/check-and-store")
    public ResponseEntity<String> checkAndStore(@RequestParam("file") MultipartFile file) {
        try {
            String fileHash = generateFileHash(file);

            boolean imageExists = licensePlateService.checkIfImageExists(fileHash);
            if (imageExists) {
                return ResponseEntity.badRequest().body("Error: Image already exists in the database.");
            }

            String uuid = UUID.randomUUID().toString();
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            String newFilename = uuid + extension; //arquivo que recebe um uuid novo, nao como enviado no request

            String jsonResponse = googleVisionService.analyzeImage(file, bucketName, newFilename);

            String licensePlate = licensePlateService.extractLicensePlate(jsonResponse);

            if (licensePlate != null) {
                boolean plateExists = licensePlateService.checkIfLicensePlateExists(licensePlate);

                if (!plateExists) {
                    licensePlateService.storeResponseInBucket(jsonResponse, licensePlate, fileHash);
                    return ResponseEntity.ok("License plate stored successfully.");
                } else {
                    return ResponseEntity.badRequest().body("License plate already exists.");
                }
            } else {
                return ResponseEntity.badRequest().body("License plate not found in the response.");
            }
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error processing the image.");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateFileHash(MultipartFile file) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] fileBytes = file.getBytes();
        byte[] digest = md.digest(fileBytes);

        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}