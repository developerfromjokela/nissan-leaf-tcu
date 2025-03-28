# TCU Firmware (2G TCU, GNO1VN, EU-version from MY2011 Leaf, SW 06.27R)
TCU firmware on NOR-flash consists of two parts. Bootloader and firmware. Both are included in this folder

## Program type

- ARMv5t
- Little endian

## Dumper program (2G TCU only, with PMB8876 baseband)

how to dump:

1. use uart on TCU pins documented at https://mynissanleaf.com/threads/tcu-teardown.34309/, connect to USB TTL
2. https://github.com/siemens-mobile-hacks/pmb887x-dev/ repo as base
3. Copy utils files except perl script into boot folder and compile. 
4. Copy perl script into root of the project
5. 65536 bytes per run, because TCU reboots/crashes for unknown reason after a while and that's not enough to dump all in one go.
6. Recompile code with new offset and run again until about 6-7 MB of data from flash is extracted.
7. Join all chunks together. Binaries (firmware codes) in PMB8876 baseband used in leaf tcu start with magic CJKT. First code is the bootloader, second one is actual code.

Perl command: 
perl receiver.pl --device /dev/<serial> --boot chaos_tcu --as_hex

