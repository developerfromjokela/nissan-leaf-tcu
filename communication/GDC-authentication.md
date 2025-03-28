# GDC | Authentication

TCU sends auth info with Type 1 and 3 responses. 

## Username

- Plaintext, allowed characters: AA-ZZ aa-zz 0-9 - _ .
- Max length: 16

## Password
- Password is "hashed" with a CRC-like algorithm by navi head unit and stored in TCU.
- Allowed characters: AA-ZZ aa-zz 0-9 - _ = + @ # ? !
- Max length: 16?

### "Hashing" algorithm

_You can check python script `passwordhasher.py` for working code._


- **Type**: Modified CRC-32

#### Fixed Suffix
- **Value**: `"evtelematics"`
- **Length**: 12 bytes
- **Encoding**: UTF-8
- **Role**: Appended to the input password before hashing
- **Purpose**: Adds a static, system-specific component to the hash input

#### CRC-32 Parameters
- **Polynomial**: `0xedb88320`
  - Reversed CRC-32 polynomial
  - Used for generating the lookup table and computing the hash
- **Initial CRC Value**: `0xffffffff`
  - 32-bit value with all bits set
- **Lookup Table**: 
  - 256 precomputed 32-bit values
  - Generated using the polynomial for each byte value (0-255)

#### Algorithm Steps
1. **Input Preparation**
   - Convert input password to UTF-8 bytes
   - Append the fixed suffix `"evtelematics"` (as UTF-8 bytes)
2. **CRC-32 Computation**
   - Initialize CRC to `0xffffffff`
   - For each byte in the combined input (password + suffix):
     - XOR the least significant byte of CRC with the input byte
     - Right-shift CRC by 8 bits
     - XOR with corresponding value from the lookup table
3. **Finalization**
   - Apply bitwise NOT (`~`) to the resulting CRC
   - Mask with `0xffffffff` to ensure a 32-bit unsigned integer output

#### Output
- **Format**: 32-bit unsigned integer
- **Value**: 8 hexadecimal digits (e.g., `1a2b3c4d`), TCU sends HEX data uppercase.
