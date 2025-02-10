package com.example.hr_broadcast.MQTT;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MqttManager {
    private String mqttBrokerUri = "tcp://192.168.0.174:1883";
    private int port = 1883;
    private String mqttClientId = "AndroidClient";
    private String mqttLogin ;
    private String mqttPassword ;
    private static MqttManager instance;
    private MqttClient mqttClient;
    private static final Logger LOG = LoggerFactory.getLogger(MqttManager.class);
    private MqttConnectionListener connectionListener;

    // Set MQTT broker settings dynamically
    public void setMqttSettings(String brokerAddress, int port, String login, String password) {
        if (brokerAddress == null || brokerAddress.isEmpty()) {
            LOG.error("Broker address is null or empty.");
            return;
        }
        if (port <= 0) {
            LOG.error("Invalid port number: " + port);
            return;
        }

        this.mqttBrokerUri = "tcp://" + brokerAddress + ":" + port;


        LOG.debug("MQTT settings configured: Broker URI = " + mqttBrokerUri);
    }

    private MqttManager() {}

    public static synchronized MqttManager getInstance() {
        if (instance == null) {
            instance = new MqttManager();
        }
        return instance;
    }

    public void connect() {
        try {
            if (mqttBrokerUri == null || mqttBrokerUri.isEmpty()) {
                LOG.error("MQTT broker URI is not set.");
                return;
            }

            mqttClient = new MqttClient(mqttBrokerUri, mqttClientId, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            // options.setWill("pacient2/status", "offline".getBytes(), 0, true);
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);

           // if (mqttLogin != null && !mqttLogin.isEmpty() && mqttPassword != null) {
              //  options.setUserName(mqttLogin);
               // options.setPassword(mqttPassword.toCharArray());
           // }

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    LOG.error("MQTT connection lost", cause);
                    if (connectionListener != null) {
                        connectionListener.onConnectionStatusChanged(false, mqttBrokerUri, getPort());
                    }
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    LOG.debug("Message arrived from topic: " + topic + " - " + new String(message.getPayload()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    LOG.debug("Delivery complete for token: " + token);
                }
            });

            mqttClient.connect(options);

            LOG.debug("Connected to MQTT broker at: " + mqttBrokerUri);
            // String time = java.time.LocalDateTime.now().toString();
            // sendCustomMessage("pacient2/" + time ,"debug");
              sendCustomMessage("pacient2/status", "online");

            if (connectionListener != null) {
                connectionListener.onConnectionStatusChanged(true, mqttBrokerUri, getPort());
            }
        } catch (MqttException e) {
            LOG.error("Failed to connect to MQTT broker", e);
        }
    }

    public void setConnectionListener(MqttConnectionListener listener) {
        this.connectionListener = listener;
    }

    public void sendMessage(String topic, String payload) {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                MqttMessage message = new MqttMessage(payload.getBytes());
                message.setQos(0);
                mqttClient.publish(topic, message);
                LOG.debug("Published message to MQTT topic '{}': {}", topic, payload);
            } catch (MqttException e) {
                LOG.error("Error publishing message to MQTT broker", e);
            }
        } else {
            LOG.error("MQTT client is not connected!");
        }
    }

    public void sendHeartRate(int heartRate) {
        if (mqttClient != null && mqttClient.isConnected()) {
            String deviceId = android.os.Build.SERIAL;
            String topic = "devices/" + deviceId + "/health/heartRate";
            String payload = String.valueOf(heartRate);
            LOG.debug("Preparing to send heart rate: {}", heartRate);
            sendMessage(topic, payload);
        } else {
            LOG.error("MQTT client is not connected!");
        }
    }

    public void sendCustomMessage(String topic, String message) {
        sendMessage(topic, message);
    }

    private int getPort() {
        try {
            String[] parts = mqttBrokerUri.split(":");
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (Exception e) {
            LOG.error("Failed to parse port from URI: " + mqttBrokerUri);
            return -1;
        }
    }

    public interface MqttConnectionListener {
        void onConnectionStatusChanged(boolean connected, String serverUri, int port);
    }

    private void notifyConnectionStatus(boolean connected) {
        if (connectionListener != null) {
            connectionListener.onConnectionStatusChanged(connected, mqttBrokerUri, getPort());
        }
    }
    public void sendStats(String statType, String device ,int value) {
        if (mqttClient != null && mqttClient.isConnected()) {

            String deviceId = device;

            String topic = "devices/" + deviceId + "/health/" + statType; // Use the stat type (heartRate, spo2, etc.)
            String payload = String.valueOf(value);
            LOG.debug("Preparing to send {}: {}", statType, value);
            sendMessage(topic, payload);
        } else {
            LOG.error("MQTT client is not connected!");
        }
    }
    public void sendData(String deviceId, String dataType, String value) {
        if (mqttClient != null && mqttClient.isConnected()) {
            String topic = "devices/" + deviceId + "/" + dataType;
            LOG.debug("Sending data to MQTT: {} = {}", dataType, value);
            sendMessage(topic, value);
        } else {
            LOG.error("MQTT client is not connected!");
        }
    }

}
