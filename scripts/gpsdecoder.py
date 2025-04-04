def hex_to_location(hex_string):
    # Convert hex string to bytes (remove spaces and convert)
    hex_string = hex_string.replace(" ", "")
    byte_data = bytes.fromhex(hex_string)

    # Check if we have at least 14 bytes (bytes 0-13, with location in 6-13)
    if len(byte_data) < 14:
        return "Error: Input must be at least 14 bytes long (7-14 contain location data)."

    # Extract home (byte 6, 0-indexed 5)
    # Ensure home_byte is within 8-bit range (since it's effectively a byte)
    home_byte = byte_data[5] & 0xFF

    # Extract flags using bitwise operations
    pos_uint = (home_byte >> 7) & 1  # Bit 7: Position indicator
    uint_datum2 = (home_byte >> 6) & 1  # Bit 6: Datum flag
    lat_mode_uint = (home_byte >> 5) & 1  # Bit 5: Latitude mode
    longitude_mode_uint = (home_byte >> 4) & 1  # Bit 4: Longitude mode
    home_uint = (home_byte >> 3) & 1  # Bit 3: Home indicator

    # Interpret the flags
    position_status = "Valid" if pos_uint == 1 else "Invalid or Unknown"
    datum_status = "Valid Datum" if uint_datum2 == 1 else "Invalid or Default Datum"
    latitude_mode = "North" if lat_mode_uint == 0 else "South"
    longitude_mode = "East" if longitude_mode_uint == 0 else "West"
    home_status = "At Home" if home_uint == 0 else "Not at Home"

    # Extract latitude (bytes 6-9, 0-indexed)
    lat_deg = byte_data[6]  # Byte 7
    lat_min = byte_data[7]  # Byte 8
    lat_sec = int.from_bytes(byte_data[8:10], byteorder='big')  # Bytes 9-10

    # Extract longitude (bytes 10-13, 0-indexed)
    lon_deg = byte_data[10]  # Byte 11
    lon_min = byte_data[11]  # Byte 12
    lon_sec = int.from_bytes(byte_data[12:14], byteorder='big')  # Bytes 13-14

    # Convert seconds (assuming scaling factor of 100)
    lat_sec_float = lat_sec / 100.0
    lon_sec_float = lon_sec / 100.0

    # Convert to decimal degrees
    latitude = lat_deg + (lat_min / 60.0) + (lat_sec_float / 3600.0)
    longitude = lon_deg + (lon_min / 60.0) + (lon_sec_float / 3600.0)

    # Apply coordinates based on latitude and longitude modes
    if latitude_mode == "South":
        latitude = -latitude
    if longitude_mode == "West":
        longitude = -longitude


    return {
        "latitude": latitude,
        "longitude": longitude,
        "home_status": home_status,
        "home_raw": f"0x{home_byte:02X}",
        "position_status": position_status,
        "datum_status": datum_status,
        "latitude_mode": latitude_mode,
        "longitude_mode": longitude_mode,
    }

locations = [
    # Add location data to dump
]

# Example usage
if len(locations) == 0:
    print("Add location hex to parse!")
for loc in locations:
    result = hex_to_location(loc)
    print(f"Decoded Location (datum {result['datum_status']}):")
    print(f"Latitude: {result['latitude']}, mode: {result['latitude_mode']}")
    print(f"Longitude: {result['longitude']}, mode: {result['longitude_mode']}")
    print(f"Home Status: {result['home_status']} (Raw: {result['home_raw']})")
    print("")
