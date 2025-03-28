# GDC | GPS data

This document describes the binary data structure used for storing GPS coordinates and metadata in a 14-byte array.

Bytes 103-117

### Metadata (Byte 5, 0-indexed)
Bit flags containing GPS metadata:
- Bit 7: Valid position (boolean)
- Bit 6: Datum flag (boolean)
- Bit 5: Latitude mode
  - 0 = South (-1)
  - 1 = North (+1)
- Bit 4: Longitude mode
  - 0 = West (-1)
  - 1 = East (+1)
- Bit 3: Home status (boolean)

### Latitude Components
- Byte 6: Degrees (0-90)
- Byte 7: Minutes (0-59)
- Bytes 8-9: Seconds
  - 16-bit integer
  - Big-endian format
  - Scaled by factor of 100

### Longitude Components
- Byte 10: Degrees (0-180)
- Byte 11: Minutes (0-59)
- Bytes 12-13: Seconds
  - 16-bit integer
  - Big-endian format
  - Scaled by factor of 100

## Coordinate Representation
- Latitude range: -90° to +90°
- Longitude range: -180° to +180°
- Seconds stored as hundredths (×100 scaling)