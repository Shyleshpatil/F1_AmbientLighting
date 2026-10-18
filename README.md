# F1 Ambient Lighting Controller

Bring the thrill of Formula 1 race day into your room! This application tracks live F1 telemetry and automatically changes the color of your ambient LED lighting to match the livery of the team currently in 1st Place (P1). 

When a driver makes an overtake for the lead, your room instantly changes color to match.

## Resources & Technologies Used
* **Java (11/17):** Core application logic, API polling, and state management.
* **Python (3.x):** Hardware interfacing using the asynchronous `bleak` library.
* **OpenF1 API:** Providing live, real-time race telemetry and driver data.
* **Maven:** Dependency management and standalone JAR packaging.
* **Hardware:** Any generic Bluetooth LED strip utilizing the "Lotus Lantern" or "ELK-BLEDOM" protocol.

## How It Works
1. **The Poller:** A lightweight Java background process queries the live [OpenF1 API](https://openf1.org/) every 5 seconds.
2. **The Logic:** It fetches the `driver_number` currently holding Position 1 and cross-references it to their current `team_name`.
3. **The Trigger:** If the leader has changed, Java maps the team to a predefined RGB hex value.
4. **The Bridge:** Java spins up a minimal Python script, passing the target MAC address and RGB values. Python connects to the LED strip via Bluetooth Low Energy (BLE), wakes up the controller, pushes the color payload, and disconnects seamlessly.

---

##  Setup & Deployment Guide

Because hardware configurations differ, the final **Deployment Folder** is not included in this repository. Follow these steps to build and deploy the application locally on your machine.

### Prerequisites
* Java Development Kit (JDK 11 or 17)
* Python 3.x installed with the `bleak` library (`pip install bleak`)
* Maven installed (or use your IDE's built-in Maven tools)

### Step 1: Build the Application
Clone this repository to your local machine. Open your terminal in the project root and run:

```bash
mvn clean package -DskipTests
```
> **Note on Tests:** We use the `-DskipTests` flag here. Because the application defaults to pulling *live* internet data from the latest session, running the standard `mvn clean package` might cause local mock-server tests to fail if real-world P1 data overrides the test data.

### Step 2: Create the Deployment Folder
Create a new, empty folder anywhere on your computer (e.g., `C:\F1_Lighting\`). 

Copy the following two files into this new folder:
1. `target/ambient-tracker-1.0-SNAPSHOT.jar` (Generated in Step 1)
2. `light_ble.py` (Found in the root of the repository)

### Step 3: Create the Batch Launcher
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
└── light_ble.py
```

### Step 4: First Launch
Double-click `F1_Lighting.bat`. On the very first run, a pop-up window will ask for your LED strip's Bluetooth MAC address (e.g., `7D:E0:BB:72:B3:AD`). Enter it, and the app will generate a `config.properties` file so you never have to type it again. 

---

##  Troubleshooting & Known Issues

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

### 2. Python Unicode/Emoji Crash during Bluetooth Scanning
If you are writing a custom BLE scanner script and Windows throws a `UnicodeEncodeError: 'charmap' codec can't encode character`, Windows is struggling to print emojis (like ✅) to the standard command prompt. 
**The Fix:** Either remove the emoji from your `print()` statement, or run the script enforcing UTF-8: `python -X utf8 your_script.py`.

### 3. Lights are not responding (Wrong UUID)
If the Python script runs successfully but the lights don't change, your specific controller might use a different BLE Characteristic UUID. 
**The Fix:** Open `send_ble_color.py` and modify the `BLE_CHAR_UUID` variable. Common Lotus Lantern UUIDs are `ffe9`, `fff3`, `ffe1`, or `ffd9`. (The default in this repo is `fff3`).

### 4. Change the MAC address (Add new light replacing the old one)
If you want to connect to a new light, we can't do it on start up again. Since we programmed the application to save your MAC address after the very first launch, we require to go to your deployment folder (e.g., C:\F1LightingApp\) where your .bat and .jar files live.
**The Fix:** 
Look for the file named config.properties in your deployment folder.
Delete it.
Double-click your F1_Lighting.bat file. Because the file is missing, the application will automatically pop up the initial setup dialog box again and ask you to type in the new MAC address.


### Snapshot
Verstapen, Redbull in P1 in Singapore GP (11th Oct 20206)

<img width="512" height="911" alt="image" src="https://github.com/user-attachments/assets/06e21b5e-7b7c-4bf8-a876-992dcd9d29db" />

---
*Happy Racing!* 

