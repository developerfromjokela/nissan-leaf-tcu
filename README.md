# Nissan Leaf TCU reverse engineering
[![](https://img.shields.io/github/sponsors/developerfromjokela?label=Sponsor&logo=GitHub)](https://github.com/sponsors/developerfromjokela)
[![BuyMeACoffee](https://raw.githubusercontent.com/pachadotdev/buymeacoffee-badges/main/bmc-donate-yellow.svg)](https://www.buymeacoffee.com/developerfromjokela)
[![Patreon](https://img.shields.io/endpoint.svg?url=https%3A%2F%2Fshieldsio-patreon.vercel.app%2Fapi%3Fusername%3Ddeveloperfromjokela%26type%3Dpatrons)](https://patreom.com/developerfromjokela)
[![Liberapay patrons](https://img.shields.io/liberapay/patrons/developerfromjokela?style=plastic&logo=liberapay&label=liberapay&link=https%3A%2F%2Fliberapay.com%2Fdeveloperfromjokela%2F)](https://liberapay.com/developerfromjokela/)


This project is dedicated for reverse-engineering and finding how to communicate to TCU and issue commands/receive data. Project aims to bring TCU back to life using custom server.

Info in this repository:
- TCU firmware (06.27: ZE0 vehicles, 06.42: AZE0 vehicles)
- Protocol used in server communication
  - Data capture for analysis
  - Documentation
- CAN documentation for configuring TCU parameters
- Android app to configure TCU connection settings with OBD adapter

> 
> If you're looking for the server code (opencarwings), check out [this repo.](https://github.com/developerfromjokela/opencarwings)
> 

## Honorable mentions
- Huge thanks to Mickey and the team for shining a light on TCU's inner workings, [DEF CON 25 - Mickey Shkatov, Jesse Michael, Oleksandr Bazhaniuk - Driving down the rabbit hole](https://www.youtube.com/watch?v=5QBOmr_ZyLo)
- [majbthrd for providing UART pinout](https://mynissanleaf.com/threads/tcu-teardown.34309/)
- [Useful information regarding TCU unit](https://mynissanleaf.ru/viewtopic.php?id=966)
