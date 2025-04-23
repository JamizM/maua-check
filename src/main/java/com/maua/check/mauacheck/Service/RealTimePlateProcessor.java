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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Service
public class RealTimePlateProcessor {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HH");

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

                    MultipartFile multipartFile = new MockMultipartFile(
                            "file", "temp.jpg", "image/jpeg", imageBytes
                    );

                    // Análise da imagem para detectar texto
                    String detectedText = googleVisionService.analyzeImage(multipartFile, bucketName, "temp.jpg");
                    String licensePlate = licensePlateService.extractLicensePlate(detectedText);

                    if (licensePlate != null) {
                        String uniqueFileName = licensePlate + "_" + LocalDateTime.now().format(formatter) + ".jpg";

                        // Verifica se a placa já existe no bucket
                        if (!licensePlateService.checkIfLicensePlateExists(licensePlate)) {
                            licensePlateService.storeResponseInBucket(licensePlate);

                            googleVisionService.uploadImageToGCS(
                                    new MockMultipartFile("file", uniqueFileName, "image/jpeg", imageBytes),
                                    bucketName
                            );

                            System.out.println("Placa detectada e processada: " + licensePlate);
                        } else {
                            System.out.println("Placa duplicada detectada: " + licensePlate);
                        }
                    }

                    Thread.sleep(1000); // Ajuste o intervalo entre capturas, se necessário
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