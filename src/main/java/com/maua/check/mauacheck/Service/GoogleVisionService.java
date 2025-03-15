package com.maua.check.mauacheck.Service;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleVisionService {

    private final ImageAnnotatorClient visionClient;

    public GoogleVisionService(ImageAnnotatorClient visionClient) {
        this.visionClient = visionClient;
    }

    public String extractTextFromImage(MultipartFile file) throws IOException {
        ByteString imgBytes = ByteString.copyFrom(file.getBytes());
        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feature = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feature)
                .setImage(img)
                .build();

        List<AnnotateImageRequest> requests = new ArrayList<>();
        requests.add(request);

        BatchAnnotateImagesResponse response = visionClient.batchAnnotateImages(requests);
        List<AnnotateImageResponse> responses = response.getResponsesList();

        StringBuilder extractedText = new StringBuilder();
        for (AnnotateImageResponse res : responses) {
            if (res.hasError()) {
                return "Erro ao processar a imagem: " + res.getError().getMessage();
            }
            extractedText.append(res.getTextAnnotationsList().getFirst().getDescription());
        }
        return extractedText.toString();
    }
}
