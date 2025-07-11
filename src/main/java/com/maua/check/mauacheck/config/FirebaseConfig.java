package com.maua.check.mauacheck.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("classpath:firebase-credentials.json")
    private Resource credentialsPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        try (InputStream credentialsStream = credentialsPath.getInputStream()) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            return FirebaseApp.initializeApp(options);
        }
    }
}