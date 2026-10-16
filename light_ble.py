import sys
import asyncio
from bleak import BleakClient

# The standard Lotus Lantern characteristic UUID you verified
BLE_CHAR_UUID = "0000fff3-0000-1000-8000-00805f9b34fb"

async def send_color(mac_address, r, g, b):
    try:
        async with BleakClient(mac_address) as client:
            # 1. Wake up the strip if turned off via app
            power_on_payload = bytearray([0x7e, 0x00, 0x04, 0xf0, 0x00, 0x01, 0xff, 0x00, 0xef])
            await client.write_gatt_char(BLE_CHAR_UUID, power_on_payload)
            await asyncio.sleep(0.1)

            # 2. Send the F1 Leader RGB color
            color_payload = bytearray([0x7e, 0x00, 0x05, 0x03, r, g, b, 0x00, 0xef])
            await client.write_gatt_char(BLE_CHAR_UUID, color_payload)
            print(f"[BLE] Sent RGB({r}, {g}, {b}) to {mac_address}")
    except Exception as e:
        print(f"[BLE Error] {e}")

if __name__ == "__main__":
    if len(sys.argv) != 5:
        print("Usage: python light_ble.py <MAC> <R> <G> <B>")
        sys.exit(1)

    mac = sys.argv[1]
    r = int(sys.argv[2])
    g = int(sys.argv[3])
    b = int(sys.argv[4])

    asyncio.run(send_color(mac, r, g, b))