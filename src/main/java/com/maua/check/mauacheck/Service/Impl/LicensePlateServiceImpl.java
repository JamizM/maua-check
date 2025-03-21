package com.maua.check.mauacheck.Service.Impl;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.maua.check.mauacheck.Service.LicensePlateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LicensePlateServiceImpl implements LicensePlateService {

    @Value("${gcp.bucket.name}")
    private String bucketName;

    private final Storage storage;

    public LicensePlateServiceImpl(Storage storage) {
        this.storage = storage;
    }

    @Override
    public String extractLicensePlate(String jsonResponse) {
        JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
        JsonArray textAnnotations = jsonObject.getAsJsonArray("text_annotations");

        for (JsonElement element : textAnnotations) {
            JsonObject annotation = element.getAsJsonObject();
            String description = annotation.get("description").getAsString();
            if (description.matches("^[A-Z0-9]{6,8}$")) { //aqui faz a verificação da placa do carro
                return description;
            }
        }
        return null;
    }

    @Override
    public boolean checkIfLicensePlateExists(String licensePlate) {
        Bucket bucket = storage.get(this.bucketName);

        for (Blob blob : bucket.list().iterateAll()) {
            if (blob.getName().contains(licensePlate)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void storeResponseInBucket(String jsonResponse, String licensePlate, String fileHash) {
        if (checkIfLicensePlateExists(licensePlate)) {
            throw new IllegalStateException("License plate already exists in the bucket");
        }

        Bucket bucket = storage.get(this.bucketName);
        String fileName = licensePlate + "_" + fileHash + ".json";
        bucket.create(fileName, jsonResponse.getBytes());
    }

    @Override
    public boolean checkIfImageExists(String fileHash) {
        Bucket bucket = storage.get(this.bucketName);

        for (Blob blob : bucket.list().iterateAll()) {
            if (blob.getName().contains(fileHash)) {
                return true;
            }
        }
        return false;
    }
}