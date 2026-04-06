# GEN 2 TCU made by Advanced Automotive Antennas

![tlm](https://github.com/user-attachments/assets/29154f46-ec7b-4c54-9304-e54f00cb935b)

TCU in Nissan Leaf 2016 and newer have shared TCU part with Renault.

30 kWh LEAF has 3G model, while 40 kWh has 4G. 

### Firmware
Extracted and unzipped firmware from update package. 
Base address is different for each firmware, 0x2A00000 for 283B0

### SIM Slot

Soldering a sim slot is needed, it does not come with one from factory.  

<img src="https://github.com/user-attachments/assets/2c85aa56-f7b3-409c-a7f5-5ee9c3a7cbf8" height="500px"/>

As you see the slot in this picture, top most right pin is ground of simslot(orange arrow), all other ones are data. The unsoldered pad on far left is also ground, both on top and bottom (blue arrow). Those ones are unused.
