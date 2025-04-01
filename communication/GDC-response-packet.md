# GDC response structure
Structure Data packets from TCU -> Server


# Common packet structure for Type 1 and 3

- [1] Packet type
- [2] Car identifier (02=ZE0, 92=AZE0)
- [3] Extra data flag?
- [4] Packet overall size (bytes, unsigned int) (NOTE: does not match on type 5 messages)
- [5-8] 20 5E B1 70 (Car model or configuration flags?)
- [9-83] Auth and info (Logon)
- [101] Request type (27 for Logon and 29 for charging cable reminder, 2C for AC Command, 28 for charge status info, 2a = AC or charging stop, 2b = charging, 56 = maybe error?, 2E = config read)
- [103-107] Time (unable to decode) and location info
- [118] Auth info length
- [119-153] Auth information
- [154] Body type (**only with Type 3 response**)
- [155-] Body (**only with Type 3 response**)
 

## TCU info and auth markers [9-153]
- 51(Q) = VIN
- 4C(L) (first) = TCU ID
- 4F(O): MSN (TCU reads MSN from SIM Card, can be blank if not available)
- 4C(L) (second) = Unit ID
- 54 (T) = SIM ICCID
- 4A (J) = TCU Ver?
- 50 (P) (first)=username
- 50 (P) (last)=password


## Packet types [2]

- 1 = Auth request
- 3 = Data response
- 5 = Configuration response


## Body types [154]
- 04: EV info