package com.example.hr_broadcast;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
    private List<Device> deviceList;

    public DeviceAdapter(List<Device> deviceList) {
        this.deviceList = deviceList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Použitie správneho layoutu pre CardView
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Device device = deviceList.get(position);
        Log.d("DeviceAdapter", "Binding device: " + device.getName());

        holder.deviceNameTextView.setText(device.getName());
        holder.heartRateTextView.setText("Heart Rate: " + device.getHeartRate() + " bpm");
        holder.heartbeatAnimationView.setHeartRate(device.getHeartRate());
    }
    public void updateDeviceList(List<Device> newDevices) {
        this.deviceList = newDevices;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        HeartbeatAnimationView heartbeatAnimationView;
        TextView deviceNameTextView, heartRateTextView;
        CardView cardView;  // Pridanie CardView do ViewHolderu

        public ViewHolder(View itemView) {
            super(itemView);
            deviceNameTextView = itemView.findViewById(R.id.deviceName);
            heartRateTextView = itemView.findViewById(R.id.heartRate);
            cardView = itemView.findViewById(R.id.cardView); // Získanie referencie na CardView
            heartbeatAnimationView = itemView.findViewById(R.id.heartbeatAnimation); // Nájdenie animácie

        }
    }

    public void updateHeartRate(String deviceName, int heartRate) {
        for (int i = 0; i < deviceList.size(); i++) {
            if (deviceList.get(i).getName().equals(deviceName)) {
                deviceList.get(i).setHeartRate(heartRate);  // Update heart rate of the device
                notifyItemChanged(i);  // Notify adapter to refresh the item at position i
                Log.d("DeviceAdapter", "Updated heart rate for " + deviceName + ": " + heartRate);
                break;
            }
        }
    }


    public void addDevice(Device device) {
        deviceList.add(device);
        notifyItemInserted(deviceList.size() - 1);
    }

    public void removeDevice(String deviceName) {
        for (int i = 0; i < deviceList.size(); i++) {
            if (deviceList.get(i).getName().equals(deviceName)) {
                deviceList.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }
}
