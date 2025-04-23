package com.maua.check.mauacheck.Service.Impl;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.vision.v1.*;
import com.maua.check.mauacheck.Service.GoogleVisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleVisionServiceImpl implements GoogleVisionService {

    private final ImageAnnotatorClient visionClient;
    private final Storage storage;

    @Autowired
    public GoogleVisionServiceImpl(ImageAnnotatorClient visionClient, Storage storage) {
        this.visionClient = visionClient;
        this.storage = storage;
    }

    @Override
    public String uploadImageToGCS(MultipartFile file, String bucketName) throws IOException {
        String blobName = file.getOriginalFilename();
        assert blobName != null;
        BlobId blobId = BlobId.of(bucketName, blobName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(file.getContentType()).build();
        storage.create(blobInfo, file.getBytes());
        return String.format("gs://%s/%s", bucketName, blobName);
    }

    @Override
    public String analyzeImage(MultipartFile file, String bucketName, String newFileName) throws IOException {
        String imageUri = uploadImageToGCS(file, bucketName, newFileName);

        ImageSource imgSource = ImageSource.newBuilder().setImageUri(imageUri).build();
        Image img = Image.newBuilder().setSource(imgSource).build();

        List<Feature> features = new ArrayList<>();
        features.add(Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).setMaxResults(1).build());
        features.add(Feature.newBuilder().setType(Feature.Type.OBJECT_LOCALIZATION).setMaxResults(1).build()); //pode fazer a detecção do automovel
        features.add(Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).setMaxResults(1).setModel("builtin/latest").build());

        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addAllFeatures(features)
                .setImage(img)
                .build();

        List<AnnotateImageRequest> requests = new ArrayList<>();
        requests.add(request);

        BatchAnnotateImagesResponse response = visionClient.batchAnnotateImages(requests);
        List<AnnotateImageResponse> responses = response.getResponsesList();

        return filterResponse(responses);
    }

    @Override
    public String uploadImageToGCS(MultipartFile file, String bucketName, String newFilename) throws IOException {
        BlobId blobId = BlobId.of(bucketName, newFilename);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(file.getContentType()).build();
        storage.create(blobInfo, file.getBytes());
        return String.format("gs://%s/%s", bucketName, newFilename);
    }

    @Override
    public String filterResponse(List<AnnotateImageResponse> responses) {
        StringBuilder result = new StringBuilder();

        for (AnnotateImageResponse res : responses) {
            if (res.hasError()) {
                result.append("Error: ").append(res.getError().getMessage()).append("\n");
                continue;
            }

            for (EntityAnnotation annotation : res.getLabelAnnotationsList()) {
                result.append("Label: ").append(annotation.getDescription())
                        .append(" (Score: ").append(annotation.getScore()).append(")\n");
            }

            for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
                result.append("Text: ").append(annotation.getDescription()).append("\n");
            }
        }

        return result.toString();
    }
}