.section .text
.arm
B	start
@ ---------------------------------------------------------------------------
	.long 0, 0, 0, 0, 0, 0, 0, 0, 0
	.ascii "SIEMENS_BOOTCODE"
	.long 3, 0
	.long 0, 0, 0, 0 @ bootkey
	.long 0, 0, 0, 0
@ ---------------------------------------------------------------------------
start:
	@ disable interrupts
	MRS	R0, CPSR
	ORR	R0, R0,	#0xC0
	MSR	CPSR_c,	R0

	@ enable write to einit-protected registers
	MOV	R0, #0
	BL	set_einit

	@ disable first watchdog
	LDR	R1, =0xF4400000
	LDR	R0, [R1,#0x7C] @ SCU_ROMAMCR
	BIC	R0, R0,	#1
	STR	R0, [R1,#0x7C] @ SCU_ROMAMCR
	MOV	R0, #8
	STR	R0, [R1,#0x28] @ SCU_ROMAMCR

	@ disable write to einit-protected registers
	MOV	R0, #1
	BL	set_einit

	BL  delay_short
	BL  delay_short
	BL  delay_short
	BL  delay_short
	BL  delay_short
	@ send ACK
	MOV	R0, #0xA5
	BL	tx_byte
	BL  delay_short
	BL  delay_short
	BL  delay_short
	BL  delay_short
	BL  delay_short

    @ Automatically start memory dump
    B   cmd_auto_read
@ ---------------------------------------------------------------------------
startwait:
	BL	rx_byte
	CMP	R0, #0x41 @ 'A'
	BEQ	stopwait
	CMP	R0, #0x2E @ '.'
	BLEQ	serve_watchdog
	B	startwait
@ ---------------------------------------------------------------------------
stopwait:
	BL	ebu_init_sdram
	BL	read_info
@ ---------------------------------------------------------------------------
cmd_ping:
	MOV	R0, #0x52 @ 'R'
	BL	tx_byte
@ ---------------------------------------------------------------------------
cmd_auto_read:
    LDR    R6, =0xA1000000       @ Start address, increment manually and recompile
    MOV    R9, #0x10000 @ Total size: 3 MB
    MOV    R0, #0xA5
    BL     tx_byte
    BL     delay_short

read_loop:
    MOV    R7, #8                @ 8 bytes per chunk
    MOV    R8, #0                @ Checksum

    MOV    R0, #0xAA
    BL     tx_byte
    BL     delay_short
    MOV    R0, #0x55
    BL     tx_byte
    BL     delay_short
    MOV    R0, #0xDB             @ Debug: Start marker sent
    BL     tx_byte
    BL     delay_short

chunk_loop:
    LDRB   R0, [R6], #1
    EOR    R8, R8, R0
    BL     tx_byte
    BL     delay_short
    SUBS   R7, R7, #1
    BNE    chunk_loop

    MOV    R0, #0xDB             @ Debug: 8 bytes sent
    BL     tx_byte
    BL     delay_short

    SUBS   R9, R9, #8            @ Decrease total size by 8
    BLE    end_dump              @ If R9 <= 0, end dump

    MOV    R0, #0x55
    BL     tx_byte
    BL     delay_short
    MOV    R0, #0xAA
    BL     tx_byte
    BL     delay_short

    MOV    R0, R8
    BL     tx_byte
    BL     delay_short

    MOV    R0, #0xDB             @ Debug: Checksum sent
    BL     tx_byte
    BL     delay_short

    B      read_loop             @ Continue

end_dump:
    MOV    R0, #0x55
    BL     tx_byte
    BL     delay_short
    MOV    R0, #0xAA
    BL     tx_byte
    BL     delay_short

    MOV    R0, R8
    BL     tx_byte
    BL     delay_short

    MOV    R0, #0xDB             @ Debug: Last checksum sent
    BL     tx_byte
    BL     delay_short

    MOV    R0, #0x45             @ 'E'
    BL     tx_byte
    BL     delay_short
    MOV    R0, #0x4E             @ 'N'
    BL     tx_byte
    BL     delay_short
    MOV    R0, #0x44             @ 'D'
    BL     tx_byte
    BL     delay_short

    B      loop

delay_short:
    PUSH   {R1, LR}
    LDR    R1, =1000
delay_loop:
    SUBS   R1, R1, #1
    BNE    delay_loop
    POP    {R1, PC}
@ ---------------------------------------------------------------------------
loop:
	BL	rx_byte
	CMP	R0, #0x41 @ 'A' ping
	BEQ	cmd_ping
	CMP	R0, #0x48 @ 'H' set baudrate
	BEQ	cmd_speed
	CMP	R0, #0x49 @ 'I' get info
	BEQ	cmd_info
	CMP	R0, #0x51 @ 'Q' quit
	BEQ	cmd_quit
	CMP	R0, #0x54 @ 'T' test for FFs
	BEQ	cmd_test
	CMP	R0, #0x52 @ 'R' read block
	BEQ	cmd_read
	CMP	R0, #0x46 @ 'F' write block
	BEQ	cmd_flash
	CMP	R0, #0x43 @ 'C' read CFI
	BEQ	read_cfi
	CMP	R0, #0x2E @ '.' keepalive
	BLEQ	serve_watchdog
	B	loop
@ ---------------------------------------------------------------------------
cmd_test:
	BL	rx_r6_r7

ct_l:
	LDMIA	R6!, {R0,R1}
	AND	R0, R0,	R1
	CMN	R0, #1
	MOVNE	R0, #0
	BNE	ct_e
	SUBS	R7, R7,	#8
	BNE	ct_l

ct_e:
	BL	tx_byte
	B	loop
@ ---------------------------------------------------------------------------
read_cfi:
	BL	rx_r6_r7
	BL	sub_920
	SUBS	R6, R10, #0x10
	STR	R0, [R6]
	MOV	R7, #4
	B	i_loop
@ ---------------------------------------------------------------------------
cmd_read:
	BL	rx_r6_r7
	MOV	R8, #0

r_loop:
	LDRB	R0, [R6],#1
	EOR	R8, R8,	R0
	BL	tx_byte
	SUBS	R7, R7,	#1
	BNE	r_loop
	MOV	R0, #0x4F
	BL	tx_byte
	MOV	R0, #0x4B
	BL	tx_byte
	MOV	R0, R8
	BL	tx_byte
	MOV	R0, #0
	BL	tx_byte
	B	loop
@ ---------------------------------------------------------------------------
cmd_speed:
	BL	rx_byte
	MOV	R6, R0
	MOV	R0, #0x68
	BL	tx_byte
	MOV	R0, #0xA00

cs_w:
	SUBS	R0, R0,	#1
	BNE	cs_w
	MOV	R0, R6
	BL	set_speed

cs_a:
	@ waiting for 'A',
	@ not serving watchdog
	BL	rx_byte
	CMP	R0, #0x41 @ 'A'
	BNE	cs_a
	@ answer: OK
	MOV	R0, #0x48 @ 'H'
	BL	tx_byte
	B	loop
@ ---------------------------------------------------------------------------
cmd_info:
	@ flash info block
	MOV	R6, R10
	MOV	R7, #0x80

i_loop:
	LDRB	R0, [R6],#1
	BL	tx_byte
	SUBS	R7, R7,	#1
	BNE	i_loop
	B	loop
@ ---------------------------------------------------------------------------
cmd_flash:
	BL	rx_r6_r7
	MOV	R4, #0xA8000000
	MOV	R5, R7
	MOV	R8, #0

cf_r:
	BL	serve_watchdog
	BL	rx_byte
	EOR	R8, R8,	R0
	STRB	R0, [R4],#1
	SUBS	R5, R5,	#1
	BNE	cf_r
	BL	rx_byte
	CMP	R0, R8
	BEQ	check_bounds

	@ checksum error
	MOV	R0, #0xBB
	BL	tx_byte
	MOV	R0, #0xBB
	BL	tx_byte

	B	loop

check_bounds:
	MOV	R0, #0x40000
	SUB	R0, R0, #1

	MOV	R1, R6
	ADD	R2, R6,	R7
	SUB	R2, R2,	#1
	BIC	R1, R1,	R0
	BIC	R2, R2,	R0
	CMP	R1, R2
	BEQ	check_flash_vendor

	@ bounds error
	MOV	R0, #0xFF
	BL	tx_byte
	MOV	R0, #0xFF
	BL	tx_byte

	B	loop

check_flash_vendor:
	MOV	R8, #0xA8000000
	LDRH	R0, [R10,#0x50]
	@ Intel (0089:xxxx) or ST (0020:xxxx)
	CMP	R0, #0x89
	CMPNE	R0, #0x20
	BEQ	erase_intel
	@ AMD (0001:xxxx or 0004:xxxx)
	CMP	R0, #1
	CMPNE	R0, #4
	BEQ	erase_amd
	@ uknown flash vendor error
	MOV	R0, #0xCC
	BL	tx_byte
	MOV	R0, #0xCC
	BL	tx_byte
	LDRH	R0, [R10,#0x50]
	BL	tx_byte
	LDRH	R0, [R10,#0x52]
	BL	tx_byte
	B	loop

erase_intel:
	@ ERASE_BEGIN
	MOV	R0, #1
	BL	tx_byte
	MOV	R0, #1
	BL	tx_byte
	MOV	R0, #0x60 @ Block to lock/unlock/lock-down
	STRH	R0, [R6]
	MOV	R0, #0xD0 @ Block Unlock
	STRH	R0, [R6]
	NOP
	MOV	R0, #0x20 @ BlockEraseSetup
	STRH	R0, [R6]
	MOV	R0, #0xD0 @ BlockEraseConfirm
	STRH	R0, [R6]

i_el:
	BL	serve_watchdog
	MOV	R0, #0x70 @ StatusRead
	STRH	R0, [R6]
	LDRH	R0, [R6]
	TST	R0, #0x80
	BEQ	i_el
	TST	R0, #0x3A
	BNE	i_err
	MOV	R0, #0xFF @ ArrayMode
	STRH	R0, [R6]

	@ ERASE_ACK
	MOV	R0, #2
	BL	tx_byte
	MOV	R0, #2
	BL	tx_byte

	LDRB	R0, [R10,#0x55]
	MOV	R3, #1
	MOV	R3, R3,LSL R0
	MOV	R3, R3,LSR#1
	MOV	R4, R6
	MOV	R5, R7,LSR#1

i_xwl:
	BL	serve_watchdog
	LDRB	R0, [R10,#0x56]
	CMP	R0, #2
	MOVNE	R0, #0xE8 @ Buffer Prog.Setup (Intel)
	MOVEQ	R0, #0xE9 @ Buffer Prog.Setup (ST)
	STRH	R0, [R4]

	@ send word count
	SUB	R0, R3,	#1
	STRH	R0, [R4]

	@ sending data
	MOV	R1, R3
i_sl:
	LDRH	R0, [R8],#2
	STRH	R0, [R4],#2
	SUBS	R1, R1,	#1
	BNE	i_sl
	MOV	R0, #0xD0 @ confirm
	STRH	R0, [R6]

i_wl:
	LDRH	R0, [R6]
	TST	R0, #0x80
	BEQ	i_wl
	TST	R0, #0x3A
	BNE	i_err
	SUBS	R5, R5,	R3
	BHI	i_xwl
	MOV	R0, #0xFF
	STRH	R0, [R6]

	@ FLASH_WRITE_ACK
	MOV	R0, #3
	BL	tx_byte
	MOV	R0, #3
	BL	tx_byte
	B	flash_checksum

i_err:
	@ write error
	MOV	R0, #0x50
	STRH	R0, [R6]
	MOV	R0, #0xFF
	STRH	R0, [R6]
	BL	tx_byte
	MOV	R0, #0xFF
	BL	tx_byte
	B	loop

erase_amd:
	@ ERASE_BEGIN
	MOV	R0, #1
	BL	tx_byte
	MOV	R0, #1
	BL	tx_byte

	MOV	R1, R6
	MOV	R0, #0xE0 @ ????
	BL	send_flash_cmd
	MOV	R0, #0xA0
	STRH	R0, [R1]
	MOV	R0, #1
	STRH	R0, [R1]
	MOV	R0, #0x90
	STRH	R0, [R1]
	MOV	R0, #0
	STRH	R0, [R1]
	MOV	R1, R6
	MOV	R0, #0x80 @ SectorEraseCmd1
	BL	send_flash_cmd
	MOV	R0, #0x30 @ SectorEraseCmd2
	BL	send_flash_cmd

wait_for_erase_amd:
	BL	serve_watchdog
	LDR	R0, [R6]
	CMN	R0, #1
	BNE	wait_for_erase_amd

	@ ERASE_ACK
	MOV	R0, #2
	BL	tx_byte
	MOV	R0, #2
	BL	tx_byte

	MOV	R1, R6
	MOV	R0, #0x20 @ Unlock Bypass Entry
	BL	send_flash_cmd
	MOV	R4, R6
	MOV	R5, R7,LSR#1

a_xwl:
	BL	serve_watchdog
	MOV	R0, #0xA0 @ Unlock Bypass Program
	STRH	R0, [R6]
	LDRH	R0, [R8]
	STRH	R0, [R4]

a_wl:
	LDRH	R1, [R4]
	CMP	R1, R0
	BNE	a_wl
	ADD	R8, R8,	#2
	ADD	R4, R4,	#2
	SUBS	R5, R5,	#1
	BNE	a_xwl
	MOV	R0, #0x90
	STRH	R0, [R6]
	MOV	R0, #0
	STRH	R0, [R6]
	MOV	R0, #0xF0
	STRH	R0, [R6]

	@ FLASH_WRITE_ACK
	MOV	R0, #3
	BL	tx_byte
	MOV	R0, #3
	BL	tx_byte
	B	flash_checksum

flash_checksum:
	MOV	R8, #0
	MOV	R1, R6
	MOV	R2, R7,LSR#1

cks_l:
	LDRH	R0, [R1],#2
	ADD	R8, R8,	R0
	SUBS	R2, R2,	#1
	BNE	cks_l

	@ send checksum
	MOV	R0, R8
	BL	tx_byte
	MOV	R0, R8,LSR#8
	BL	tx_byte

	@ send OK
	MOV	R0, #0x4F @ 'O'
	BL	tx_byte
	MOV	R0, #0x4B @ 'K'
	BL	tx_byte

	B	loop
@ ---------------------------------------------------------------------------
set_speed:
	ADR	R1, speeds_list
	LDR	R1, [R1,R0,LSL#2]
	MOV	R2, #0xF1000000 @ USART0_CLC
	MOV	R0, R1,LSR#16
	STR	R0, [R2,#0x14] @ USART0_BG
	MOV	R0, R1,LSL#16
	MOV	R0, R0,LSR#16
	STR	R0, [R2,#0x18] @ USART0_FDV
	BX	LR

speeds_list:
	.long 0x001901d8 @ 57600/0x1d8=122.033898*0x1a=3172.881348
	.long 0x000c01d8 @ 115200/0x1d8=244.067797*13=3172.881361
	.long 0x000501b4 @ 230400/0x1b4=528.440367*0x6=3170.642202
	.long 0x00000092 @ 460800/0x92=3156.164384
	.long 0x000000c3 @ 614400/0xc3=3150.769231
	.long 0x00000127 @ 921600/0x127=3124.067797
	.long 0x0000018a @ 1228800/0x18a=3118.781726
	.long 0x00000000 @ 1600000/0x200=3125
	.long 0x000001d0 @ 1500000/0x1d0=3232.758621
@ ---------------------------------------------------------------------------
tx_byte:
	MOV	R2, #0xF1000000
	LDR	R1, [R2,#0x20] @ USART0_TXB
	BIC	R1, R1,	#0xFF
	ORR	R1, R0,	R1
	STR	R1, [R2,#0x20] @ USART0_TXB

tx_w:
	LDR	R1, [R2,#0x68] @ USART0_FCSTAT
	ANDS	R1, R1,	#2
	BEQ	tx_w
	LDR	R1, [R2,#0x70] @ USART0_ICR
	ORR	R1, R1,	#2
	STR	R1, [R2,#0x70] @ USART0_ICR
	B	serve_watchdog
@ ---------------------------------------------------------------------------
rx_byte:
	MOV	R1, #0xF1000000
	LDR	R0, [R1,#0x68] @ USART0_FCSTAT
	ANDS	R0, R0,	#4
	BEQ	rx_byte
	LDR	R0, [R1,#0x70] @ USART0_ICR
	ORR	R0, R0,	#4
	STR	R0, [R1,#0x70] @ USART0_ICR
	LDR	R0, [R1,#0x24] @ USART0_RXB
	AND	R0, R0,	#0xFF
	BX	LR
@ ---------------------------------------------------------------------------
rx_word:
	MOV	R3, LR
	BL	rx_byte
	MOV	R4, R0,LSL#24
	BL	rx_byte
	ORR	R4, R4,	R0,LSL#16
	BL	rx_byte
	ORR	R4, R4,	R0,LSL#8
	BL	rx_byte
	ORR	R0, R4,	R0
	BX	R3
@ ---------------------------------------------------------------------------
rx_r6_r7:
	MOV	R5, LR
	BL	rx_word
	MOV	R6, R0
	CMP	R6, #0x10000000
	ADDCS	R6, R0,	R9
	BL	rx_word
	MOV	R7, R0
	BX	R5
@ ---------------------------------------------------------------------------
set_einit:
	LDR	R3, =0xF4400000
	LDR	R1, [R3,#0x24] @ SCU_WDTCON0
	BIC	R1, R1,	#0xE
	ORR	R1, R1,	#0xF0
	LDR	R2, [R3,#0x28] @ SCU_WDTCON1
	AND	R2, R2,	#0xC
	ORR	R1, R1,	R2
	STR	R1, [R3,#0x24] @ SCU_WDTCON0
	BIC	R1, R1,	#0xD
	ORR	R1, R1,	#2
	ORR	R0, R0,	R1
	STR	R0, [R3,#0x24] @ SCU_WDTCON0
	BX	LR
@ ---------------------------------------------------------------------------
sub_608:
	ADD	R1, R5,	#0x31
	LDR	R0, =0x522600
	STR	R1, [R6]
	STR	R0, [R6,#0x40]
	MOV	R0, #0x98
	ADD	R1, R5,	#0xA00
	STRH	R0, [R1,#0xAA]
	NOP
	LDRH	R0, [R5,#0x20]
	CMP	R0, #0x51
	BNE	loc_6E4
	LDRH	R0, [R5,#0x22]
	CMP	R0, #0x52
	BNE	loc_6E4
	LDRB	R0, [R10,#0x54]
	ADD	R0, R0,	#1
	STRB	R0, [R10,#0x54]
	LDRH	R0, [R5,#0x54]
	STRB	R0, [R10,#0x55]
	LDRH	R0, [R5,#0x28]
	STRB	R0, [R10,#0x56]
	LDRB	R2, [R10,#0x57]
	ADD	R2, R10, R2,LSL#2
	ADD	R2, R2,	#0x58
	ADD	R3, R5,	#0x5A

loc_66C:
	LDRH	R0, [R3],#2
	STRB	R0, [R2],#1
	AND	R0, R3,	#0xFF
	CMP	R0, #0x7A @ 'z'
	BNE	loc_66C
	LDRH	R0, [R5,#0x58]
	LDRB	R2, [R10,#0x57]
	ADD	R2, R2,	R0
	STRB	R2, [R10,#0x57]
	LDRH	R0, [R5,#0x4E]
	CMP	R0, #0x18
	BCC	loc_6E4
	ADDEQ	R1, R5,	#0x1000000
	ADDEQ	R2, R5,	#0x31
	CMP	R0, #0x19
	ADDEQ	R1, R5,	#0x2000000
	ADDEQ	R2, R5,	#0x21
	CMP	R0, #0x1A
	ADDEQ	R1, R5,	#0x4000000
	ADDEQ	R2, R5,	#0x11
	CMP	R0, #0x1B
	BHI	loc_6E4
	ADDEQ	R1, R5,	#0x8000000
	ADDEQ	R2, R5,	#1
	MOV	R0, #0xFF
	STRH	R0, [R5]
	STR	R2, [R6]
	MOV	R5, R1
	CMP	R0, R0
	BX	LR

loc_6E4:
	MOV	R0, #0
	STR	R0, [R6]
	CMP	R0, #1
	BX	LR

ebu_init_sdram:
	MOV	R7, LR
	MOV	R9, #0x10000000
	MOV	R1, #0xF0000000

	MOV	R0, #0xA8000000
	ADD	R0, R0, #0x41

	STR	R0, [R1,#0x88] @ ADDRSEL1
	LDR	R0, =0x30720200
	STR	R0, [R1,#0xC8] @ BUSCON1
	MOV	R0, #6
	STR	R0, [R1,#0x40] @ SDRMREF0
	LDR	R0, =0x891C70
	STR	R0, [R1,#0x50] @ SDRMCON0
	MOV	R0, #0x23
	STR	R0, [R1,#0x60] @ SDRMOD0

ebu_init_flash:
	@ init flash in EBU
	MOV	R1, #0xF0000000
	LDR	R0, =0xA0000011
	STR	R0, [R1,#0x80] @ ADDRSEL0
	LDR	R0, =0x522600
	STR	R0, [R1,#0xC0] @ BUSCON0

	@ read phone info
	LDR	R2, =0x534C
	LDR	R0, =0xA0000200
	LDRH	R1, [R0,#2]
	CMP	R1, R2
	BNE	loc_774
	LDRH	R8, [R0]
	LDR	R4, =0xA000065C @ IMEI
	LDR	R5, =0xA0000238 @ HASH
	LDR	R6, =0xA0000210 @ phone info
	ANDS	R0, R8,	#2
	ADDNE	R4, R4,	#4
	ADDNE	R5, R5,	#4
	B	loc_794

loc_774:
	LDR	R0, =0xA0001200
	LDRH	R1, [R0,#2]
	CMP	R1, R2
	BNE	loc_7BC
	LDRH	R8, [R0]
	LDR	R4, =0xA003E410 @ IMEI
	LDR	R5, =0xA003E400 @ HASH
	LDR	R6, =0xA003E000 @ phone info

loc_794:
	MOV	R1, R10
	MOV	R0, R6
	MOV	R2, #0x20
	BL	sub_960
	MOV	R0, R4
	MOV	R2, #0x10
	BL	sub_960
	MOV	R0, R5
	MOV	R2, #0x10
	BL	sub_960

loc_7BC:
	ADD	R1, R10, #0x40
	MOV	R2, #0x80
	MOV	R0, #0

loc_7C8:
	STR	R0, [R1],#4
	SUBS	R2, R2,	#4
	BNE	loc_7C8
	MOV	R1, #0xA0000000
	STR	R1, [R10,#0x40]
	STRH	R8, [R10,#0x48]
	MOV	R0, #0x90
	BL	send_flash_cmd
	LDRH	R0, [R1]
	STRH	R0, [R10,#0x50]
	BL	send_flash_reset
	LDRH	R0, [R1]
	LDRH	R2, [R10,#0x50]
	SUBS	R0, R0,	R2
	streqh	R0, [R10,#0x50]
	streqh	R0, [R10,#0x52]
	BEQ	loc_850
	MOV	R0, #0x90
	BL	send_flash_cmd
	LDRH	R0, [R1,#2]
	STRH	R0, [R10,#0x52]
	BL	send_flash_reset
	ADD	R5, R9,	#0xA0000000
	LDR	R6, =0xF0000080
	BL	sub_608
	BNE	loc_850
	CMP	R5, #0xB2000000
	MOVEQ	R9, #0x12000000
	BEQ	ebu_init_flash
	LDR	R6, =0xF0000090
	BL	sub_608
	BNE	loc_850
	LDR	R6, =0xF0000098
	BL	sub_608

loc_850:
	ADD	R1, R9,	#0xA0000000
	ADD	R5, R10, #0x80
	LDRH	R0, [R10,#0x50]
	CMP	R0, #0x89
	CMPNE	R0, #0x20
	ADDEQ	R1, R1,	#0x100
	ADDEQ	R1, R1,	#2
	MOVEQ	R0, #0x90
	MOVEQ	R4, #0x10
	BEQ	loc_88C
	CMP	R0, #1
	CMPNE	R0, #4
	BNE	loc_8E0
	MOV	R0, #0x88
	MOV	R4, #0x100

loc_88C:
	BL	send_flash_cmd
	MOV	R6, R4

loc_894:
	LDRH	R0, [R1],#2
	STRH	R0, [R5],#2
	SUBS	R6, R6,	#2
	BNE	loc_894
	ADD	R1, R9,	#0xA0000000
	MOV	R0, #0x90
	BL	send_flash_cmd
	MOV	R0, #0
	STRH	R0, [R1]
	BL	send_flash_reset
	LDR	R0, =0x3F7E532
	ADD	R2, R10, #0x80

loc_8C4:
	LDR	R1, [R2],#4
	CMN	R1, #1
	BEQ	loc_8DC
	EOR	R0, R0,	R1
	SUBS	R4, R4,	#4
	BNE	loc_8C4

loc_8DC:
	STR	R0, [R10,#0x44]

loc_8E0:
	BX	R7

read_info:
	MOV	R6, #0x3800
	LDR	R5, =0xEDB88320
	MOV	R4, #0

loc_8F0:
	MOV	R2, R4
	MOV	R3, #8

loc_8F8:
	TST	R2, #1
	EORNE	R2, R5,	R2,LSR#1
	MOVEQ	R2, R2,LSR#1
	SUBS	R3, R3,	#1
	BNE	loc_8F8
	STR	R2, [R6],#4
	ADD	R4, R4,	#1
	CMP	R4, #0x100
	BCC	loc_8F0
	BX	LR

sub_920:
	MOV	R0, #0xFFFFFFFF
	MOV	R5, #0x3800
	ADD	R2, R6,	R7

loc_92C:
	LDR	R1, [R6],#4
	MOV	R4, #4

loc_934:
	EOR	R3, R1,	R0
	MOV	R1, R1,LSR#8
	AND	R3, R3,	#0xFF
	LDR	R3, [R5,R3,LSL#2]
	EOR	R0, R3,	R0,LSR#8
	SUBS	R4, R4,	#1
	BNE	loc_934
	CMP	R6, R2
	BNE	loc_92C
	SUB	R6, R6,	R7
	BX	LR

sub_960:
	LDRB	R3, [R0],#1
	STRB	R3, [R1],#1
	SUBS	R2, R2,	#1
	BNE	sub_960
	BX	LR
@ ---------------------------------------------------------------------------
send_flash_cmd:
	MOV	R3, #0xAA
	ADD	R2, R1,	#0xA00 @ 0xA0000A00
	STRH	R3, [R2,#0xAA] @ 0xA0000AAA

	MOV	R3, #0x55
	ADD	R2, R1,	#0x500 @ 0xA0000500
	STRH	R3, [R2,#0x54] @ 0xA0000554

	ADD	R2, R1,	#0xA00 @ 0xA0000A00
	STRH	R0, [R2,#0xAA] @ 0xA0000AAA

	BX	LR
@ ---------------------------------------------------------------------------
send_flash_reset:
	MOV	R0, #0xF0
	STRH	R0, [R1]
	MOV	R0, #0xFF
	STRH	R0, [R1]
	BX	LR
@ ---------------------------------------------------------------------------
init_watchdog:
	LDR	R0, =0xF4300000
	MOV	R2, #0x100
	STR	R2, [R1]
	LDR	R0, =0xF4400000
	LDR	R0, [R0,#0x60]
	MOV	R0, R0,LSR#8
	AND	R0, R0,	#0xFF
	LDR	R1, =0xF4300118
	SUB	R2, R1,	#0xCC
	CMP	R0, #0x14
	BNE	iwd_1a
	ADD	R1, R1,	#0x60
	ADD	R2, R2,	#4

iwd_1a:
	ADD	R3, R2,	#4
	SUB	R4, R3,	#0xC
	MOV	R12, R1
	MOV	R0, #1
	STR	R0, [R2]
	MOV	R0, #0x10
	STR	R0, [R3]
	MOV	R0, #0x500
	STR	R0, [R1]

	MOV     R0, #0x4000
	ORR     R0, R0, #0x510

	STR	R0, [R4]
	LDR	R1, =0xF4B00000
	LDR	R11, [R1,#0x20] @ STM_4
	B	switch_watchdog
@ ---------------------------------------------------------------------------
serve_watchdog:
	@ read timer
	LDR	R2, =0xF4B00000
	LDR	R0, [R2,#0x20] @ STM_4
	SUB	R1, R0,	R11
	CMP	R1, #0x200
	BCC	swd_exit
	@ save timer value
	MOV	R11, R0
	B	switch_watchdog
@ ---------------------------------------------------------------------------
cmd_quit:
	BL	switch_watchdog
@ ---------------------------------------------------------------------------
switch_watchdog:
	LDR	R0, [R12]
	MOV	R0, R0,LSL#22
	LDR	R2, [R12]
	MVN	R0, R0,LSR#31
	BIC	R2, R2,	#0x200
	AND	R0, R0,	#1
	ORR	R0, R2,	R0,LSL#9
	STR	R0, [R12]
@ ---------------------------------------------------------------------------
swd_exit:
	BX	LR
@ ---------------------------------------------------------------------------
.end
