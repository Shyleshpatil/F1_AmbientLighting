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
git clone https://github.com/julesr0y/f1-livetiming-api.git
cd f1-livetiming-api
npm install
npm start
```
*Leave this terminal window running in the background while the race is live.*

### Step 2: Build the Java Application
Clone this repository to your local machine. Open your terminal in the project root and run:

```bash
mvn clean package -DskipTests
```
> **Note on Tests:** We use the `-DskipTests` flag here. Because the application defaults to pulling live internet data, running the standard `mvn clean package` might cause local mock-server tests to fail if real-world live data overrides the expected test data.

### Step 3: Create the Deployment Folder
Create a new, empty folder anywhere on your computer (e.g., `C:\F1_Lighting\`). 

Copy the following two files into this new folder:
1. `target/ambient-tracker-1.0-SNAPSHOT.jar` (Generated in Step 2)
2. `send_ble_color.py` (Found in the root of the repository)

### Step 4: Create the Batch Launcher
Inside your deployment folder, create a new text file named `F1_Lighting.bat`. Add the following code and save it:

```bat
@echo off
title F1 Ambient Lighting Controller
java -jar ambient-tracker-1.0-SNAPSHOT.jar
pause
```

Your final deployment folder should look exactly like this:
```text
F1_Lighting/
├── ambient-tracker-1.0-SNAPSHOT.jar
├── F1_Lighting.bat
└── send_ble_color.py
```

### Step 5: First Launch
Double-click `F1_Lighting.bat`. On the very first run, a pop-up window will ask for your LED strip's Bluetooth MAC address (e.g., `7D:E0:BB:72:B3:AD`). Enter it, and the app will generate a `config.properties` file so you never have to type it again. 

---

## ⚠️ Troubleshooting & Known Issues

### 1. Error: `'java' is not recognized as an internal or external command`
**The Problem:** You double-clicked the `.bat` file, but Windows doesn't know where Java is installed because it's not in your system environment variables. (This is common if you rely on your IDE's bundled Java).
**The Fix:** Edit your `F1_Lighting.bat` file to point directly to your Java installation path. 
1. Find your JDK path (e.g., `C:\Users\YourName\.jdks\corretto-17.0.8\bin\java.exe`).
2. Wrap the path in quotes and replace the word `java`. 

*Updated F1_Lighting.bat:*
```bat
@echo off
title F1 Ambient Lighting Controller
"C:\Users\YourName\.jdks\corretto-17.0.8\bin\java.exe" -jar ambient-tracker-1.0-SNAPSHOT.jar
pause
```

### 2. Changing the MAC Address
**The Problem:** You bought a new LED strip or entered the wrong MAC address on the first launch.
**The Fix:** Simply delete the `config.properties` file inside your deployment folder. The next time you run `F1_Lighting.bat`, the setup prompt will reappear.

### 3. Python Unicode/Emoji Crash during Bluetooth Scanning
If you are writing a custom BLE scanner script and Windows throws a `UnicodeEncodeError: 'charmap' codec can't encode character`, Windows is struggling to print emojis to the standard command prompt. 
**The Fix:** Either remove the emoji from your `print()` statement, or run the script enforcing UTF-8: `python -X utf8 your_script.py`.

### 4. Lights are not responding (Wrong UUID)
If the Python script runs successfully but the lights don't change, your specific controller might use a different BLE Characteristic UUID. 
**The Fix:** Open `send_ble_color.py` and modify the `BLE_CHAR_UUID` variable. Common Lotus Lantern UUIDs are `ffe9`, `fff3`, `ffe1`, or `ffd9`.

---
*Happy Racing!* 🏁