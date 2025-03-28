# Communication with TCU

TCU supports two types of communication: TCP server connection and ACP SMS.

## Server Connection

TCU waits for SMS "NISSAN_EVIT_TELEMATICS_CENTER" to be received. After that, it connects to the server programmed into the device, with port number 55230.

After opening the connection, it sends a INIT request which contains auth info, TCU info and location info. It expects a command response, and timeouts about 20-30 seconds if no valid command response is sent and closes the connection.

Documentation is in files starting with GDC (codename for Global Data Center, used internally at Nissan/Renault).

## ACP SMS (TODO)

Purpose: SVTApp (Stolen Vehicle Tracking), FOTA (Firmware Over-The-Air update), ACPApp.   
Functions (not tested, no analysis on protocol):
- Immobilizer control: prevent car from turning on
- Journey history
- EV info
- Configuration
- TCU disable command
