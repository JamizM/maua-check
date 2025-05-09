package com.maua.check.mauacheck.Service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.maua.check.mauacheck.domain.entity.Aluno;
import lombok.extern.slf4j.Slf4j;
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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class RealTimePlateProcessor {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss" , Locale.getDefault());

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

                        LocalDateTime data = LocalDateTime.now();
                        String horarioISO = data.atZone(ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        OffsetDateTime offsetDateTime = OffsetDateTime.parse(horarioISO);
                        String horario = offsetDateTime.format(formatter);

                        String uniqueFileName = licensePlate + "_" + data.format(formatter) + ".jpg";

                        // Verifica se a placa já existe no bucket
                        if (licensePlateService.checkIfLicensePlateExists(licensePlate)) {
                            licensePlateService.storeResponseInBucket(licensePlate);

                            String imageUrl = googleVisionService.uploadImageToGCS(
                                    new MockMultipartFile("file", uniqueFileName, "image/jpeg", imageBytes),
                                    bucketName
                            );

                            Aluno Student = new Aluno();
                            Student.setHorario(horario);
                            Student.setImageUrl(imageUrl);

                            Map<String, Object> dataStudent = new HashMap<>();
                            dataStudent.put("horario", Student.getHorario());
                            dataStudent.put("imageUrl", Student.getImageUrl());

                            //Altera os campos horario e imageUrl no firestore database
                            Firestore dbFirestore = FirestoreClient.getFirestore(); //variavel que acessa o firebase
                            var docRef = dbFirestore.collection("alunos").document(licensePlate); //faz a procura para achar placa do caro
                            ApiFuture<DocumentSnapshot> future = docRef.get();

                            try{
                                DocumentSnapshot document = future.get();
                                if (document.exists()){
                                    docRef.update(dataStudent);
                                }
                                else{
                                    System.out.println("Documento não encontrado para a placa: " + licensePlate);
                                }
                            } catch (Exception  e){
                                e.printStackTrace();
                            }

                            String sanitizedFileName = licensePlate + "-" + data.format(formatter).replace("/", "-") + ".jpg";

                            googleVisionService.uploadImageToGCS(
                                    new MockMultipartFile("file", sanitizedFileName, "image/jpeg", imageBytes),
                                    bucketName
                            );

                            System.out.println("Placa detectada e processada: " + licensePlate);
                        } else {
                            System.out.println("Placa não encontrada na base de dados: " + licensePlate);
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