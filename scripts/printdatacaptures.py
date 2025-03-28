"""""
Script for testing
"""""
from os import listdir
from os.path import isfile, join

def range1(byte17):
    b17 = f"{byte17:08b}"
    return b17[0:]

def range2(byte17):
    b17 = f"{byte17:08b}"
    return b17[0:]

def bitstring_to_bytes(s):
    v = int(s, 2)
    b = bytearray()
    while v:
        b.append(v & 0xff)
        v >>= 8
    return bytes(b[::-1])

def extract_acon(byte1, byte2):
    bin1 = f"{byte1:08b}"
    bin2 = f"{byte2:08b}"

    highlighted = bin1[2:8] + bin2[:1]
    return bitstring_to_bytes(highlighted)[0]


onlyfiles = [f for f in listdir("../captured_data/ze0/runtimehistory") if isfile(join(
    "../captured_data/ze0/runtimehistory", f))]
onlyfiles.sort()
dtrows = []
for file in onlyfiles:
    with open(f"captured_data/ze0/runtimehistory/{file}", "rb") as f:
        data = f.read()
        r1 = range1(data[155])
        r2 = range2(data[157])
        bs1 = bitstring_to_bytes(r1)[0]
        bs2 = bitstring_to_bytes(r2)[0]
        rngacon = extract_acon(data[169], data[170])
        dtrows.append(f"{int(bs1)};{int(bs2)}")
        print(r1, r2, bs1, bs2, rngacon)


