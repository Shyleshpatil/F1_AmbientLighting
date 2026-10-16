package com.f1lighting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.JOptionPane;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

public class Main {

    private static final String CONFIG_FILE = "config.properties";
    private static final String API_BASE = "https://api.openf1.org/v1";

    private final String bleMacAddress;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String currentLeaderTeam = null;

    private static final Map<String, int[]> TEAM_COLORS = Map.of(
            "Ferrari", new int[]{255, 0, 0},
            "McLaren", new int[]{255, 135, 0},
            "Red Bull Racing", new int[]{0, 0, 255},
            "Mercedes", new int[]{0, 210, 190},
            "Aston Martin", new int[]{0, 102, 51},
            "Alpine", new int[]{255, 105, 180},
            "Williams", new int[]{0, 160, 222},
            "Haas F1 Team", new int[]{255, 255, 255},
            "Kick Sauber", new int[]{0, 225, 10},
            "Racing Bulls", new int[]{25, 50, 255}
    );

    public Main(String bleMacAddress) {
        this.bleMacAddress = bleMacAddress;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public static String getOrPromptMacAddress() {
        Properties props = new Properties();
        File file = new File(CONFIG_FILE);

        if (file.exists()) {
            try (InputStream input = new FileInputStream(file)) {
                props.load(input);
                String mac = props.getProperty("BLE_MAC_ADDRESS");
                if (mac != null && !mac.trim().isEmpty()) {
                    return mac.trim();
                }
            } catch (IOException e) {
                System.err.println("Could not read config: " + e.getMessage());
            }
        }

        // Display an input dialog if no configuration is found
        String macInput = JOptionPane.showInputDialog(
                null,
                "Enter your Lotus Lantern BLE MAC Address (e.g., 7D:E0:BB:72:B3:AD):",
                "F1 Ambient Lighting Setup",
                JOptionPane.QUESTION_MESSAGE
        );

        if (macInput == null || macInput.trim().isEmpty()) {
            System.err.println("No MAC address entered. Exiting...");
            System.exit(0);
        }

        String cleanedMac = macInput.trim();
        props.setProperty("BLE_MAC_ADDRESS", cleanedMac);
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            props.store(output, "F1 Ambient Lighting Configuration");
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }

        return cleanedMac;
    }

    public void startPolling() {
        System.out.println("==================================================");
        System.out.println(" F1 AMBIENT LIGHTING ENGINE ACTIVATED");
        System.out.println(" Target MAC: " + bleMacAddress);
        System.out.println(" Polling: " + API_BASE);
        System.out.println("==================================================");

        while (true) {
            try {
                pollAndUpdate();
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Cycle error: " + e.getMessage());
            }
        }
    }

    public String pollAndUpdate() {
        try {
            // 1. Fetch current leader driver number
            HttpRequest posRequest = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/position?session_key=latest&position=1"))
                    .GET().build();
            HttpResponse<String> posResponse = httpClient.send(posRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode posData = objectMapper.readTree(posResponse.body());

            if (posData.isEmpty() || !posData.isArray()) return null;

            int driverNumber = posData.get(posData.size() - 1).get("driver_number").asInt();

            // 2. Resolve team name
            HttpRequest driverRequest = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/drivers?session_key=latest&driver_number=" + driverNumber))
                    .GET().build();
            HttpResponse<String> driverResponse = httpClient.send(driverRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode driverData = objectMapper.readTree(driverResponse.body());

            if (driverData.isEmpty() || !driverData.isArray()) return null;

            String team = driverData.get(driverData.size() - 1).get("team_name").asText();

            if (!team.equals(currentLeaderTeam)) {
                System.out.printf("%n[LEAD CHANGE] %s has taken P1!%n", team);
                currentLeaderTeam = team;

                int[] rgb = TEAM_COLORS.getOrDefault(team, new int[]{255, 255, 255});
                triggerBle(rgb[0], rgb[1], rgb[2]);
            }
            return team;
        } catch (Exception e) {
            System.err.println("Cycle error: " + e.getMessage());
            return null;
        }
    }

    private void triggerBle(int r, int g, int b) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "python",
                    "light_ble.py",
                    bleMacAddress,
                    String.valueOf(r),
                    String.valueOf(g),
                    String.valueOf(b)
            );
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();
        } catch (Exception e) {
            System.err.println("Failed to run BLE script: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String mac = getOrPromptMacAddress();
        new Main(mac).startPolling();
    }
}