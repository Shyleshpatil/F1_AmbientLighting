import asyncio
from bleak import BleakScanner

async def scan_for_lights():
    print("Scanning for Bluetooth LE devices for 5 seconds...")
    devices = await BleakScanner.discover(timeout=5.0)

    for device in devices:
        # Filter to make it easier to read. Lotus Lanterns usually broadcast
        # names like ELK-BLEDOM, MELK, LEDBLE, or BLEDOWFA.
        if device.name and ("ELK" in device.name or "LED" in device.name or "BLE" in device.name):
            print(f"Found LED Strip! Name: {device.name} | MAC Address: {device.address}")
        else:
            # Print other unknown devices just in case
            print(f"Found: {device.name} | MAC Address: {device.address}")

if __name__ == "__main__":
    asyncio.run(scan_for_lights())