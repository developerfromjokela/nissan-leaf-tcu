import csv
import hashlib
import os
import sys


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


def parse_battinfo2(data, filename):
    return {
        'file': filename,
        'h1': (data[1] & 0b10000000) > 0,
        'h2': (data[1] & 0b01000000) > 0,
        'h3': (data[1] & 0b00100000) > 0,
        'h4': (data[1] & 0b00010000) > 0,
        'h5': (data[1] & 0b00001000) > 0,
        'h6': (data[1] & 0b00000100) > 0,
        'h7': (data[1] & 0b00000010) > 0,
        'h8': (data[1] & 0b00000001) > 0,
        'chg1': (data[2] << 3) | ((data[3] & 0b11100000) >> 5),
        'chg2': ((data[3] & 0b00011111) << 6) | ((data[4] & 0b11111100) >> 2),
        'b5': (data[5] & 0b11111000) >> 3,
        'gids': ((data[6]) << 2) | ((data[7] & 0b11) >> 7),
        'socd': ((data[7] & 0b00111111) << 1) | ((data[8] & 0b10000000) >> 7),
        'sohnom': ((data[8] & 0b01111111) << 3) | ((data[9] & 0b11100000) >> 5),
        '9v2': ((data[9] & 0b00011000) >> 3),
        '9v2flag': ((data[9] & 0b00000100) >> 2),
        '10v1': ((data[10] & 0b11111000) >> 3),
        '11v2': ((data[10] & 0b00000111) << 5) | ((data[11] & 0b11111000) >> 3),
        '12': data[12],
        '13': data[13],
        '14': data[14] >> 1,
        '15v1': (data[15] << 3) | ((data[16] & 0b11100000) >> 5),
        '16f1': (data[16] & 0b00010000) >> 4,
        '16f2': (data[16] & 0b00001000) >> 3,
        '16f4': (data[16] & 0b00000100) >> 2,
        '16f5': (data[16] & 0b00000010) >> 1,
        '17': (data[17]),
        '18v1': float(((data[18] << 4) | ((data[19] & 0b11110000) >> 4)))/8.0,
        '19f1': (data[19] & 0b00000100) >> 2,
        '19f2': (data[19] & 0b00000010) >> 1,
        '20v1': (data[20] << 2) | ((data[21] & 0b11000000) >> 6),
        '21v1': ((data[21] & 0b00111111) << 4) | ((data[22] & 0b11110000) >> 4),
        '22v1': ((data[22] & 0b00001111) << 6) | ((data[23] & 0b11111100) >> 2),
        '23v2': ((data[23] & 0b00000011)),

    }




to_csv = []

for itm in os.listdir(f"chgcapture"):
    if itm.endswith(".bin"):
        print(itm)
        with open(f"chgcapture/{itm}", "rb") as f:
            data = f.read()
            to_csv.append(parse_battinfo2(data[203:], itm))

keys = to_csv[0].keys()

with open('data.csv', 'w', newline='') as output_file:
    dict_writer = csv.DictWriter(output_file, keys)
    dict_writer.writeheader()
    dict_writer.writerows(to_csv)