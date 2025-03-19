package com.maua.check.mauacheck.Service.Impl;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
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

    @Override
    public String extractLicensePlate(String jsonResponse) {
        JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
        JsonArray textAnnotations = jsonObject.getAsJsonArray("text_annotations");

        for (JsonElement element : textAnnotations) {
            JsonObject annotation = element.getAsJsonObject();
            String description = annotation.get("description").getAsString();
            if (description.matches("^[A-Z0-9]{6,8}$")) {
                return description;
            }
        }
        return null;
    }

    @Override
    public boolean checkIfLicensePlateExists(String licensePlate) {
        Storage storage = StorageOptions.getDefaultInstance().getService();
        Bucket bucket = storage.get(this.bucketName);

        for (Blob blob : bucket.list().iterateAll()) {
            if (blob.getName().contains(licensePlate)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void storeResponseInBucket(String jsonResponse, String licensePlate) {
        Storage storage = StorageOptions.getDefaultInstance().getService();
        Bucket bucket = storage.get(this.bucketName);
        bucket.create(licensePlate + ".json", jsonResponse.getBytes());
    }
}