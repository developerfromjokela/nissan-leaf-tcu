## Response Packet types

- 1 = packet without data
- 3 = packet with additional data


**# Common response packet structure

- [1] Packet type
- [2] ? - so far 02 (protocol version?)
- [3] direction type? 00 for request, 01 for response?
- [4] Packet overall size (bytes, unsigned int) 
- [5-8] 20 5E B1 70 (before auth)
- [9-83] Auth and info (Logon)
- [101] Body type? (27 for Logon and 29 for charging reminder)
- [103-107] Potential location data
- [118] Auth info length
- [119-153] Auth information
- [154] Body type?
- [155-] Request body


# GDC server commands

Documentation of GDC commands that can be sent after initial 01 response from NISSAN_EVIT_TELEMATICS_CENTER.

## General Message Structure

| Byte | Field            | Size    | Description                                      | Notes                                      |
|------|------------------|---------|--------------------------------------------------|--------------------------------------------|
| 0    | Message Type     | 1 byte  | Identifies the message category (0–31, `& 0x1f`) | `0x02` = Authentication/Commands, `0x04` = Config Request, `0x06` = Notification |
| 1    | Reserved         | 1 byte  | Unused, typically `0x00`                        |                                            |
| 2–3  | Length           | 2 bytes | Total message size (big-endian)                 | `0x0008` for Type 2, variable for Type 4   |
| 4    | Destination      | 1 byte  | Target or subtype (e.g., `0x28`, `0x2C`)        | Defines action or subsystem                |
| 5    | Payload/Reserved | 1 byte  | Context-specific, often `0x02`                  | Required by `HandleRFCMessage` check       |
| 6    | Reserved         | 1 byte  | Unused, typically `0x00`                        |                                            |
| 7    | Command/Flags    | 1 byte  | Command code and flags                          | MSB + bits 4–6 define action              |
| 8+   | Payload          | Variable| Additional data (if length > 8)                 | Not used in Type 2 commands               |

- **Endianness**: Big-endian for multi-byte fields (e.g., length).
- **Minimum Size**: 8 bytes for Type 2; Type 4 and 6 may vary.

## Message Types

| Type | Hex  | Description            | Length Constraint | Notes                              |
|------|------|------------------------|-------------------|------------------------------------|
| 2    | `0x02` | Authentication/Commands | Exactly 8 bytes   | Handled by `HandleCommands` for specific destinations |
| 4    | `0x04` | Configuration Request  | 8+ bytes          | Subtypes `0x2E`, `0x2D`, `0x27` recognized |
| 6    | `0x06` | Configuration Notification | Exactly 8 bytes   | Subtype `0x2D` processed         |
| Others | `0x00–0x1F` | Unknown          | Unknown           | Logs `"message_type_not_recognised"` |

## Destinations (Byte 4)

| Value   | Context         | Description                     | Notes                              |
|---------|-----------------|---------------------------------|------------------------------------|
| `0x27`  | Type 2, 4       | Common Destination             | Logs `"desination_is_common"`      |
| `0x28`  | Type 2          | Charge Status                  | Paired with command `0x01`         |
| `0x2B`  | Type 2          | Charge Request                 | Paired with command `0x02`         |
| `0x2C`  | Type 2          | AC Control                     | Commands `0x03` (setting), `0x04` (stop) |
| `0x2D`  | Type 4, 6       | Configuration Change/Notification | Variable length in Type 4       |
| `0x2E`  | Type 4          | Configuration Confirm          | Returns device config, 8 bytes    |


## Command/Flags (Byte 7)

| Bit(s)    | Description          | Values           | Notes                                      |
|-----------|----------------------|------------------|--------------------------------------------|
| 7 (MSB)   | Success/Failure      | `0` = Success, `1` = Failure/Continue | Type 2: `0` triggers `"NG"` (auth failure) |
| 4–6       | Command Code         | `0x00`–`0x07`    | Extracted as `(byte & 0x70) >> 4`          |
| 0–3       | Unused/Reserved      | Typically `0`    | Not parsed in Type 2                       |

- **Encoding**: Command value is shifted left 4 bits and ORed with MSB (e.g., `0x01` → `0x90` with MSB = 1).
- **Examples**:
  - `0x90`: MSB = 1, Command = 1 (`0x10 >> 4`).
  - `0xB0`: MSB = 1, Command = 3 (`0x30 >> 4`).

## Confirmed Commands (Type 2)

| Destination | Hex (Byte 4) | Command | Hex (Byte 7) | Full Message             | Description            | Response Function | Notes                     |
|-------------|--------------|---------|--------------|--------------------------|------------------------|-------------------|---------------------------|
| `0x28`      | `0x28`       | `0x01`  | `0x90`       | `02 00 00 08 28 02 00 90` | Charge Status          | `FUN_002594d0`    | Logs `"charge_status_m"`  |
| `0x2B`      | `0x2B`       | `0x02`  | `0xA0`       | `02 00 00 08 2B 02 00 A0` | Charge Request         | `FUN_00290f04`    | Logs `"charge_request_r"` |
| `0x2C`      | `0x2C`       | `0x03`  | `0xB0`       | `02 00 00 08 2C 02 00 B0` | AC Setting Received    | `FUN_00290f64`    | Logs `"AC_setting_rece"`  |
| `0x2C`      | `0x2C`       | `0x04`  | `0xC0`       | `02 00 00 08 2C 02 00 C0` | AC Stop Received       | `FUN_00290f94`    | Logs `"AC_stop_receive"`  |

- **Byte 5 Requirement**: `0x02` appears necessary (possibly for `HandleRFCMessage`’s payload check).
- **Response**: Typically starts with `0x03` (response type), followed by device data (e.g., IDs, status).

## Potential Additional Commands

### Type 2: Auth result and command
- **Untested Commands**: `0x00`, `0x05`, `0x06`, `0x07` (Byte 7: `0x80`, `0xD0`, `0xE0`, `0xF0`).
  - Example: `02 00 00 08 28 02 00 D0` (`0x05` with `0x28`).
- **Other Destinations**: Test `0x27`, `0x2D`, etc., with known commands.
  - Example: `02 00 00 08 27 02 00 90`.

### Type 4: Configuration
- **Confirmed**: 
  - `04 00 00 08 2E 00 00 00` (Config Confirm, returns device config, response type 5).
- **Untested**: 
  - `04 00 00 08 27 00 00 00` (Common Destination).
  - `04 00 00 0C 2D 00 00 C0 00 00 00 00` (Config Change, variable length).

### Type 6
- **Untested**: 
  - `06 00 00 08 2D 02 00 00` (Notification).

## Notes
- **Socket Behavior**: MSB = 1 in Byte 7 keeps the connection alive; MSB = 0 closes it (auth termination).
- **State**: `field585_0x24c == 0x02` required by `HandleCommands`, set by `NetIPSocketHandler` or RFC.


