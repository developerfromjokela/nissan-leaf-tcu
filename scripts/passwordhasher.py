"""
Reverse-engineered function for generating password, that is sent to TCU from IVI
Algo source: ComMid.exe from infotainment system files
"""

def generate_crc32_table():
    table = []
    poly = 0xedb88320  # Reversed CRC-32 polynomial
    for i in range(256):
        value = i
        for _ in range(8):
            if value & 1:
                value = (value >> 1) ^ poly
            else:
                value = value >> 1
        table.append(value)
    return table


def crc32(data):
    """Compute the CRC-32 checksum of the input data."""
    data = data+b"evtelematics"
    crc_table = generate_crc32_table()
    crc = 0xffffffff

    for byte in data:
        crc = (crc >> 8) ^ crc_table[(crc & 0xff) ^ byte]

    return ~crc & 0xffffffff


if __name__ == "__main__":
    test_data = input("Enter password to generate hash: ").encode('utf-8')
    # Compute CRC-32
    result = crc32(test_data)
    print("Hash (custom CRC-32) of '%s': %08x" % (test_data.decode(), result))