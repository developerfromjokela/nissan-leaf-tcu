"""
TCP server for testing purposes
"""
import socket
from datetime import datetime

_print=print

def print(*args, **kw):
    _print("[%s]" % (datetime.now()),*args, **kw)

def handle_tcucommoninfo(packet):
    return {
        "vehicle_code": packet[4:8],
        "vin": packet[9:26].decode('ascii').rstrip('\x00'),
        "tcu_id": packet[27:55].decode('ascii').rstrip('\x00'),
        "unit_id": packet[56:68].decode('ascii').rstrip('\x00'),
        "imei": packet[69:89].decode('ascii').rstrip('\x00'),
        "sw_version": packet[91:100].decode('ascii').rstrip('\x00'),
        "username": packet[120:136].decode('ascii').rstrip('\x00'),
        "pw_hash": packet[136:153].decode('ascii').rstrip('\x00'),
        "datapacket_type": packet[100]
    }


def handle_logonrequest(packet, socket):
    tcu_info = handle_tcucommoninfo(packet)

    print("TCU info:",tcu_info)

    response = bytes.fromhex(
      "02 00 00 08 28 02 00 90" # Get Charge Status
    )
    socket.send(response)
    print(f"Sent resp: {response.hex().upper()}")
    
def handle_datarequest(packet):
    tcu_info = handle_tcucommoninfo(packet)

    print("TCU info:",tcu_info)

    if tcu_info["datapacket_type"] == 0x2C:
        print("AC command result")
    if tcu_info["datapacket_type"] == 0x28:
        print("Charge status data")
    if tcu_info["datapacket_type"] == 0x29:
        print("Charging cable reminder")
    if tcu_info["datapacket_type"] == 0x2a:
        print("Charging or AC Stop")
    if tcu_info["datapacket_type"] == 0x2b:
        print("Charging Start")



with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_socket:
    server_socket.bind(("0.0.0.0", 55230))
    server_socket.listen(5)
    print(f"Listening for TCP connections on 55230...")

    while True:
        client_socket, client_address = server_socket.accept()
        print(f"Connection established with {client_address}")

        with client_socket:
            while True:
                data = client_socket.recv(1024)
                if not data:
                    print("NO DATA FROM CLIENT / CLIENT DISCONNECTED")
                    break
                print(data.hex())
                print(f"Received (TCP): {data.hex()}")
                if data.startswith(bytes.fromhex('01 02 00')):
                    print("EVIT LOGON request")
                    handle_logonrequest(data, client_socket)
                if data.startswith(bytes.fromhex('03 02 00')):
                    print("Data request")
                    handle_datarequest(data)