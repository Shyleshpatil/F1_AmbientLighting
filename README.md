# 🏎️ F1 Ambient Lighting Controller

Bring the thrill of Formula 1 race day into your room! This application tracks live F1 telemetry and automatically changes the color of your ambient LED lighting to match the livery of the team currently in 1st Place (P1).

**New:** Now with real-time Track Flag support! When the FIA issues a Safety Car, Virtual Safety Car, or Red Flag, your room will pulse Yellow or Red to match track conditions before seamlessly transitioning back to the leader's color when the track goes green.

## 🛠️ Resources & Technologies Used
- **Node.js:** Hosts the local timing bridge, tapping directly into the official FIA websocket with zero latency or rate limits.
- **Java (11/17):** Core application logic, API polling, and state management.
- **Python (3.x):** Hardware interfacing and animation sequencing using the asynchronous `bleak` library.
- **f1-livetiming-api:** Open-source Node.js bridge replacing paid third-party APIs.
- **Maven:** Dependency management and standalone JAR packaging.
- **Hardware:** Any generic Bluetooth LED strip utilizing the "Lotus Lantern" or "ELK-BLEDOM" protocol.

## 🧠 How It Works
1. **The Bridge:** A local Node.js server continuously listens to the official FIA live stream, exposing real-time REST endpoints (`/api/status` and `/api/leaderboard`).
2. **The Poller:** A lightweight Java background process queries the local bridge every 5 seconds.
3. **The Logic:** Java first checks for track flags (Safety Car, Red Flag, etc.). If the track is clear, it fetches the P1 driver and maps their team to a predefined RGB hex value.
4. **The Trigger:** Java spins up the Python script, passing the target MAC address, the animation mode (`SOLID`, `BLINK_YELLOW`, or `BLINK_RED`), and the RGB values. Python connects to the LED strip via Bluetooth Low Energy (BLE), executes the animation sequence, sets the final color, and disconnects seamlessly.

---

## 🚀 Setup & Deployment Guide

Because hardware configurations differ, the final **Deployment Folder** is not included in this repository. Follow these steps to build and deploy the application locally on your machine.

### Prerequisites
- Node.js installed (to run the live timing bridge)
- Java Development Kit (JDK 11 or 17)
- Python 3.x installed with the `bleak` library (`pip install bleak`)
- Maven installed (or use your IDE's built-in Maven tools)

### Step 1: Start the Live F1 Bridge
Since third-party APIs now restrict live telemetry, we use a local bridge to pull data straight from the source. Open a terminal and run:

```bash
git clone [https://github.com/julesr0y/f1-livetiming-api.git](https://github.com/julesr0y/f1-livetiming-api.git)
cd f1-livetiming-api
npm install
npm start