package com.example.hr_broadcast;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hr_broadcast.MQTT.MqttManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private static final String DEVICE_NAME = "YourDeviceName";
    private static final UUID HEART_RATE_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB");
    private static final UUID HEART_RATE_MEASUREMENT_UUID = UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;

    private TextView deviceNameTextView;
    private TextView heartRateTextView;
    private HeartbeatAnimationView heartbeatAnimationView;
    private String deviceName;

    private List<String> users = new ArrayList<>();

    private RecyclerView recyclerView;
    private DeviceAdapter deviceAdapter;
    private List<Device> deviceList = new ArrayList<>();

    private MqttManager mqttManager;
    private boolean isScanning = false;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerViewDevices); // Use the class-level variable
        deviceAdapter = new DeviceAdapter(deviceList);

        recyclerView.setAdapter(deviceAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setBackgroundColor(Color.parseColor("#0C6B37"));

        Log.d("RecyclerView", "Setting adapter...");
        Log.d("RecyclerView", "Device list size: " + deviceList.size());
        deviceList.add(new Device("Huawei Band 6"));
        deviceList.add(new Device("Mi Band 7"));

        FirebaseApp.initializeApp(this);

        new Thread(() -> {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            auth.signInAnonymously()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            Log.d("FirebaseAuth", "Anonymous sign-in successful: " + user.getUid());
                        } else {
                            Log.e("FirebaseAuth", "Anonymous sign-in failed", task.getException());
                        }
                    });
        }).start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestPermissionsAndInitialize();
    }

    private void requestPermissionsAndInitialize() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), REQUEST_BLUETOOTH_PERMISSIONS);
        } else {
            initializeBluetoothAndScan();
        }
    }

    private void initializeBluetoothAndScan() {
        initializeBluetooth();
        startScan();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Log.e("Permissions", "Niektoré oprávnenia neboli udelené.");
                    return;
                }
            }
            initializeBluetoothAndScan();
        }
    }

    private void initializeBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                if (bluetoothLeScanner != null && !isScanning) {
                    isScanning = true;
                    startScan();
                }
            } else {
                Log.e("Bluetooth", "Bluetooth nie je zapnutý.");
            }
        }
    }

    private void startScan() {
        if (bluetoothLeScanner == null) return;

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(HEART_RATE_SERVICE_UUID)).build());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothLeScanner.startScan(filters, settings, scanCallback);
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device.getName() != null && !deviceNameExists(device.getName())) {
                Log.d("BluetoothScan", "Device found: " + device.getName());

                runOnUiThread(() -> {
                    deviceList.add(new Device(device.getName()));
                    deviceAdapter.notifyItemInserted(deviceList.size() - 1);  // Použi notifyItemInserted
                    Log.d("RecyclerView", "Device list size: " + deviceList.size());
                });

                connectToDevice(device);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("BluetoothScan", "Scan failed with error: " + errorCode);
        }
    };

    // Helper method to check if the device is already in the list
    private boolean deviceNameExists(String name) {
        for (Device device : deviceList) {
            if (device.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private BluetoothGattCharacteristic heartRateCharacteristic;

    private void connectToDevice(BluetoothDevice device) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            new Thread(() -> {
                bluetoothGatt = device.connectGatt(this, false, gattCallback);
            }).start();
            deviceName = device.getName();
        } else {
            Log.e("BluetoothConnect", "Missing BLUETOOTH_CONNECT permission");
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BluetoothGatt", "Connected, discovering services...");
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gatt.discoverServices();
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BluetoothGatt", "Disconnected");
                if (bluetoothGatt != null) {
                    bluetoothGatt.close();
                    bluetoothGatt = null;
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService heartRateService = gatt.getService(HEART_RATE_SERVICE_UUID);
                if (heartRateService != null) {
                    heartRateCharacteristic = heartRateService.getCharacteristic(HEART_RATE_MEASUREMENT_UUID);
                    if (heartRateCharacteristic != null) {
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            gatt.setCharacteristicNotification(heartRateCharacteristic, true);

                            // Set CCCD
                            BluetoothGattDescriptor descriptor = heartRateCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
                            if (descriptor != null) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(descriptor);
                                onCharacteristicChanged(gatt, heartRateCharacteristic);
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (HEART_RATE_MEASUREMENT_UUID.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    int heartRate = parseHeartRate(data);
                    Log.d("HeartRate", "Received heart rate data (RAW): " + bytesToHex(data));

                    runOnUiThread(() -> deviceAdapter.updateHeartRate(deviceName, heartRate));
                   // mqttManager.sendCustomMessage( "/Realtime" + deviceName, String.valueOf(heartRate));

                    Log.d("HeartRate", "Parsed heart rate: " + heartRate + " bpm");

                    String deviceId = deviceName;
                  //  MqttManager.getInstance().sendData(deviceId, "heartRate", String.valueOf(heartRate));

                    // ✅ Save to Firestore
                    saveHeartRateToFirestore(deviceId, heartRate);
                } else {
                    Log.w("HeartRate", "Received empty heart rate data");
                }
            }
        }
    };

    // Helper methods
    private int parseHeartRate(byte[] data) {
        if ((data[0] & 0x01) == 0) {
            return data[1] & 0xFF;  // 8-bit value
        } else {
            return ((data[2] & 0xFF) << 8) | (data[1] & 0xFF);  // 16-bit value
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    private void saveHeartRateToFirestore(String deviceId, int heartRate) {
        Map<String, Object> heartRateData = new HashMap<>();
        heartRateData.put("deviceId", deviceId);
        heartRateData.put("heartRate", heartRate);
        heartRateData.put("timestamp", System.currentTimeMillis());

        db.collection("heartRateData").add(heartRateData)
                .addOnSuccessListener(docRef -> Log.d("Firestore", "Heart rate saved: " + docRef.getId()))
                .addOnFailureListener(e -> Log.e("Firestore", "Error saving data", e));
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }
}
