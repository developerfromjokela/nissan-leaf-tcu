LD=arm-none-eabi-ld
AS=arm-none-eabi-as
OBJCOPY=arm-none-eabi-objcopy

all: clean chaos_tcu

chaos_tcu:
	$(AS) -c -march=armv5te -mcpu=arm926ej-s chaos_tcu.S -o chaos_tcu.o
	$(LD) chaos_tcu.o -o chaos_tcu.elf  -T "../lib/ld/intram.ld"
	$(OBJCOPY) -O binary chaos_tcu.elf chaos_tcu.bin
	perl bin2hex.pl chaos_tcu.bin > chaos_tcu.hex

clean:
	rm *.elf *.bin *.hex *.o 2>/dev/null || true
