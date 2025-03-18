package com.maua.check.mauacheck.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class GoogleCloudConfig {

    @Value("classpath:google-credentials.json")
    private Resource credentialsPath;

    @Bean
    public ImageAnnotatorClient imageAnnotatorClient() throws IOException {
        try (InputStream credentialsStream = credentialsPath.getInputStream()) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(() -> credentials)
                    .build();
            return ImageAnnotatorClient.create(settings);
        }
    }

    @Bean
    public Storage storage() throws IOException {
        try (InputStream credentialsStream = credentialsPath.getInputStream()) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
            return StorageOptions.newBuilder().setCredentials(credentials).build().getService();
        }
    }
}