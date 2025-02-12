package com.example.hr_broadcast;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainFragment extends Fragment {
    private SharedViewModel viewModel;
    private RecyclerView recyclerView;
    private DeviceAdapter deviceAdapter;
    private List<Device> deviceList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewDevices);
        deviceAdapter = new DeviceAdapter(deviceList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(deviceAdapter);

        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        viewModel.getDeviceList().observe(getViewLifecycleOwner(), devices -> {
            if (devices != null) {
                for (Device device : devices) {
                    if (!deviceListContains(deviceList, device)) {
                        deviceList.add(device);
                    }
                }
            }
            deviceAdapter.notifyDataSetChanged();
        });

        Device testDevice = new Device("Test Device");
        testDevice.setHeartRate(75);
        viewModel.addDevice(testDevice);
    }
    private boolean deviceListContains(List<Device> list, Device newDevice) {
        for (Device device : list) {
            if (device.getName().equals(newDevice.getName())) {
                return true;
            }
        }
        return false;
    }
}

