
import base64
import hashlib
import hmac
import struct
from datetime import datetime


def encode_timestamp(dt: datetime) -> bytes:
    """
    Encode a datetime object to a 5-byte compact timestamp.

    Args:
        dt: datetime object to encode

    Returns:
        bytes: 5-byte encoded timestamp

    Raises:
        ValueError: If the datetime is out of valid range
    """
    # Validate ranges
    year = dt.year
    month = dt.month
    day = dt.day
    hour = dt.hour
    minute = dt.minute
    second = dt.second

    # Year must be 1990-2020 (0x7c6 to 0x804)
    year_offset = year - 1990
    if not (0 <= year_offset <= 62):
        raise ValueError(f"Year {year} out of range (1990-2052)")

    if not (1 <= month <= 12):
        raise ValueError(f"Month {month} out of range (1-12)")

    if not (1 <= day <= 31):
        raise ValueError(f"Day {day} out of range (1-31)")

    if not (0 <= hour <= 23):
        raise ValueError(f"Hour {hour} out of range (0-23)")

    if not (0 <= minute <= 59):
        raise ValueError(f"Minute {minute} out of range (0-59)")

    if not (0 <= second <= 59):
        raise ValueError(f"Second {second} out of range (0-59)")

    # Allocate 5 bytes
    timestamp = bytearray(5)

    # Byte 0: IE_ID (0) + More_Flag (0) + Length (4)
    timestamp[0] = 0x04  # [7:6]=00, [5]=0, [4:0]=00100

    # Byte 1: Year[5:0] + Month[3:2]
    year_bits = year_offset & 0x3F  # 6 bits
    month_upper = (month >> 2) & 0x03  # Upper 2 bits of month
    timestamp[1] = (year_bits << 2) | month_upper

    # Byte 2: Month[1:0] + Day[4:0] + Hour[4]
    month_lower = month & 0x03  # Lower 2 bits
    day_bits = day & 0x1F  # 5 bits
    hour_upper = (hour >> 4) & 0x01  # Bit 4 of hour
    timestamp[2] = (month_lower << 6) | (day_bits << 1) | hour_upper

    # Byte 3: Hour[3:0] + Minute[5:2]
    hour_lower = hour & 0x0F  # Lower 4 bits
    minute_upper = (minute >> 2) & 0x0F  # Upper 4 bits
    timestamp[3] = (hour_lower << 4) | minute_upper

    # Byte 4: Minute[1:0] + Second[5:0]
    minute_lower = minute & 0x03  # Lower 2 bits
    second_bits = second & 0x3F  # 6 bits
    timestamp[4] = (minute_lower << 6) | second_bits

    return bytes(timestamp)

