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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;

import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hr_broadcast.MQTT.MqttManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private static final UUID HEART_RATE_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB");
    private static final UUID HEART_RATE_MEASUREMENT_UUID = UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;

    private String deviceName;
    private List<Device> deviceList = new ArrayList<>();
    private DeviceAdapter deviceAdapter;

    private MqttManager mqttManager;
    private boolean isScanning = false;

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth;
    private TextView statusTextView;
    private String userEmail = "Not logged in";
    private Toolbar toolbar;

    private NavController navController;
    private BottomNavigationView bottomNavigationView;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    SharedViewModel viewModel;
    private Set<String> addedDeviceNames = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewModel = new ViewModelProvider(this).get(SharedViewModel.class);

        // Initialize RecyclerView
      //  recyclerView = findViewById(R.id.recyclerViewDevices);
        deviceAdapter = new DeviceAdapter(deviceList);
      //  recyclerView.setAdapter(deviceAdapter);

        viewModel.getDeviceList().observe(this, devices -> {
            addedDeviceNames.clear();  // Clear HashSet before repopulating
            List<Device> newDeviceList = new ArrayList<>(deviceList); // Create a copy of the list

            for (Device device : devices) {
                if (device.getName() != null && !addedDeviceNames.contains(device.getName())) {
                    newDeviceList.add(device);
                    addedDeviceNames.add(device.getName());
                }
            }

            deviceList.clear();
            deviceList.addAll(newDeviceList);
            deviceAdapter.notifyDataSetChanged();
        });


        // Initialize NavController for navigation handling
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        } else {
            Log.e("MainActivity", "NavHostFragment is null");
        }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.openDrawer, R.string.closeDrawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        if (navController != null) {
            NavigationUI.setupWithNavController(navigationView, navController);
        }

        bottomNavigationView = findViewById(R.id.bottomNavigationView);


        NavigationUI.setupWithNavController(bottomNavigationView, navController);
        mAuth = FirebaseAuth.getInstance();
        FirebaseApp.initializeApp(this);

        db = FirebaseFirestore.getInstance();

        statusTextView = findViewById(R.id.statusTextView);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
        }

        updateStatusText();

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.mainFragment) {
            navController.navigate(R.id.mainFragment);
            Log.d("MenuItem", "Selected: " + item.getItemId());

            return true;
        } else if (item.getItemId() == R.id.loginFragment) {
            navController.navigate(R.id.loginFragment);
            Log.d("MenuItem", "Selected: " + item.getItemId());

            return true;
        } else if (item.getItemId() == R.id.userProfileFragment) {
            navController.navigate(R.id.userProfileFragment);
            Log.d("MenuItem", "Selected: " + item.getItemId());

            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d("MainActivity", "User is logged in: " + currentUser.getEmail());
        } else {
            startActivity(new Intent(this, LoginFragment.class));
            finish();
        }
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
                runOnUiThread(() -> addDeviceIfNotExists(device));
                viewModel.updateDeviceList(deviceList);

                connectToDevice(device);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("BluetoothScan", "Scan failed with error: " + errorCode);
        }
    };
    public void updateDeviceList(List<Device> devices) {
        if (deviceAdapter != null) {

            deviceList.addAll(devices);
            deviceAdapter.notifyDataSetChanged();
        }
    }
    private boolean deviceNameExists(String name) {
        if (name == null) return false; // Ak je null, považujeme ho za neexistujúci
        return addedDeviceNames.contains(name);
    }

    @SuppressLint("MissingPermission")
    public void addDeviceIfNotExists(BluetoothDevice device) {
        String deviceName = device.getName();
        if (deviceName != null && !addedDeviceNames.contains(deviceName)) {
            Log.d("DeviceList", "Pridávam nové zariadenie: " + deviceName);
            Device newDevice = new Device(device); // Vytvor nový objekt Device
            deviceList.add(newDevice);
            addedDeviceNames.add(deviceName); // Ulož názov zariadenia do HashSet-u
            deviceAdapter.notifyDataSetChanged();
        } else {
            Log.d("DeviceList", "Zariadenie už existuje: " + deviceName);
        }
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

                    runOnUiThread(() -> viewModel.updateHeartRate(deviceName, heartRate));
                    runOnUiThread(() -> deviceAdapter.updateHeartRate(deviceName, heartRate));

                    // mqttManager.sendCustomMessage( "/Realtime" + deviceName, String.valueOf(heartRate));

                    Log.d("HeartRate", "Parsed heart rate: " + heartRate + " bpm");

                    String deviceId = deviceName;
                  //  MqttManager.getInstance().sendData(deviceId, "heartRate", String.valueOf(heartRate));

                    // Save to Firestore
                    saveHeartRateToFirestore(deviceId, heartRate);
                } else {
                    Log.w("HeartRate", "Received empty heart rate data");
                }
            }
        }
    };

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
        heartRateData.put("name", deviceId);
        heartRateData.put("heartRate", heartRate);
        heartRateData.put("timestamp", System.currentTimeMillis());

        db.collection("devices").add(heartRateData)
                .addOnSuccessListener(docRef -> Log.d("Firestore", "Heart rate saved: " + docRef.getId()))
                .addOnFailureListener(e -> Log.e("Firestore", "Error saving data", e));
    }


    private void updateStatusText() {
        String status = "User status: " + userEmail + "\n";
        statusTextView.setText(status);
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
