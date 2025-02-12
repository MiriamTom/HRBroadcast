package com.example.hr_broadcast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<List<Device>> deviceList = new MutableLiveData<>();

    public LiveData<List<Device>> getDeviceList() {
        return deviceList;
    }

    public void updateDeviceList(List<Device> devices) {
        deviceList.setValue(devices);
    }

    public void addDevice(Device device) {
        List<Device> currentList = deviceList.getValue();
        if (currentList != null) {
            currentList.add(device);
            deviceList.setValue(currentList);
        }
    }

    public void updateHeartRate(String deviceName, int heartRate) {
        List<Device> currentList = deviceList.getValue();
        if (currentList != null) {
            for (Device device : currentList) {
                if (device.getName().equals(deviceName)) {
                    device.setHeartRate(heartRate);
                    deviceList.setValue(currentList);  // Update the LiveData
                    break;
                }
            }
        }
    }

    public void clearDeviceList() {
        List<Device> currentList = deviceList.getValue();
        if (currentList != null) {
            currentList.clear();
            deviceList.setValue(currentList);
        }
    }
}
