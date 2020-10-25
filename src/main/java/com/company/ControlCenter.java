package com.company;

import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.net.InetAddress;
import java.net.NetworkInterface;

public class ControlCenter {
    private static String macAddress;
    private static final String OS_NAME = System.getProperty("os.name");
    public static void main(String[] args) {
        // 1. create the client
        final Mqtt5Client client = Mqtt5Client.builder()
                .identifier("controlcenter-" + getMacAddress()) // use a unique identifier
                .serverHost("broker.hivemq.com") // use the public HiveMQ broker
                .automaticReconnectWithDefaultConfig() // the client automatically reconnects
                .build();

        // 2. connect the client
        client.toBlocking().connectWith()
                .cleanStart(false)
                .sessionExpiryInterval(TimeUnit.HOURS.toSeconds(1)) // buffer messages
                .send();

        // 3. subscribe and consume messages
        client.toAsync().subscribeWith()
                .topicFilter("home/#")
                .callback(publish -> {
                    System.out.println("Received message on topic " + publish.getTopic() + ": " +
                            new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8));
                })
                .send();
    }

    private static String getMacAddress() {
        if (macAddress == null) {
            try {
                fetchMacAddress();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (macAddress == null) {
                macAddress = "unknown";
            }
        }
        return macAddress;

    }

    private static void fetchMacAddress() throws Exception {
        InetAddress ip = InetAddress.getLocalHost();
        NetworkInterface network = NetworkInterface.getByInetAddress(ip);
        if (network == null) {
            network = isLinux() ? NetworkInterface.getByName("eth0") : null;
        }
        byte[] mac = network.getHardwareAddress();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            sb.append(String.format("%02X%s", mac[i],
                    (i < mac.length - 1) ? "-" : ""));
        }
        macAddress = sb.toString();
    }

    public static boolean isLinux() {
        return OS_NAME.toLowerCase().contains("linux");
    }
}
