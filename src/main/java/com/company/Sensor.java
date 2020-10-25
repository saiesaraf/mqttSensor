package com.company;

import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;


import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.net.InetAddress;
import java.net.NetworkInterface;

public class Sensor {
    private static String macAddress;
    private static final String OS_NAME = System.getProperty("os.name");
    public static void main(String[] args) throws InterruptedException {
        // 1. create the client
        final Mqtt5Client client = Mqtt5Client.builder()
                .identifier("sensor-" + getMacAddress()) // use a unique identifier
                .serverHost("broker.hivemq.com") // use the public HiveMQ broker
                .automaticReconnectWithDefaultConfig() // the client automatically reconnects
                .build();

        // 2. connect the client
        client.toBlocking().connectWith()
                .willPublish()
                .topic("home/will")
                .payload("sensor gone".getBytes())
                .applyWillPublish()
                .send();

        // 3. simulate periodic publishing of sensor data
        while (true) {
            client.toBlocking().publishWith()
                    .topic("home/brightness")
                    .payload(getBrightness())
                    .send();

            TimeUnit.MILLISECONDS.sleep(500);

            client.toBlocking().publishWith()
                    .topic("home/temperature")
                    .payload(getTemperature())
                    .send();
            String temp = new String(getTemperature());
            System.out.println("data sent" + temp);
            TimeUnit.MILLISECONDS.sleep(500);
        }
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

    private static byte[] getBrightness() {
        // simulate a brightness sensor with values between 1000lux and 10000lux
        final int brightness = ThreadLocalRandom.current().nextInt(1_000, 10_000);
        return (brightness + "lux").getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] getTemperature() {
        // simulate a temperature sensor with values between 20°C and 30°C
        final int temperature = ThreadLocalRandom.current().nextInt(20, 30);
        return (temperature + "°C").getBytes(StandardCharsets.UTF_8);
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
