# CAN TCU Configuration
Documentation on TCU Commands supported by TCU, for configuration

- ECU TXID = 746
- ECU RXID = 783

## Common commands
- Retrieve configuration value: `21 XX` (XX = Field ID)
- Write configuration value: `3B XX YY...` (XX = Field ID, YY = Field Value)

#### Data response:
`61 13 XX YYYY`
- `61` = Configuration value
- `13` = Configuration ID
- `XX` = Connection Profile number (1 as default)
- after that comes the field value

#### Configuration Write
When writing Connection profile to TCU, packet must be populated with enough zero bytes for it to go through.
Each connection profile field has 128-bytes of space. Unused bytes must be zero.

When writing Global fields, there is no requirement to fill zero bytes.

Example (writing APN Name to "internet"): 
```hex
3B 13 01 69 6E 74 65 72 6E 65 74 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
```

## Global TCU Fields

### VIN
Field ID: `0x81`

Content:
- [0-19 bytes] = VIN
- [20-22 bytes] = CRC (usually just zeroed out)

### TCU Activation status
Field ID: `0x81`

Content:
- Byte 3, offset 7: 1 = ON, 2 = OFF

0x82
WRONG RESPONSE : Unknown(618261470000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00FFFFFFFFFFFFFFFF)

0x83

61 83 33 4E 44 30 44 01 44 03 18 33 35 31 34 38 06 1B 00 00 00 00 00 00 00 80 

### GSM Signal info
Field ID: `0x09`

Content:
- [0] = Antenna level (RSSI?)
- [1] = Reception power
- [2] = Error rate

## PPP Dial

Field ID: `0x10`  
Field type: ASCII

Example:
```hex
2A 39 39 23 0000000000000000000000000000000000000000
```
```text
*99#
```

## PPP Username
Field ID: `0x11`  
Field type: ASCII

Example :
```hex
7A 65 72 6F 00000000000000000000000000000000000000000000000000000000
```
```text
zero
```

## PPP Password
Field ID: `0x12`  
Field type: ASCII

Example :
```hex
65 6D 69 73 73 69 6F 6E 000000000000000000
```
```text
emission
```

## APN Name
Field ID: `0x13`  
Field type: ASCII

Example :

```hex
67 64 63 2E 6E 69 73 73 61 6E 2E 63 78 6E 00000000000000000000000000000000000000000000000000000000000000000000
```
```text
gdc.nissan.cxn
```

## DNS1
Field id: `0x14`  
Field type: ASCII

DNS1 IP address

Example:
```hex
61 75 74 6F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
```
```text
auto
```


## DNS2
Field id: `0x15`  
Field type: ASCII

DNS2 IP address

Example:
```hex
61 75 74 6F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
```
```text
auto
```



## Proxy Name
Field id: `0x16`  
Field type: ?

Proxy hostname (no idea what kind of proxy, HTTP?)

Nullbytes


## Proxy port
Field id: `0x17`  
Field type: ?

Port number of proxy

Nullbytes


## PDP Connection Type
Field id: `0x18`  
Field type: ASCII

Data connection mode (`IP` by default). I guess used for modem's AT+CGDCONT configuration



## Server Hostname
Field id: `0x19`
Field type: ASCII

Example:

```hex
6E 69 73 73 61 6E 2D 65 75 2D 64 63 6D 2D 62 69 7A 2E 76 69 61 61 71 2E 65 75
```
```text
nissan-eu-dcm-biz.viaaq.eu
```

## Connection Point
Field id: `0x1C`  
Field Type: ?

```hex
33 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20
```

## Access Point Info
Field id: `0x1D`  
Field Type: ?

```hex
30 20 20 20202020202020202020202020
```

## PPP Auth Logic
Field id: `0x1E`  
Field Type: ?

```hex
31 20 20 20202020202020202020202020
```


