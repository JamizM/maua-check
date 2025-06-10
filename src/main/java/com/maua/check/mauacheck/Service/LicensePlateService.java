package com.maua.check.mauacheck.Service;

public interface LicensePlateService {

    String extractLicensePlate(String jsonResponse);

    boolean checkIfLicensePlateExists(String licensePlate);

    void storeResponseInBucket(String licensePlate);

    boolean checkIfImageExists(String checkIfImageExists);

    void sendLicensePlateEndPoint(String licensePlate);
}
