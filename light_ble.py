import sys
import asyncio
from bleak import BleakClient

BLE_CHAR_UUID = "0000fff3-0000-1000-8000-00805f9b34fb"

async def send_color(client, r, g, b):
    color_payload = bytearray([0x7e, 0x00, 0x05, 0x03, r, g, b, 0x00, 0xef])
    await client.write_gatt_char(BLE_CHAR_UUID, color_payload)

async def run_sequence(mac, mode, r, g, b):
    try:
        async with BleakClient(mac) as client:
            # Always wake up the strip first
            power_on_payload = bytearray([0x7e, 0x00, 0x04, 0xf0, 0x00, 0x01, 0xff, 0x00, 0xef])
            await client.write_gatt_char(BLE_CHAR_UUID, power_on_payload)
            await asyncio.sleep(0.1)

            if mode == "SOLID":
                await send_color(client, r, g, b)
                print(f"[BLE] SOLID: RGB({r}, {g}, {b})")

            elif mode == "BLINK_YELLOW":
                for _ in range(3):
                    await send_color(client, 255, 255, 0)
                    await asyncio.sleep(0.5)
                    await send_color(client, 0, 0, 0)
                    await asyncio.sleep(0.5)
                await send_color(client, r, g, b)
                print(f"[BLE] YELLOW FLAG CYCLE: Complete")

            elif mode == "BLINK_RED":
                for _ in range(3):
                    await send_color(client, 255, 0, 0)
                    await asyncio.sleep(0.5)
                    await send_color(client, 0, 0, 0)
                    await asyncio.sleep(0.5)
                await send_color(client, r, g, b)
                print(f"[BLE] RED FLAG CYCLE: Complete")

    except Exception as e:
        print(f"[BLE Error] {e}")

if __name__ == "__main__":
    if len(sys.argv) != 6:
        print("Usage: python send_ble_color.py <MAC> <MODE> <R> <G> <B>")
        sys.exit(1)

    mac = sys.argv[1]
    mode = sys.argv[2]
    r_val = int(sys.argv[3])
    g_val = int(sys.argv[4])
    b_val = int(sys.argv[5])

    asyncio.run(run_sequence(mac, mode, r_val, g_val, b_val))