package com.maua.check.mauacheck.rest.controller;

import com.maua.check.mauacheck.Service.RealTimePlateProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LicensePlateController {

    private final RealTimePlateProcessor realTimePlateProcessor;
    private String lastLicensePlate; // Armazena a última placa recebida

    @Autowired
    public LicensePlateController(RealTimePlateProcessor realTimePlateProcessor) {
        this.realTimePlateProcessor = realTimePlateProcessor;
    }

    @PostMapping("/placa")
    public ResponseEntity<Map<String, String>> plateProcess(@RequestBody String licensePlate) {
        this.lastLicensePlate = licensePlate; // Salva a placa recebida
        Map<String, String> response = new HashMap<>();
        response.put("licensePlate", licensePlate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/placa")
    public ResponseEntity<Map<String, String>> getPlate() {
        Map<String, String> response = new HashMap<>();
        String plate = realTimePlateProcessor.getLastPlate();
        response.put("licensePlate", plate != null ? plate : "");
        return ResponseEntity.ok(response);
    }
}