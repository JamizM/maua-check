package com.maua.check.mauacheck.Service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.maua.check.mauacheck.Service.Impl.GoogleVisionServiceImpl;
import com.maua.check.mauacheck.Service.Impl.LicensePlateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

class RealTimePlateProcessorTest {

    private RealTimePlateProcessor plateProcessor;
    private GoogleVisionServiceImpl googleVisionService;
    private LicensePlateServiceImpl licensePlateService;
    private Firestore firestore;

    @BeforeEach
    void setUp() {
        googleVisionService = mock(GoogleVisionServiceImpl.class);
        licensePlateService = mock(LicensePlateServiceImpl.class);
        firestore = mock(Firestore.class);

        plateProcessor = new RealTimePlateProcessor(googleVisionService, licensePlateService, firestore);
    }

    @Test
    void testStartProcessing_WhenLicensePlateExistsInFirestore_ShouldBlockProcessing() throws Exception {
        String licensePlate = "ABC1234";
        String detectedText = "Placa: ABC1234";

        when(googleVisionService.analyzeImage(any(), anyString(), anyString())).thenReturn(detectedText);

        when(licensePlateService.extractLicensePlate(detectedText)).thenReturn(licensePlate);

        var documentSnapshot = mock(DocumentSnapshot.class);
        when(documentSnapshot.exists()).thenReturn(true);

        ApiFuture<DocumentSnapshot> future = (ApiFuture<DocumentSnapshot>) CompletableFuture.completedFuture(documentSnapshot);
        when(firestore.collection("alunos").document(licensePlate).get()).thenReturn(future);

        MockMultipartFile mockFile = new MockMultipartFile("file", "temp.jpg", "image/jpeg", new byte[0]);
        when(googleVisionService.uploadImageToGCS(any(), anyString())).thenReturn("mockImageUrl");

        plateProcessor.startProcessing();

        verify(licensePlateService, never()).storeResponseInBucket(licensePlate);
        verify(googleVisionService, never()).uploadImageToGCS(any(), anyString());
    }
}