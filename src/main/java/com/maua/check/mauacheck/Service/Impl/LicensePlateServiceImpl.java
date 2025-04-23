package com.maua.check.mauacheck.Service.Impl;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;

import com.maua.check.mauacheck.Service.LicensePlateService;
import com.maua.check.mauacheck.exception.RegraDeNegocioException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.StringReader;

@Service
public class LicensePlateServiceImpl implements LicensePlateService {

    @Value("${gcp.bucket.name}")
    private String bucketName;

    private final Storage storage;

    public LicensePlateServiceImpl(Storage storage) {
        this.storage = storage;
        Gson gson = new GsonBuilder().create();
    }

    public String extractLicensePlate(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            System.out.println("The input JSON response is null or empty");
        }

        try {
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{ \"response\": [");

            String[] lines = jsonResponse.split("\n");
            for (int i = 0; i < lines.length; i++) {
                jsonBuilder.append("\"").append(lines[i].replace("\"", "\\\"")).append("\"");
                if (i < lines.length - 1) {
                    jsonBuilder.append(",");
                }
            }

            jsonBuilder.append("] }");

            JsonReader reader = new JsonReader(new StringReader(jsonBuilder.toString()));
            reader.setLenient(true);

            JsonElement jsonElement = JsonParser.parseReader(reader);
            if (!jsonElement.isJsonObject()) {
                System.out.println("Response is not a JSON object");
            }

            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonArray responseArray = jsonObject.getAsJsonArray("response");

            for (JsonElement element : responseArray) {
                String line = element.getAsString().trim();
                if (line.matches("^[A-Z]{3}\\s?-?\\d{4}$") || line.matches("^[A-Z]{3}\\d[A-Z]\\d{2}$")) { //verificao da placa do carro
                    return line;
                }
            }
        } catch (JsonSyntaxException | IllegalStateException e) {
            System.err.println("Error parsing JSON response: " + e.getMessage());
            throw new IllegalArgumentException("Invalid JSON response format", e);
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
    public void storeResponseInBucket(String licensePlate, String fileHash) {
        if (checkIfLicensePlateExists(licensePlate)) {
            System.out.println("License plate already exists in the bucket");
            return;
        }

        Bucket bucket = storage.get(this.bucketName);
        String fileName = licensePlate + "_" + fileHash + ".txt";
        bucket.create(fileName, licensePlate.getBytes());
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