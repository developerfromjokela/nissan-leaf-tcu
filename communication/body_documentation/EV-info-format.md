# GDC | EV Info

At end of type 3 response, you have a EV info packet, which is indicated by hex 0x04. This contains various information about car's state

Example: `04 00 2B 00 35 02 00 00 0B 0C 16 87 80 32 1A A3 43 C0 29 52`

Data is scrambled and packed into bits, likely to save in data transmission cost/speed

## Format
Format is documented in EVInfo spreadsheet. 

Currently documented for cars:
- ZE0 2011