def semi(num):
    n = b''
    for x in range(len(num) // 2):  # Use // for integer division in Python 3
        n += bytes([int(num[(x*2)+1]) << 4 | int(num[x*2])])
    if len(num) % 2 != 0:
        n += bytes([0xF0 | int(num[-1])])
    return n

def decode_timestamp(data: bytes) -> datetime:
    if len(data) < 5:
        raise ValueError(f"Expected 5 bytes, got {len(data)}")

    # Byte 0: IE_ID + More_Flag + Length
    ie_id = (data[0] >> 6) & 0x03
    more_flag = (data[0] >> 5) & 0x01
    length = data[0] & 0x1F

    if ie_id != 0:
        raise ValueError(f"Invalid IE_ID: {ie_id}")
    if length != 4:
        raise ValueError(f"Invalid Length: {length} (expected 4)")

    # Byte 1: Year + Month[3:2]
    year_offset = (data[1] >> 2) & 0x3F
    month_upper = data[1] & 0x03

    # Byte 2: Month[1:0] + Day + Hour[4]
    month_lower = (data[2] >> 6) & 0x03
    day = (data[2] >> 1) & 0x1F
    hour_upper = data[2] & 0x01

    # Byte 3: Hour[3:0] + Minute[5:2]
    hour_lower = (data[3] >> 4) & 0x0F
    minute_upper = data[3] & 0x0F

    # Byte 4: Minute[1:0] + Second
    minute_lower = (data[4] >> 6) & 0x03
    second = data[4] & 0x3F

    # Reconstruct values
    year = year_offset + 1990
    month = (month_upper << 2) | month_lower
    hour = (hour_upper << 4) | hour_lower
    minute = (minute_upper << 2) | minute_lower

    # Create datetime
    try:
        return datetime(year, month, day, hour, minute, second)
    except ValueError as e:
        raise ValueError(f"Invalid timestamp values: {e}")


def hex_dump(data: bytes) -> str:
    """Return hex string representation of bytes."""
    return " ".join(f"{b:02x}" for b in data)

# 32-byte key — replace with your actual key
# Example: 32 zero bytes (placeholder)
key = bytes.fromhex("b8 8d dc e8 8e 64 ca 09 63 22 4e 2c 94 f3 10 10 3e 25 57 20 c0 4a 8f 79 4b 9f 9a 27 58 b6 00 b7")

# Binary data
data = bytearray.fromhex("8203003304020100022021b00051534a4e4641415a453055363033393239334c32303335353531303132383228a9046eecf9600000008eeb81a2b1f0e8c41efbbae66663aac5b7a8af4b11")
length_field = struct.unpack('>H', data[2:4])[0]
offset = length_field + 4  # Add 4 for header and length fields

if offset > len(data):
    raise ValueError(f"Invalid offset {offset}: message length is {len(data)}")

# Split into payload and HMAC
orig_hmac = data[offset:]
data = bytearray(data[:offset])
mac = hmac.new(key, data, hashlib.sha1)

print(f"HMAC-SHA1 (hex):    {mac.hexdigest().upper()}")
print(f"HMAC-SHA1 ORIG (hex):    {orig_hmac.hex().upper()}")
print(f"IDENTICAL: {mac.digest() == orig_hmac}")

tstamp = decode_timestamp(bytes(data[46:51]))
print(f"Old Tstamp: {bytes(data[46:51]).hex()} {tstamp}")
now = datetime.now()
new_tstamp = encode_timestamp(now)
print(f"New Tstamp: {new_tstamp.hex()} {now}")


old_reqid = data[45]
print(f"Old RequestID: {old_reqid}")
new_reqid = 212
print(f"New RequestID: {new_reqid}")
data[45] = new_reqid

old_destid = data[44]
print(f"Old DestinationID: {hex(old_destid)}")
new_destid = old_destid
print(f"New DestionationId: {hex(new_destid)}")
data[44] = new_destid

# Compute HMAC-SHA1
mac = hmac.new(key, data, hashlib.sha1)

# 71 F5 D9 1F 54 E7 F3 B4 A8 DC 8E 01 31 EA 99 09 FA BA F1 03
print(f"Data ({len(data)} bytes): {data.hex(' ').upper()}")
print(f"Key  ({len(key)} bytes):  {key.hex(' ').upper()}")
print()
print(f"HMAC-SHA1 (hex):    {mac.hexdigest().upper()}")
print(f"HMAC-SHA1 ORIG (hex):    {orig_hmac.hex().upper()}")
print(f"IDENTICAL: {mac.digest() == orig_hmac}")
print(f"HMAC-SHA1 (bytes):  {' '.join(f'{b:02X}' for b in mac.digest())}")
print("MAC LEN", len(mac.digest()))
data = data+mac.digest()
print("hex", data.hex().lower())
print("base64", base64.b64encode(data).decode('ascii'))

number = "358444444444"
semi_num = semi(number)

hdr = b''.join((
    b'\x00',  # SMSC info length, not included in PDU len
    b'\x11',  # First octet of SMS-SUBMIT message
    # 0x11 = message type SMS SUBMIT, validity
    # period present and relative
    b'\x00',  # Message reference
    len(number).to_bytes(1, 'big'),  # Length of the phone number (11)
    b'\x91',  # Type of number (0x81 = international)
    semi_num,  # Telephone number
    b'\x00',  # TP-PID Protocol ID.
    b'\x04',  # TP-DCS data coding scheme. 0x4=8 bit data
    b'\xAA',  # Validity period
    len(data).to_bytes(1, 'big'),  # TP-User-Data-Length. Length of the message
    data
))

print(f"PDU: {len(hdr)-1} {hdr.hex()}")

with open("smsfile.bin", "wb") as f:
    f.write(data)
    f.write(mac.digest())
