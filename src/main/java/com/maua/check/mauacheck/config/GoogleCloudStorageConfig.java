package com.maua.check.mauacheck.config;

import com.google.api.client.util.Lists;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.io.FileInputStream;
import java.io.IOException;

public class GoogleCloudStorageConfig {

    public static Storage getStorage() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream("src/main/resources/google-credentials.json"))
                .createScoped(Lists.newArrayList());
        return StorageOptions.newBuilder().setCredentials(credentials).build().getService();
    }
}