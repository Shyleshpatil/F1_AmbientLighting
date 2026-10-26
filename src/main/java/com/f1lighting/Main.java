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

    private final String bleMacAddress;
    private final String apiBaseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private String currentLeaderTeam = null;
    private String previousTrackStatus = "1"; // "1" is the FIA code for Track Clear (Green)

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

    public Main(String bleMacAddress, String apiBaseUrl) {
        this.bleMacAddress = bleMacAddress;
        this.apiBaseUrl = apiBaseUrl;
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
        System.out.println(" Polling: " + apiBaseUrl);
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
            // 1. FETCH LIVE TRACK STATUS
            String trackStatus = "1";
            try {
                HttpRequest statusReq = HttpRequest.newBuilder()
                        .uri(URI.create(apiBaseUrl + "/api/status"))
                        .GET().build();
                HttpResponse<String> statusRes = httpClient.send(statusReq, HttpResponse.BodyHandlers.ofString());
                JsonNode statusNode = objectMapper.readTree(statusRes.body());
                if (statusNode.has("Status")) {
                    trackStatus = statusNode.get("Status").asText();
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not fetch track status.");
            }

            // 2. FETCH CURRENT LEADER
            HttpRequest leaderReq = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/api/leaderboard"))
                    .GET().build();
            HttpResponse<String> leaderRes = httpClient.send(leaderReq, HttpResponse.BodyHandlers.ofString());
            JsonNode data = objectMapper.readTree(leaderRes.body());

            if (data.isEmpty() || !data.has("Lines")) return null;

            JsonNode lines = data.get("Lines");
            String p1Team = null;

            java.util.Iterator<Map.Entry<String, JsonNode>> fields = lines.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode driverData = field.getValue();

                if (driverData.has("Position") && driverData.get("Position").asText().equals("1")) {
                    p1Team = driverData.has("TeamName") ? driverData.get("TeamName").asText() : null;
                    break;
                }
            }

            if (p1Team == null) return null;

            // 3. DETERMINE FLAGS AND TRIGGERS
            boolean statusChanged = !trackStatus.equals(previousTrackStatus);
            boolean leaderChanged = !p1Team.equalsIgnoreCase(currentLeaderTeam);

            String mode = "SOLID";
            if (trackStatus.equals("5")) {
                mode = "BLINK_RED";
            } else if (trackStatus.equals("2") || trackStatus.equals("4") || trackStatus.equals("6")) {
                mode = "BLINK_YELLOW";
            }

            if (statusChanged && !trackStatus.equals("1")) {
                System.out.println("\n[TRACK ALERT] FIA Flag Status changed to: " + trackStatus);
            }

            // Fire the lights if we need to blink, or if the flag cleared, or if the leader changed
            if (mode.startsWith("BLINK") || statusChanged || leaderChanged) {
                if (leaderChanged) {
                    System.out.printf("%n[LEAD CHANGE] %s has taken P1!%n", p1Team);
                }

                currentLeaderTeam = p1Team;
                previousTrackStatus = trackStatus;

                int[] rgb = TEAM_COLORS.getOrDefault(currentLeaderTeam, new int[]{255, 255, 255});
                triggerBle(mode, rgb[0], rgb[1], rgb[2]);
            }

            return p1Team;
        } catch (Exception e) {
            System.err.println("Polling error: " + e.getMessage());
            return null;
        }
    }

    private void triggerBle(String mode, int r, int g, int b) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "python",
                    "light_ble.py",
                    bleMacAddress,
                    mode,
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
        // Pointing to your local Node.js bridge
        new Main(mac, "http://localhost:3000").startPolling();
    }
}