package com.maua.check.mauacheck.Service;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayOutputStream;

import org.springframework.beans.factory.annotation.Value;
import com.maua.check.mauacheck.Service.Impl.GoogleVisionServiceImpl;
import com.maua.check.mauacheck.Service.Impl.LicensePlateServiceImpl;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.Frame;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

@Service
public class RealTimePlateProcessor {

    private final Set<String> processedHashes = new HashSet<>();
    private final GoogleVisionServiceImpl googleVisionService;
    private final LicensePlateServiceImpl licensePlateService;
    private boolean running = false;
    private boolean plateDetected = false;

    @Value("${gcp.bucket.name}")
    private String bucketName;

    public RealTimePlateProcessor(GoogleVisionServiceImpl googleVisionService, LicensePlateServiceImpl licensePlateService) {
        this.googleVisionService = googleVisionService;
        this.licensePlateService = licensePlateService;
    }

    public void startProcessing() {
        if (running) return;

        running = true;

        new Thread(() -> {
            try (OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0)) {
                grabber.start();
                Java2DFrameConverter converter = new Java2DFrameConverter();

                while (running) {
                    Frame frame = grabber.grab();
                    if (frame == null) break;

                    BufferedImage image = converter.convert(frame);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "jpg", baos);
                    byte[] imageBytes = baos.toByteArray();

                    String uniqueFileName = "frame_" + System.currentTimeMillis() + ".jpg";

                    MultipartFile multipartFile = new MockMultipartFile(
                            "file", uniqueFileName, "image/jpeg", imageBytes
                    );

                    // Análise da imagem para detectar texto
                    if (!plateDetected) {
                        String detectedText = googleVisionService.analyzeImage(multipartFile, bucketName, uniqueFileName);
                        String licensePlate = licensePlateService.extractLicensePlate(detectedText);

                        if (licensePlate != null) {
                            System.out.println("Placa detectada: " + licensePlate);

                            // Envia a imagem e o texto ao bucket
                            licensePlateService.storeResponseInBucket(licensePlate, uniqueFileName);

                            googleVisionService.uploadImageToGCS(multipartFile, bucketName);

                            plateDetected = true;
                        }
                    }

                    Thread.sleep(500); // Ajuste o intervalo entre capturas, se necessário
                }

                grabber.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void resetDetection() {
        plateDetected = false;
    }

    public void stopProcessing() {
        running = false;
    }
}