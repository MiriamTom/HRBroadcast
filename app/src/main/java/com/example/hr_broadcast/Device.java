package com.example.hr_broadcast;

public class Device {
    private String user;
    private String name;
    private int heartRate;

    public Device(String name) {
        this.name = name;
        this.heartRate = 0;  // Default heart rate value
    }
    public Device(String name,String user) {
        this.user = user;
        this.name = name;
        this.heartRate = 0;  // Default heart rate value
    }

    public String getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }
}
