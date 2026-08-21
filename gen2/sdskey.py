


def derive_sds_key(param_1: bytes) -> bytes | None:
    """
    AES-Key derivation sent from PMC (PICC-chip on TCU) -> Baseband
    """
    if len(param_1) < 20:
        raise ValueError("input must be at least 20 bytes")

    # First byte must equal 0xCC (0xCC == -0x34 as a signed char)
    if param_1[0] != 0xCC:
        return None

    key = bytearray(16)
    key_idx = 0
    i = 0     # mirrors iVar6
    src = 1   # mirrors uVar5

    while True:
        if i == 4 or i == 9:
            # skipped positions (src == 5 or src == 10) -- no write
            if src > 18:
                break
        elif i != 14:
            key[key_idx] = (~param_1[src]) & 0xFF
            key_idx = (key_idx + 1) & 0xFF
            if src > 18:
                break
        # i == 14 (src == 15): also skipped, no break-check this iteration
        i += 1
        src += 1

    return bytes(key)


print(derive_sds_key(bytes.fromhex("CC 00 09 00 09 00 09 00 09 00 09 00 09 00 09 00 09 00 EF 00 09 00 09 00 09 00 09 00 09 00 09 00 09 00 09 00 7B 00 09 00 09 00 09 00 09 00 36 00 4E 01 36 00 4E 01 E1 00 36 00 36 00 36 00 36 00 4E 01 36 00 36 00 36 00")).hex())