# Nissan Leaf TCU reverse engineering
This project is dedicated for reverse-engineering and finding how to communicate to TCU and issue commands/receive data. Project aims to bring TCU back to life using custom server.

Info in this repository:
- TCU firmware (06.27: ZE0 vehicles, 06.42: AZE0 vehicles)
- Protocol used in server communication
  - Data capture for analysis
  - Documentation
- CAN documentation for configuring TCU parameters
- Android app to configure TCU connection settings with OBD adapter


## Honorable mentions
- Huge thanks to Mickey and the team for shining a light on TCU's inner workings, [DEF CON 25 - Mickey Shkatov, Jesse Michael, Oleksandr Bazhaniuk - Driving down the rabbit hole](https://www.youtube.com/watch?v=5QBOmr_ZyLo)
- [majbthrd for providing UART pinout](https://mynissanleaf.com/threads/tcu-teardown.34309/)
- [Useful information regarding TCU unit](https://mynissanleaf.ru/viewtopic.php?id=966)
