# SIM Slot Mod — Troubleshooting

Companion to [SIMSLOT.md](SIMSLOT.md): that file covers doing the mod, this one covers what to do when it's done and the
TCU still won't go online.

Facts below marked *(measured)* come from a debug session on a 2018 ZE1 40 kWh, Gen2 4G.

Start with the [quick check](#quick-check-with-tcu-sim-watchsh). It needs only a USB cable and answers "is it
registered, does SMS arrive, why don't remote commands work". The numbered sections after it are the detailed
hardware/OBD/DIAG procedure for when the quick check points at the board.

## Quick check with `tcu-sim-watch.sh`

[`tcu-sim-watch.sh`](tcu-sim-watch.sh) wraps the USB AT, NMEA and DIAG ports (§4, §8) in three commands.

### Before you start

- **Car on.** The TCU drops off USB when the car sleeps; `/dev/ttyUSB0..4` must exist.
- **Port access:** your user needs read/write on `/dev/ttyUSB*` (`dialout` group or the desktop seat ACL).
- **Stop ModemManager:** `sudo systemctl stop ModemManager`. It grabs the TCU as a modem, pokes the ports, and makes
  QCSuper try to escalate via `pkexec`/`sudo` (password prompt that may fail with no terminal or agent).
- `sms-diag` needs `tshark` and [uv](https://docs.astral.sh/uv/) (runs QCSuper via `uvx`, nothing installed), or set
  `QCSUPER_PYTHON` to a Python that has `qcsuper`.

### Commands

| Command                       | Port                  | What it does                                                             |
|-------------------------------|-----------------------|--------------------------------------------------------------------------|
| `./tcu-sim-watch.sh net`      | `ttyUSB3`             | Polls `AT+COPS?` / `AT+CGACT?` every `$INTERVAL` s, prints on change     |
| `./tcu-sim-watch.sh sms-diag` | `ttyUSB0`             | Passive SMS sniff: QCSuper → pcap → tshark, one row per SMS              |
| `./tcu-sim-watch.sh debug`    | `ttyUSB3` + `ttyUSB2` | Sends `ATENABLEDEBUG`, streams the firmware log, saves `tcu-debug-*.log` |
| `./tcu-sim-watch.sh selftest` |                       | Checks the parsers                                                       |

Ports are overridable with `AT_PORT`, `DIAG_PORT` and `NMEA_PORT`. All three commands can run side by side.

### Firmware debug log

`ATENABLEDEBUG` on an AT port (`OK`) makes the TCU write its internal Legato log into the `ttyUSB2` NMEA stream;
`ATDISABLEDEBUG` turns it off. Log lines never start with `$`, so `debug` drops the NMEA sentences and shows only the
log. Re-enabling (`ATDISABLEDEBUG` then `ATENABLEDEBUG`) is said to separate the two on some units; `debug` does that,
but the filter is what makes it reliable. Tip from the opencarwings developer: anything the TCU refuses gets logged
here.

Useful lines:

| Line                                                        | Meaning                                               |
|-------------------------------------------------------------|-------------------------------------------------------|
| `[MDM] ModemDataMng SetUpProf 1 APN: <apn>`                 | APN the TCU uses for its own data session             |
| `[MDM] … start session error: other failures for prof 1`    | Network refused that session — usually the APN        |
| `[MDM] TimeoutsAndRetries: Tryouts expired for profile [1]` | TCU gave up for now, retries later                    |
| `SMS RX Handler. Received B message from <number> '…'`      | Binary SMS reached the application                    |
| `[SMS]OK: source of the SMS VALID` / `[SMS]OK: HMAC VALID`  | Sender and signature accepted                         |
| `Executing SMS` → `Adding action [Server: 0xF6]`            | Command accepted and queued for the server            |
| `Connection state is 0`                                     | No data session — queued command can't be fetched yet |

### Reading `net`

| Output                                      | Meaning                                                                |
|---------------------------------------------|------------------------------------------------------------------------|
| `REGISTERED <operator> LTE \| …`            | SIM read, registered. Access tech from `AT+COPS?` (`LTE` = 7)          |
| `LIMITED (camped, not registered)`          | RF fine, SIM not read or rejected → §5, §6                             |
| `NO SERVICE`                                | No cell at all — antenna, or modem not up                              |
| `no active bearer (TCU connects on demand)` | **Not a fault.** The TCU only opens data when it has something to send |

`AT+CGACT?` returning a bare `OK` does not mean data is broken. The server's "last connection" timestamp (e.g. in
opencarwings) is the real data test.

### SMS over USB

- **Stored SMS can't be read over USB:** `AT+CMGL`, `AT+CMGR`, `AT+CPMS`, `AT+CSMS?`, `AT+CNMI` all `ERROR`;
  `$SMSLIST`, `$GETNUMSMS`, `$GET_SMS_MEMORY` get no answer at all. Needs the UART (§7).
- `sms-diag` works for incoming SMS. Incoming SMS arrive as NAS Downlink Transport over LTE and decode cleanly. Output
  columns:
  `time | sender | DCS | text`. DCS `0` = GSM-7 text, `4` = 8-bit binary, `8` = UCS-2. Binary SMS show empty text; dump
  them with `tshark -r diag-*.pcap -Y gsm_sms -x`.

QCSuper writes its pcap through an 8 KB buffer, so on a quiet link nothing reaches disk for minutes. The script runs
QCSuper with unbuffered binary writes so rows show up immediately.

### Debugging remote commands

A remote command (AC, charge) is: server sends a **binary** ACP wake SMS, HMAC-SHA1 signed → TCU checks sender +
signature → TCU connects to the server and fetches the command. Periodic uploads already reaching the server prove the
last step; test the rest in order with `sms-diag` and `debug` running:

1. **Control:** text the car's number from a phone. A row with DCS `0` and your text must appear. If not, this carrier
   delivers SMS some way the DIAG capture doesn't decode (e.g. IMS) — go UART.
2. **Command:** send AC from opencarwings. Expect a row with **DCS `4`**, 74 bytes of user data (`82 03 00 32 …`, your
   VIN in ASCII, 20-byte HMAC at the end — same layout as [`genhmac.py`](genhmac.py)).
3. **Provider test without opencarwings:** send `payload` `"AID/"` (= `00 80 FF`) with `encoding` `BINARY` straight to
   the provider's API. Must arrive as DCS `4`, `00 80 ff`.

**DCS `8` with every byte ≥ `0x80` turned into `FFFD`** means the SMS provider decoded the binary payload as UTF-8 text
and resent it as UCS-2. opencarwings sends a correct request (`payload` base64 + `encoding: BINARY`), so the fix is on
the provider side: get it to deliver `TP-DCS 0x04`, or pick a provider that does. Don't enable an "always use GSM 03.38"
style option — that is a text setting and makes it worse. The TCU can only reject a corrupted message, so whether the
default HMAC key is accepted can only be tested once step 3 passes.

Once the SMS arrives intact, the `debug` log shows the TCU's decision: `HMAC VALID` and `Executing SMS` mean the wake
SMS is fine (the default opencarwings key is accepted on this unit). If it's followed by `Connection state is 0`
and `start session error … prof 1`, the remaining problem is the APN of the TCU's data profile — check which APN
`SetUpProf 1` names and fix that profile (§3, §9).

opencarwings caveat (as of 2026-10): in `tculink/coordinators/ficosa2016.py` a custom per-car HMAC key is parsed with
`bytes.fromhex(hmac_key)` instead of `car.hmac_key`, so setting one breaks every command. Leave it empty.

---

# Detailed diagnostics

## 1. Not faults

| Symptom                                         |                                                                                                                                                                                                                    |
|-------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ICCID in NissanConnect still shows the old eSIM | ~Normal, never updates. Confirmed by @smachj, @junior013, @albertbm in [#15](https://github.com/developerfromjokela/nissan-leaf-tcu/discussions/15). Don't use as a test.~ It's possible to correct ICCID, use "Retrieve SIM information" routine in configuration app                                           |
| NissanConnect services never return             | Nissan shut the AZE0/ZE1 backend down **2026-03-30**. Target [opencarwings](https://github.com/developerfromjokela/opencarwings).                                                                                  |
| Remote commands (AC, charge) don't work         | Unsolved on ZE1 — activation SMS is HMAC-SHA1 signed, key not yet rewritable over OBD. Periodic messages do get through. Check binary SMS delivery first: [Debugging remote commands](#debugging-remote-commands). |

Crossed-out car icon **is** real: no cell registration.

## 2. Which TCU

|            | 2018–2019 (navi has SD slot)                 | 2020+ (no SD slot)    |
|------------|----------------------------------------------|-----------------------|
| TCU        | Ficosa / Advanced Automotive Antennas Gen2.5 | Continental AIVC v2.5 |
| Modem      | Sierra AirPrime **AR7554**, USB `1199:68ab`  | —                     |
| CAN TX/RX  | `746`/`783`                                  | `7CA`/`7DA`           |
| Session    | `10 C0`, or `10 FA` on some units            | `10 03`               |
| Read/write | `21 xx` / `3B xx`                            | `22 xxxx` / `2E xxxx` |

AAA makes the **TCU**; the modem is the shielded can on it. Board silkscreen
`TCU-2G-AR7-KAI` / `C60055513`. Revisions vary — @junior013 reported a different PCB.

## 3. Reading state over OBD

App profile *"TCU 2015-2019"*, or ddt4all profile `[DCM Nissan] N_H60B_DCM_20160331`.

> **Set ddt4all CAN timeout to 800 ms** (@junior013). Without it the TCU answers ~1 in 6
> and a healthy unit looks dead.

Field IDs confirmed on a 2018 ZE1, cross-checked against [
`FicosaGen2_5.kt`](../configapp-android/app/src/main/java/com/developerfromjokela/nissanleaftelematics/profiles/FicosaGen2_5.kt):

| Read              | Field                                          | Factory value                                 |
|-------------------|------------------------------------------------|-----------------------------------------------|
| `21 09`           | Signal (byte 0 antenna, 1 power, 2 error rate) |                                               |
| `21 81`           | VIN                                            |                                               |
| `21 04`           | TCU activation                                 |                                               |
| `21 10`           | APN (GDC)                                      | *empty*                                       |
| `21 2C`           | APN (TSP)                                      | `nissanvit.fr`                                |
| `21 14` / `21 13` | GDC server URL / port                          | `https://biz-tcu.nissan-carwings.com` / `443` |

Write is `3B <id> 01 <data>`; profile fields are 128 bytes and **unused bytes must be zero-padded** or the write is
silently rejected ([CAN.md](../CAN.md)).

## 4. Reading state over USB *(measured)*

Five nodes, `sierra` driver, USB interfaces 00–04:

| Node                 | Role                                                                            |
|----------------------|---------------------------------------------------------------------------------|
| `ttyUSB0`, `ttyUSB1` | DIAG — silent to `AT`, this is the QCSuper/QPST port                            |
| `ttyUSB2`            | NMEA — streams `$GPRMC`, `$GNGNS`, `$GPGGA`; firmware log after `ATENABLEDEBUG` |
| `ttyUSB3`, `ttyUSB4` | AT                                                                              |

**The AT port is Ficosa's application layer, not the Sierra modem:**

```
ATI → Manufacturer: Ficosa International / Model: TCU Gen2 Kai / Revision: CX.6_RC01
```

It replaces the modem's AT interpreter with a small curated subset and bare-`ERROR`s the rest — including every SIM
command and all Sierra `AT!` vendor commands.

- **Works:** `ATE`, `ATI`/`ATI3`/`ATI9`, `AT+GMM`, `AT+COPS?`, `AT+CGDCONT?`, `AT+CGACT?`, `ATENABLEDEBUG`/
  `ATDISABLEDEBUG`
- **`ERROR`:** `AT+CPIN?`, `AT+CSQ`, `AT+CIMI`, `AT+CCID?`, `AT+CFUN?`, `AT+CREG?`,
  `AT+CGREG?`, `AT+CEER`, `AT+CGATT?`, `AT+CPAS`, `AT+CLAC`, `AT&V`, `AT+CMEE`, `AT+GMI`,
  `AT+GMR`, `AT+CGSN`, `AT+CNUM`, `AT!ENTERCND`, `AT!GSTATUS?`, and all `$`/`AT$` forms

**So SIM state cannot be read over USB.** Use the UART (§7) or DIAG (§8).

What the two working commands give:

```
AT+COPS?     → +COPS: 0,0,"Limited Service",0
AT+CGDCONT?  → 1,"IP","nissanvit.fr"   2,"IP","nissancnt.fr"   3,"IPV4V6",""
```

**`Limited Service`** = camped on a cell but not registered, emergency only. It means the **RF path is healthy**
(antenna, receiver, cell search all work) and the failure is at SIM/registration — usually the SIM not being read,
though a network-rejected SIM does the same. `AT+CGDCONT?` confirms the factory APNs are intact.

## 5. Measuring the SIM interface

VSIM is **not a standing rail** — it is asserted only during a card-activation attempt (VCC up → CLK → RST released →
card must answer with an ATR). A DMM at 2–3 Hz reads 0.00 V and tells you nothing. Use a scope single-shot, or DMM
MIN/MAX armed before power-up. Expect **1.8 V or ~2.95 V**.

The firmware latches `[SIM]DTC SIM failure = 1, SIM initialization stops`, so after a failed boot the TCU may stop
retrying entirely for that power cycle.

| Pin        | Unpowered                                                   | During the VCC pulse                                                 |
|------------|-------------------------------------------------------------|----------------------------------------------------------------------|
| C1 VCC     | high-R to GND (a few Ω = short, regulator folds back)       | VSIM                                                                 |
| C2 RST     | high-R                                                      | ≈ VSIM after clock starts                                            |
| C3 CLK     | high-R                                                      | ≈ **half** VSIM (DMM averaging ~3.25 MHz); full VSIM or 0 = no clock |
| **C5 GND** | **~0 Ω to board ground** — probe the contact, not the shell | 0 V                                                                  |
| C7 I/O     | high-R                                                      | ≈ VSIM idle high                                                     |

C4/C6/C8 unused. `gen2/README.md:24` notes some pads are ground-only and genuinely unused — treating the *required*
ground pin as one of those leaves the card floating while everything else measures perfectly.

The **MFF2 eUICC footprint sits beside the holder footprint on the same nets**
([img/s4.jpeg](img/s4.jpeg)). Identify its pads functionally (GND = 0 Ω; VCC = has the pulse and the decoupling cap;
CLK = half VSIM; RST = rises after CLK; I/O = the remainder), then measure **resistance holder C7 → MFF2 I/O pad**.
Healthy is well under 1 Ω. Probing VCC at both places separates "modem not driving" from "link broken".

## 6. Worked diagnostic log

2018 ZE1 40 kWh 4G, consumer SIM, PIN disabled. Symptom: crossed-out car icon.

| Measurement                     | Result                                    | Eliminates                                     |
|---------------------------------|-------------------------------------------|------------------------------------------------|
| VCC steady-state                | 0 V                                       | nothing — DMM was missing the pulse            |
| VCC on power-up                 | **pulses periodically**                   | low-power state; latched DTC; broken VCC trace |
| C1 → GND                        | **150 kΩ**                                | VSIM short                                     |
| C5 → board ground               | **0 Ω**                                   | missing ground return                          |
| C3 during pulse                 | **≈ half VSIM**                           | no-clock fault                                 |
| All pins → GND                  | high-R                                    | shorts                                         |
| Springs, card size, orientation | OK                                        | card not contacting                            |
| VSIM peak                       | **~2.8 V**                                | 1.8 V/3 V class mismatch                       |
| SIM in a phone                  | registers, no PIN                         | dead card, PIN lock                            |
| `AT+COPS?`                      | **`Limited Service`**                     | RF path, antenna, receiver                     |
| **C7 (I/O) during pulse**       | **shorter pulse, lower level than VCC**   | — *the remaining anomaly*                      |
| DIAG capture, 35 s *(measured)* | **32 L3 messages, 100 % RR, zero MM/GMM** | SIM being read but network-rejected            |

**Conclusion.** The modem powers the card at a valid level, clocks it, releases reset and gets no ATR, so it retries.
Power, ground, clock, RF, card and card geometry are all confirmed good. The card is electrically absent from the
modem's point of view, and the only anomaly left is the I/O line.

The DIAG capture confirms this at the baseband level (§8): the modem camps on a GSM cell and decodes broadcast traffic,
but **never initiates registration**. A modem with a readable SIM sends a Location Updating / Attach Request within
seconds; one whose SIM the network rejects sends it and gets a Reject with a cause code. Neither happened — no MM or GMM
message at all. That is the signature of **no readable SIM**, not a subscription or network problem. The fault is on the
board.

A *shorter* window on I/O is expected — it only idles high between RST release and deactivation. The **lower level** is
the finding: I/O should idle at essentially VSIM.

### After the rework — resolved

Holder desoldered, flux properly removed, I/O path inspected and repaired, re-soldered. Re-measured *(measured)*:

|                   | Before                                   | After                                            |
|-------------------|------------------------------------------|--------------------------------------------------|
| `AT+COPS?`        | `0,0,"Limited Service",0`                | **`0,0,"One HU",0`** — registered                |
| DIAG, L3 messages | 32 in 35 s, **100 % RR**, zero MM/GMM/SM | 461 in 60 s, incl. **SM PDP-context signalling** |

**The SIM interface is fixed.** The card is read, the modem registers on a real network. Contamination and/or
rotary-tool damage on the I/O line was the fault, as suspected.

The remaining failure is no longer hardware:

```
SM: ACTIVATE PDP CONTEXT REQUEST     x3
SM: ACTIVATE PDP CONTEXT REJECT      x3   cause 33 = "Requested service option not subscribed"
```

The network accepts the attach but refuses packet data for this subscription. Registered on **GSM/2G** (`AT+COPS?`
access technology `0`), so the modem is asking for GPRS data. Leading causes, in order:

1. **2G/GPRS data not provisioned on the SIM.** Most modern consumer plans are LTE-first and many no longer carry 2G
   packet data — which yields exactly cause 33.
2. **Wrong APN for the carrier.** Note cause 33 rather than 27 ("missing or unknown APN")
   suggests the APN is recognised but not subscribed, so this is the weaker theory.
3. **No data allowance on the plan at all.**

Initial bench tests ran with **no antenna connected**, which explained the 2G camping: 2G has a far better link budget
than LTE, so an antenna-less module still finds a GSM cell on stray pickup while LTE, needing much better SNR, is
unreachable. Band support was never the issue — three LTE bands matched the carrier.

### With antennas connected *(measured)*

|             | No antenna            | Antennas on                                                                                 |
|-------------|-----------------------|---------------------------------------------------------------------------------------------|
| `AT+COPS?`  | `"One HU",0` — GSM/2G | **`"One HU",7`** — E-UTRAN/LTE                                                              |
| RRC         | —                     | 3 connection setups, 25 UL-DCCH + 15 DL-DCCH — real dedicated signalling                    |
| GNSS        | no fix                | **valid fix, 7 satellites**                                                                 |
| `AT+CGACT?` | —                     | bare `OK`, no context listed → no bearer *at that moment* (see quick check, not conclusive) |

NAS capture (`qcsuper --pcap-dump x.pcap --decrypt-nas`, 60 s) gives the whole story:

```
Auth Request -> Auth Response -> Security Mode Command -> Security Mode Complete
PDN CONNECTIVITY REQUEST -> PDN CONNECTIVITY REJECT   cause 33
   ... 11x
```

**The SIM is definitively fine.** Completing authentication and security mode means the card ran the
mutual-authentication algorithm with the network using its Ki — which is impossible over a marginal SIM interface. The
hardware repair is fully confirmed.

**The failure is now entirely carrier-side:** ESM cause 33, *"Requested service option not subscribed"* — the network
authenticates the subscriber but has no matching APN in the subscription profile. Causes, in order:

1. **Wrong APN string.** The HSS has no entry for the requested APN for this subscriber.
   `AT+CGDCONT?` shows context 1 = `internet`; use the carrier's exact documented APN.
2. **No data allowance on the plan.**
3. **Subscription restricted to a specific APN.**

Quickest resolution: put the SIM in a phone, let it auto-configure, confirm data works and note the exact APN it uses,
then write that to TCU context 1 over OBD (`3B 10`). If data fails in the phone too, the SIM has no usable data
subscription — get one that does, which is why the community uses IoT SIMs.

### Prime suspects (historical — this was the fault)

The rework history is material here: the eUICC was removed with a **rotary tool**, not hot air, and **flux was never
cleaned off**.

1. **Conductive flux residue.** Uncleaned RA/RMA or water-soluble flux is conductive and hygroscopic — leakage in the
   tens-to-hundreds of kΩ. Fits exactly: CLK is push-pull and shrugs it off, I/O is weak open-drain with a pull-up and
   gets dragged down. Residue trapped under a holder that can't sit flat is the textbook case, and it grows dendrites
   under bias.
2. **Rotary-tool damage.** Cut or thinned traces near the footprint, abraded solder mask, conductive grinding debris. A
   partly severed I/O trace conducting through debris reads *resistive*, not open — again a low idle level rather than a
   dead line.
3. **Damaged/missing passive in the I/O path.** Compare it against the equivalents on CLK and RST — usually the same
   value.
4. **Leaky ESD clamp.** Compare C7 → GND against C2 and C3. "All high" isn't enough; C7 materially *lower* points here.

### Rework hazards

|                                 |                                                                                                         |
|---------------------------------|---------------------------------------------------------------------------------------------------------|
| Rotary tool to remove the eUICC | cut traces, abraded mask, conductive debris. [SIMSLOT.md](SIMSLOT.md) specifies **hot air, 420–450 °C** |
| Leaving flux on the board       | conductive, hygroscopic leakage; worst on weakly-pulled lines like SIM I/O                              |

Cleaning: flux remover or 99% IPA, stiff brush, several passes lifting residue rather than smearing, compressed air,
full dry. Residue *under* the holder is what matters and a surface wipe won't reach it — desolder the holder, clean the
footprint, re-solder.

**Do not inject external 3 V into SIM VCC.** The net isn't isolated from the modem's UIM interface, and it can't work
anyway — the modem drives activation, so an externally powered card it hasn't activated still never enumerates. If a pad
genuinely lacks VCC, jumper from the MFF2 VCC pad, which is the module's own regulated VSIM.

## 7. Finding the UART

Not documented anywhere. [img/s1.jpeg](img/s1.jpeg) shows two rows of **unpopulated plated through-holes** — ~10 beside
the Renesas R7F7010263, ~12 along the bottom edge. Classic factory debug header position; start there.

TX idles logic-high and bursts at power-up; RX is the adjacent pin that idles high but stays silent. Try 115200 first.

> ⚠️ **Measure the idle-high level before connecting anything.** Sierra UARTs are typically
> **1.8 V**; the 2.8 V on VSIM says nothing about the UART rail. A 3.3 V — let alone 5 V —
> adapter can damage the module. Use a level shifter, ground first.

Worth it, because `$SIM_STATUS`, `$SIM_INFO`, `$IMSI`, `$DTC`, `$RSSI` and
`$DBGTCU,1` / `$DBGLOG,1` boot tracing all work there and none work over USB. (The runtime firmware log *is*
available over USB via `ATENABLEDEBUG` — see the quick check — but not the SIM/IMSI queries or boot trace.) Firmware log
strings to expect:

| String                                               | Meaning                               |
|------------------------------------------------------|---------------------------------------|
| `[SIM]SIM present` / `[SIM]SIM initialized`          | working                               |
| `SIM removed: Cannot be initialized`                 | activation failed                     |
| `[SIM]SIM PIN error`                                 | PIN problem                           |
| `[SIM]SIM Net Lock`                                  | modem operator-locked                 |
| `[SIM]DTC SIM failure = 1, SIM initialization stops` | TCU has given up for this power cycle |

## 8. DIAG port *(measured)*

`ttyUSB0` bypasses the Ficosa layer and talks to the Qualcomm baseband.
[QCSuper](https://github.com/P1sec/QCSuper) (`pip install qcsuper`) is a passive logger — reads diagnostic frames,
writes nothing.

```sh
qcsuper --usb-modem /dev/ttyUSB0 --info
qcsuper --usb-modem /dev/ttyUSB0 --pcap-dump diag.pcap
```

`--info` → baseband **MDM9615**, firmware build `M9615A-CETWTDZM-A7101900`, built 2016-12-25. (`ttyUSB1` times out; use
`ttyUSB0`.)

`--pcap-dump` is the useful one — it settles "SIM not read" vs "SIM rejected" without touching the board. Decode the
GSMTAP payloads and count L3 protocol discriminators:

| What you see                               | Meaning                                                     |
|--------------------------------------------|-------------------------------------------------------------|
| RR only (paging, SI, immediate assignment) | camped and listening, **never registers** → no readable SIM |
| MM/GMM Location Updating or Attach Request | SIM *is* being read                                         |
| …followed by a Reject + cause code         | SIM read but refused by the network                         |

Measured here, **broken SIM interface**: 32 messages in 35 s — 27 Paging Request Type 1, 3 Immediate Assignment, SI
2quater, SI 13. **Zero MM/GMM.** All broadcast traffic meant for other subscribers.

**After repair**: 461 messages in 60 s including `ACTIVATE PDP CONTEXT REQUEST` /
`REJECT` — i.e. the modem now registers and attempts data. See §6.

Decoding note: group frames by GSMTAP **type** first. Type `0x02` (ABIS) carries clean GSM L3 and decodes directly; type
`0x01` (GSM_UM) carries L2-framed data and will produce nonsense if parsed as L3.

Note the capture was cut short by the port being preempted, consistent with the TCU resetting the modem on its
SIM-failure retry loop.

## 9. Once the card is detected

1. TCU APN over OBD: `3B 10` (GDC) and `3B 2C` (TSP) — set both. Server `3B 14` / `3B 13`.
2. Navi SD card APN, separately: <https://opencarwings.viaaq.eu/static/navi_guide2.html>
   (40 kWh procedure differs from 30 kWh).
3. Power-cycle the TCU (fuse #34).

The TCU also needs **binary/data SMS**, which many consumer plans drop silently — hence the community's use of IoT SIMs
(Hologram, Elisa IoT).

## 10. Known gaps

- Gen2 UART pinout (the only documented one is for the 2G Continental TCU).
- SIM pad pinout / schematic for either Gen2 board.
- VSIM class for the AR7554 in this application.
- Whether `10 C0` or `10 FA` is the correct diagnostic session — appears to vary by unit.
