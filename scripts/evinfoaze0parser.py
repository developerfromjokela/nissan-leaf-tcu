from os import listdir
from os.path import isfile, join

def parse_evinfo(byte_data):
    rangeinfo_len = int(byte_data[0])

    evinfo_len = int(byte_data[8])

    # Charge & Pre-AC state
    charge_state = (byte_data[9] >> 6) & 0b11

    pluggedin = (byte_data[9] & 1) == 1
    charging = charge_state == 1
    acstate = bool((byte_data[9] >> 1) & 0b1)

    chg_time_1 = (
        (byte_data[10] << 3) | ((byte_data[11] & 0b11100000) >> 5)
    )

    chg_time_2 = (
        ((byte_data[11] & 0b00011111) << 6) | ((byte_data[12] & 0b11111100) >> 2)
    )

    # Drive info
    # GEAR info, True = D, False = R or N?
    drive_forward = bool(byte_data[9] & 0b00001000)
    park_gear = bool(byte_data[9] & 0b00000100)
    ignition = bool(byte_data[9] & 0b00100000)


    # Battery info
    soc_display = 0
    chargebars = (
        ((byte_data[18] & 0b00000011) << 2) | ((byte_data[19] & 0b11000000) >> 6)
    )
    capacity_bars = (byte_data[19] & 0b00011110) >> 1
    soc = (
        ((byte_data[14] & 0b00111111) << 1) | ((byte_data[15] & 0b10000000) >> 7)
    )
    gids = (
        ((byte_data[14] & 0b00111111) << 2) | ((byte_data[15] & 0b11000000) >> 6)
    )
    soh = (
        ((byte_data[15] & 0b00111111) << 1) | ((byte_data[16] & 0b10000000) >> 7)
    )

    # AZE0 extra info
    if evinfo_len > 11:
        soc_display = (
                ((byte_data[20] & 0b00000111) << 4) | ((byte_data[21] & 0b11110000) >> 4)
        )
    byte1 = byte_data[16]  # 01010001 in binary
    byte2 = byte_data[17]  # 00100000 in binary

    # Extract bits:
    # - Byte 1: Ignore first bit (0), take last 7 bits (1010001)
    bits_from_byte1 = byte1 & 0x7F  # Mask to get last 7 bits: 1010001 (81 decimal)
    # Shift left by 4 to make room for byte2's bits
    shifted_bits = bits_from_byte1 << 4

    # - Byte 2: Take first 4 bits (0010)
    bits_from_byte2 = (byte2 >> 4) & 0x0F  # Shift right 4, mask to get 0010 (2 decimal)

    # Combine the bits
    param16_17 = shifted_bits | bits_from_byte2  # 10100010010 in binary


    range_acon = byte_data[2]
    range_acoff = byte_data[4]

    resultstate = byte_data[7]
    alertstate = byte_data[6]

    return {
        "rangeinfo_len": rangeinfo_len,
        "evinfo_len": evinfo_len,
        "acon": range_acon,
        "acoff": range_acoff,
        "pluggedin":pluggedin,
        "charging": charging,
        "acstate": acstate,
        "chargebars": chargebars,
        "chargestate": charge_state,
        "resultstate": resultstate,
        "alertstate": alertstate,
        "ignition": ignition,
        "parked": park_gear,
        "direction_forward": drive_forward,
        "soc": soc,
        "soc_display": soc_display,
        "gids": gids,
        "soh": soh,
        "gids_relative": param16_17,
        "capacity_bars": capacity_bars,
        "full_chg": chg_time_1,
        "limit_chg": chg_time_2,
    }

if __name__ == "__main__":
    onlyfiles = [f for f in listdir("../captured_data/aze0/evcaninfo") if isfile(join(
        "../captured_data/aze0/evcaninfo", f)) and f.endswith(".bin")]
    onlyfiles.sort()
    dtrows = []
    for file in onlyfiles:
        with open(f"../captured_data/aze0/evcaninfo/{file}", "rb") as f:
            data = f.read()
            ev_data = data[153:]

            try:
                result = parse_evinfo(ev_data)
                for key, value in result.items():
                    print(f"{key}: {value} (0x{value:02X})")
                print("----------------", file)
            except ValueError as e:
                print(f"Error: {e}")
