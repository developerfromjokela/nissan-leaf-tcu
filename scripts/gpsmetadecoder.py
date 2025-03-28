def extract_data_from_home_byte(home_byte):

    home_byte = home_byte & 0xFF

    # Extract flags using bitwise operations
    pos_uint = (home_byte >> 7) & 1  # Bit 7: Position indicator
    uint_datum2 = (home_byte >> 6) & 1  # Bit 6: Datum flag
    lat_mode_uint = (home_byte >> 5) & 1  # Bit 5: Latitude mode
    longitude_mode_uint = (home_byte >> 4) & 1  # Bit 4: Longitude mode
    home_uint = (home_byte >> 3) & 1  # Bit 3: Home indicator

    # Interpret the flags
    position_status = pos_uint == 1
    datum_status = uint_datum2 == 1
    latitude_mode = "North" if lat_mode_uint == 0 else "South"
    longitude_mode = "East" if longitude_mode_uint == 0 else "West"
    home_status = home_uint == 0

    # Return extracted data in a dictionary
    return {
        "home_byte_binary": f"{home_byte:08b}",
        "home_byte_decimal": home_byte,
        "position_status": position_status,
        "datum_status": datum_status,
        "latitude_mode": latitude_mode,
        "longitude_mode": longitude_mode,
        "home_status": home_status,
    }


# Example usage
if __name__ == "__main__":
    # Example 1: At home
    home_byte_valid = 0xC0  # 11111000 (pos=1, datum=1, lat=1, long=1, home=1)
    result_valid = extract_data_from_home_byte(
        home_byte_valid,
    )
    print("Example 1: At home")
    for key, value in result_valid.items():
        print(f"{key}: {value}")
    print()

    # Example 2: Not at home
    home_byte_invalid = 0xC8  # 00000000
    result_invalid = extract_data_from_home_byte(
        home_byte_invalid,
    )
    print("Example 2: Not at home")
    for key, value in result_invalid.items():
        print(f"{key}: {value}")
    print()