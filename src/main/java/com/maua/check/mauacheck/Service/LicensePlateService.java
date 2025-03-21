package com.maua.check.mauacheck.Service;

public interface LicensePlateService {

    String extractLicensePlate(String jsonResponse);

    boolean checkIfLicensePlateExists(String licensePlate);

    void storeResponseInBucket(String jsonResponse, String licensePlate, String fileHash);

    boolean checkIfImageExists(String fileHash);
}
