package com.rideshare.driver.dto;

import com.rideshare.driver.model.VehicleType;

public class VehicleResponse {
    private String model;
    private VehicleType vehicleType;
    private String licensePlate;
    private Integer capacity;
    private String color;

    public VehicleResponse() {
    }

    public VehicleResponse(String model, VehicleType vehicleType, String licensePlate, Integer capacity, String color) {
        this.model = model;
        this.vehicleType = vehicleType;
        this.licensePlate = licensePlate;
        this.capacity = capacity;
        this.color = color;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
