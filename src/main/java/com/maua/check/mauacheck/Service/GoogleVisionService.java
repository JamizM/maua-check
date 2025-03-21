package com.maua.check.mauacheck.Service;

import com.google.cloud.vision.v1.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public interface GoogleVisionService {

    String uploadImageToGCS(MultipartFile file, String bucketName) throws IOException;

    String analyzeImage(MultipartFile file, String bucketName, String newFileName) throws IOException;

    String uploadImageToGCS(MultipartFile file, String bucketName, String newFilename) throws IOException;

    String filterResponse(List<AnnotateImageResponse> responses);


}