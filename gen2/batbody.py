import csv

def parse_new_battinfo(data):
    return {
       # 'len': data[0],
       # 'partial': bool(data[1] & 0b00100000),
        'h1': (data[1] & 0b11000000) >> 6,
        'h2': (data[1] & 0b00110000) >> 4,
        'h3': (data[1] & 0b00001100) >> 2,
        'h4': (data[1] & 0b00000010) >> 1,
        'h5': (data[1] & 0b00000001),
        'cap': (data[1] & 0b00011111),
        'chg_3.6': (data[2] << 3) | ((data[3] & 0xE0) >> 5),
        'chg_1.0': (((data[3] & 0b00011111) << 6) | ((data[4]) >> 2)),
        'flg1': bool(data[4] & 0b00000010),
        'unk1': data[5] >> 3,
        'gids': (data[6] << 2) | (data[7] >> 6),
        'soh': str(bin(((data[7] & 0b00111111) << 1) | (data[8] >> 7))),
        'soc': (((data[8] & 0b01111111) << 4) | ((data[9] & 0b11110000) >> 4))/20, # SOC Nominal
        'unk2': (data[9] & 0b00001100) >> 2,
        'flg2': bool(data[9] & 0b00000100),
        'unk3': data[10] >> 3,
        'unk4': (data[10] << 5) | (data[11] >> 3),
        'socd': data[12], # SOC Display
        'b': bin(data[13]),
        'unk5': bin(data[14] >> 1),
        'chg_6.6': (data[15] << 3) | ((data[16] & 0xE0) >> 5),
        'unk6': data[17],
        'unk7': (data[18] << 2) | (data[19] >> 6),
        'unk8': (data[19] & 0b00000110) >> 1,
        'acoff': (data[20] << 2) | ((data[21] & 0b11) >> 6),
        'acon': ((data[21] & 0b00111111) << 4) | (data[22] >> 4),
    }


to_csv = [
    parse_new_battinfo(bytes.fromhex('17 C9 1A 45 28 08 38 A5 63 40 2E 40 53 BA 94 0F 02 8A 2A 80 12 C4 55 DC 00 00 00')),
    parse_new_battinfo(bytes.fromhex('17 C9 25 87 08 08 32 A5 59 40 2D A0 4A BA 94 0F 02 83 24 90 12 04 65 DC 00 00 00')),
    parse_new_battinfo(bytes.fromhex('17 C9 0F 02 58 08 40 65 72 40 2E E0 5F BA 94 08 C2 90 31 60 13 44 55 DC 00 00 00')),
    parse_new_battinfo(bytes.fromhex('17 C9 06 40 C8 08 44 25 7A 20 2F 80 64 BA 94 05 02 83 34 80 13 44 B5 DC 00 00 00')),
    parse_new_battinfo(bytes.fromhex('17 C9 06 40 C8 08 44 25 7A 20 2F 80 64 BA 94 05 02 83 34 80 13 44 B5 DC 00 00 00')),
    parse_new_battinfo(bytes.fromhex('17 C9 FF FF FC 08 43 25 78 20 2F 80 64 BA 94 FF E2 83 33 E0 13 44 C5 DC 00 00 00')),
    parse_new_battinfo(bytes.fromhex('17 C9 1E 05 A0 08 37 E5 62 00 2E 40 52 BA 94 0F 02 81 29 40 12 84 95 DC 00 00 00')),
    parse_new_battinfo(bytes.fromhex('17 C9 05 00 C8 08 44 25 7A 20 2F 80 64 BA 94 05 02 87 32 F0 12 C4 75 DC 00 00 00')),
    parse_new_battinfo(bytes.fromhex('17 C0 1A 45 28 08 38 A5 63 40 2E 40 53 BA 94 0B 42 82 26 20 10 C4 25 DC 00 00 00')),
    parse_new_battinfo(bytes.fromhex('17 C9 06 40 C8 08 44 25 7A 20 2F 80 64 BA 94 05 02 89 2F 30 11 44 15 DC 00 00 00')),
    parse_new_battinfo(bytes.fromhex('17 C9 FF FF FC 08 44 25 7A 20 2F 80 64 BA 94 FF E2 86 30 C0 12 04 45 DC 00 00 00')),
    parse_new_battinfo(bytes.fromhex('17 C9 FF FF FC 08 44 25 7A 20 2F 80 64 BA 94 FF E2 8A 31 B0 12 44 45 DC 00 00 00')),
    parse_new_battinfo(bytes.fromhex('17 C9 16 83 C0 08 3B E5 69 60 2E E0 58 BA 94 0B 42 84 2A 80 11 C4 55 DC 00 00 00')),
    parse_new_battinfo(bytes.fromhex('17 C0 0F 02 58 08 3F E5 71 60 2E E0 5F BA 94 0A 02 85 2D 50 11 C4 45 DC 00 00 00')),
    parse_new_battinfo(bytes.fromhex('17 C9 1A 45 28 08 38 65 62 E0 2E 40 53 BA 94 0F 02 80 26 20 11 04 35 DC 00 00 00')),
    parse_new_battinfo(bytes.fromhex('17 C0 16 83 C0 08 3B E5 69 A0 2E E0 59 BA 94 0B 42 82 2E 40 13 44 C5 DC 00 00 00')),
    parse_new_battinfo(bytes.fromhex('17 C9 16 84 38 08 3A A5 67 20 2E 40 56 BA 94 0B 42 82 2D 50 13 44 C5 DC 00 00 00')),
    parse_new_battinfo(bytes.fromhex('17 C9 16 83 C0 08 3C 25 69 E0 2E E0 58 BA 94 0B 42 84 2C B0 12 84 85 DC 00 00 00')),
    parse_new_battinfo(bytes.fromhex('17 C9 1E 05 A0 08 36 E5 60 60 2E 40 51 BA 94 0F 02 82 2A D0 13 84 C5 DC 00 00 00')),
    parse_new_battinfo(bytes.fromhex('17 C9 16 84 38 08 3B E5 69 40 2E E0 58 BA 94 0B 42 82 2D F0 13 44 B5 DC 00 00 00')),
    parse_new_battinfo(bytes.fromhex('17 C9 0F 02 D0 08 3E A5 6E A0 2E E0 5C BA 94 0A 02 88 2F 30 13 04 75 DC 00 00 00'))]

keys = to_csv[0].keys()

with open('data.csv', 'w', newline='') as output_file:
    dict_writer = csv.DictWriter(output_file, keys)
    dict_writer.writeheader()
    dict_writer.writerows(to_csv)