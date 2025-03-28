#!/usr/bin/env perl
use warnings;
use strict;
use File::Basename;
use File::Slurp qw(read_file write_file);
use lib dirname(__FILE__).'/tools/lib';
use Sie::SerialPort;
use Sie::Utils;
use Sie::Boot;
use Time::HiRes;
use Getopt::Long;
use Time::HiRes qw|usleep time|;
no utf8;

my %STD_BOOTS = (
    BurninMode	=> "F104A0E3201090E5FF10C1E3A51081E3201080E51EFF2FE1040108000000000000000000000000005349454D454E535F424F4F54434F444501000700000000000000000000000000000000000000000001040580830003",
    ServiceMode	=> "F104A0E3201090E5FF10C1E3A51081E3201080E51EFF2FE1040108000000000000000000000000005349454D454E535F424F4F54434F4445010007000000000000000000000000000000000000000000010405008B008B",
    NormalMode	=> "F104A0E3201090E5FF10C1E3A51081E3201080E51EFF2FE1040108000000000000000000000000005349454D454E535F424F4F54434F444501000700000000000000000000000000000000000000000001040500890089",
);

$| = 1;

my $serial;

END { releasePort() };
$SIG{INT} = $SIG{TERM} = sub { releasePort(); exit(1); };

my %options = (
    device		=> "/dev/serial/by-id/usb-Prolific_Technology_Inc._USB-Serial_Controller-if00-port0",
    boot_speed	=> 115200,
    speed		=> 1600000,
    module		=> undef,
    as_hex		=> 0,
    picocom		=> 0,
    help		=> 0,
    ign			=> 1,
    boot		=> 'ServiceMode'
);

main();

sub main {
    GetOptions(
        "device=s"		=> \$options{device},
        "boot_speed=s"	=> \$options{boot_speed},
        "speed=s"		=> \$options{speed},
        "boot=s"		=> \$options{boot},
        "ign=s"			=> \$options{ign},
        "as_hex"		=> \$options{as_hex},
        "picocom"		=> \$options{picocom},
        "module=s"		=> \$options{module},
        "help=s"		=> \$options{help},
    );

    $serial = Sie::SerialPort->new($options{device});
    die("open port error (".$options{device}.")") if !$serial;

    $serial->setSpeed($options{boot_speed});

    my $boot_status = Sie::Boot::detectPhone($serial, $options{ign});
    die "Can't boot device!" if !$boot_status;

    my $bootcode = getBootCode($options{boot});
    die "Bootcode '".$options{boot}."' not found!" if !$bootcode;

    print "Sending bootcode '".$options{boot}."' to the phone...\n";
    my ($load_status, $err) = Sie::Boot::loadBootCode($serial, $bootcode);
    die $err if !$load_status;

    print "Bootcode loaded!\n";

    print "Bootcode loaded! Waiting for memory dump...\n";

    receive_dump5();
}

sub getBootCode {
    my ($name) = @_;

    return hex2bin($STD_BOOTS{$name}) if exists $STD_BOOTS{$name};

    if ($name =~ /^([\w\d_-]+)$/i) {
        my $precompiled_hex = dirname(__FILE__)."/boot/$name.hex";
        return readBootCode($precompiled_hex) if -f $precompiled_hex;

        my $precompiled_bin = dirname(__FILE__)."/boot/$name.bin";
        return readBootCode($precompiled_bin) if -f $precompiled_bin;
    }

    return readBootCode($name) if -f $name;
    return undef;
}

sub readBootCode {
    my ($path) = @_;
    print "Using boot code: $path (".(-s $path)." bytes)\n";
    if ($path =~ /\.hex$/i) {
        my $data = scalar(read_file($path));
        $data =~ s/\s+//g;
        return hex2bin($data);
    }
    return scalar(read_file($path));
}

sub releasePort {
    if ($serial) {
        $serial->dtr($options{ign} && $options{ign} == 2 ? 1 : 0);
        $serial->close;
        undef $serial;
    }
}

use strict;
use warnings;

sub crc16_ccitt {
    my ($data) = @_;
    my $crc = 0xFFFF;

    foreach my $char (split //, $data) {
        $crc ^= ord($char) << 8;
        for (my $i = 0; $i < 8; $i++) {
            if ($crc & 0x8000) {
                $crc = ($crc << 1) ^ 0x1021;
            } else {
                $crc <<= 1;
            }
            $crc &= 0xFFFF; # Keep it 16-bit
        }
    }
    return $crc;
}

use strict;
use warnings;

sub xor_checksum {
    my ($data) = @_;
    my $checksum = 0;
    foreach my $char (split //, $data) {
        $checksum ^= ord($char);
    }
    return $checksum;
}

sub receive_dump5 {
    my $bin_file = "flash.bin";
    open(my $fh, '>:raw', $bin_file) or die "Cannot open file '$bin_file': $!";

    my $total_received = 0;
    my $chunk_count = 0;
    my $timeout = 20000;

    print "Waiting for ACK (0xA5) to start dump...\n";
    my $ack_received = 0;
    while (!$ack_received) {
        my $c = $serial->getChar($timeout);
        if ($c < 0) { die "Timeout waiting for ACK!\n"; }
        if ($c == 0xA5) {
            $ack_received = 1;
            print "Received ACK, starting firmware dump...\n";
        }
    }

    while (1) {
        my $buffer = "";

        # Sync to start marker: 0xAA 0x55
        my $synced = 0;
        while (!$synced) {
            my $c1 = $serial->getChar($timeout);
            if ($c1 < 0) { die "Timeout waiting for start marker on chunk $chunk_count!\n"; }
            if ($c1 == 0xAA) {
                my $c2 = $serial->getChar($timeout);
                if ($c2 >= 0 && $c2 == 0x55) {
                    $synced = 1;
                }
            }
        }
        print "Synced to chunk $chunk_count start marker\n";

        # Read debug byte after AA 55
        my $debug1 = $serial->getChar($timeout);
        if ($debug1 < 0) { die "Timeout waiting for debug1 on chunk $chunk_count!\n"; }
        printf("Debug1: %02X\n", $debug1);

        # Read exactly 8 bytes
        my $byte_count = 0;
        while ($byte_count < 8) {
            my $c1 = $serial->getChar($timeout);
            if ($c1 < 0) {
                print "Chunk $chunk_count partial data: " . unpack("H*", $buffer) . "\n";
                die "Timeout reading chunk $chunk_count data!\n";
            }
            printf("%02X ", $c1);
            $buffer .= chr($c1);
            $byte_count++;
        }

        # Read debug byte after 8 bytes
        my $debug2 = $serial->getChar($timeout);
        if ($debug2 < 0) { die "Timeout waiting for debug2 on chunk $chunk_count!\n"; }
        printf("Debug2: %02X\n", $debug2);

        # Check end marker: 0x55 0xAA
        my $c1 = $serial->getChar($timeout);
        if ($c1 < 0 || $c1 != 0x55) {
            print "Chunk $chunk_count data: " . unpack("H*", $buffer) . "\n";
            die "Expected 0x55, got " . sprintf("%02X", $c1) . " after $byte_count bytes!\n";
        }
        printf("%02X ", $c1);
        my $c2 = $serial->getChar($timeout);
        if ($c2 < 0 || $c2 != 0xAA) {
            print "Chunk $chunk_count data: " . unpack("H*", $buffer) . "\n";
            die "Expected 0xAA, got " . sprintf("%02X", $c2) . " after 0x55!\n";
        }
        printf("%02X ", $c2);

        # Read checksum
        my $checksum_received = $serial->getChar($timeout);
        if ($checksum_received < 0) { die "Failed to read checksum for chunk $chunk_count!\n"; }
        printf("\nChecksum: %02X\n", $checksum_received);

        # Read debug byte after checksum
        my $debug3 = $serial->getChar($timeout);
        if ($debug3 < 0) { die "Timeout waiting for debug3 on chunk $chunk_count!\n"; }
        printf("Debug3: %02X\n", $debug3);

        my $computed_checksum = xor_checksum($buffer);

        if ($chunk_count == 0 || $chunk_count >= 37) {
            print "Chunk $chunk_count data: " . unpack("H*", $buffer) . "\n";
            print "Computed checksum: 0x" . sprintf("%02X", $computed_checksum) .
                ", Received checksum: 0x" . sprintf("%02X", $checksum_received) . "\n";
        }

        if ($computed_checksum != $checksum_received) {
            print "Chunk $chunk_count data: " . unpack("H*", $buffer) . "\n";
            die "Checksum mismatch in chunk $chunk_count! Expected: 0x" . sprintf("%02X", $checksum_received) .
                ", Computed: 0x" . sprintf("%02X", $computed_checksum) . ". Aborting!\n";
        }

        print $fh $buffer;
        $total_received += length($buffer);
        $chunk_count++;

        print "Chunk $chunk_count received & verified: " . length($buffer) . " bytes (Total: $total_received bytes)\n";

        # Check for END
        if ($debug3 == 0x45) {  # 'E' after last chunk
            my $next_c = $serial->getChar($timeout);
            if ($next_c >= 0 && $next_c == 0x4E) {  # 'N'
                my $last_c = $serial->getChar($timeout);
                if ($last_c >= 0 && $last_c == 0x44) {  # 'D'
                    print "Firmware dump completed. Total received: $total_received bytes.\n";
                    last;
                }
            }
        }
    }

    close($fh);
    print "Memory dump saved to '$bin_file' ($total_received bytes)\n";
}


sub receive_dump4 {
    my $bin_file = "flash.bin";
    open(my $fh, '>:raw', $bin_file) or die "Cannot open file '$bin_file': $!";

    my $total_received = 0;
    my $chunk_size = 1024;
    my $chunk_count = 0;

    print "Starting firmware dump...\n";

    while (1) {
        my $buffer = "";
        my $bytes_received = 0;

        # Read 1024-byte chunk
        while ($bytes_received < $chunk_size) {
            my $c = $serial->getChar(1);
            if ($c > -1) {
                $buffer .= chr($c);
                $bytes_received++;
            }
        }

        # Read 2-byte CRC
        my $crc_received = "";
        for (1..2) {
            my $c = $serial->getChar(1);
            if ($c > -1) {
                $crc_received .= chr($c);
            }
        }

        # Convert received CRC bytes
        my $crc_high = ord(substr($crc_received, 0, 1));
        my $crc_low  = ord(substr($crc_received, 1, 1));
        my $crc_value = ($crc_high << 8) | $crc_low;

        # Compute CRC for received data
        my $computed_crc = crc16_ccitt($buffer);

        if ($computed_crc != $crc_value) {
            die "CRC mismatch in chunk $chunk_count! Expected: 0x" . sprintf("%04X", $crc_value) .
                ", Computed: 0x" . sprintf("%04X", $computed_crc) . ". Aborting!\n";
        }

        # Write chunk to file only if CRC is valid
        print $fh $buffer;
        $total_received += $bytes_received;
        $chunk_count++;

        print "Chunk $chunk_count received & verified: $bytes_received bytes (Total: $total_received bytes)\n";

        # Wait for "OK"
        print "Waiting for OK...\n";
        my $ok_received = 0;
        while (!$ok_received) {
            my $c = $serial->getChar(1);
            last if $c < 0; # Handle timeout or disconnection
            if ($c == ord('O')) {
                my $next_c = $serial->getChar(1);
                if ($next_c == ord('K')) {
                    $ok_received = 1;
                    print "Received OK, requesting next chunk...\n";
                }
            }
        }

        # Check for "END"
        my $c = $serial->getChar(1);
        if ($c == ord('E')) {
            my $next_c = $serial->getChar(1);
            if ($next_c == ord('N')) {
                my $last_c = $serial->getChar(1);
                if ($last_c == ord('D')) {
                    print "Firmware dump completed. Total received: $total_received bytes.\n";
                    last;
                }
            }
        }
    }

    close($fh);
    print "Memory dump saved to '$bin_file' ($total_received bytes)\n";
}


sub receive_dump3 {
    my $bin_file = "flash.bin";
    open(my $fh, '>:raw', $bin_file) or die "Cannot open file '$bin_file': $!";

    my $total_received = 0;
    my $chunk_size = 1024;
    my $chunk_count = 0;

    print "Starting firmware dump...\n";

    while (1) {
        my $buffer = "";
        my $bytes_received = 0;

        # Read 1024-byte chunk
        while ($bytes_received < $chunk_size) {
            my $c = $serial->getChar(1);
            if ($c > -1) {
                $buffer .= chr($c);
                $bytes_received++;
            }
        }

        # Write chunk to file
        print $fh $buffer;
        $total_received += $bytes_received;
        $chunk_count++;

        print "Chunk $chunk_count received: $bytes_received bytes (Total: $total_received bytes)\n";

        # Wait for "OK"
        print "Waiting for OK...\n";
        my $ok_received = 0;
        while (!$ok_received) {
            my $c = $serial->getChar(1);
            last if $c < 0; # Handle timeout or disconnection
            if ($c == ord('O')) {
                my $next_c = $serial->getChar(1);
                if ($next_c == ord('K')) {
                    $ok_received = 1;
                    print "Received OK, requesting next chunk...\n";
                }
            }
        }

        # Check for "END"
        my $c = $serial->getChar(1);
        if ($c == ord('E')) {
            my $next_c = $serial->getChar(1);
            if ($next_c == ord('N')) {
                my $last_c = $serial->getChar(1);
                if ($last_c == ord('D')) {
                    print "Firmware dump completed. Total received: $total_received bytes.\n";
                    last;
                }
            }
        }
    }

    close($fh);
    print "Memory dump saved to '$bin_file' ($total_received bytes)\n";
}


sub receive_dump2 {
    my $bin_file = "flash.bin";
    open(my $fh, '>:raw', $bin_file) or die "Cannot open file '$bin_file': $!";

    my $total_received = 0;

    while (1) {
        my $buffer = "";
        my $bytes_received = 0;

        # Read 1024 bytes chunk
        while ($bytes_received < 1024) {
            my $c = $serial->getChar(1);
            if ($c > -1) {
                $buffer .= chr($c);
                $bytes_received++;
            }
        }

        # Write chunk to file
        print $fh $buffer;
        $total_received += $bytes_received;

        # Wait for "OK"
        my $ok_received = 0;
        while (!$ok_received) {
            my $c = $serial->getChar(1);
            last if $c < 0; # Handle timeout or disconnection
            if ($c == ord('O')) {
                my $next_c = $serial->getChar(1);
                if ($next_c == ord('K')) {
                    $ok_received = 1;
                    print "Chunk finished, "
                }
            }
        }

        # Check for "END"
        my $c = $serial->getChar(1);
        if ($c == ord('E')) {
            my $next_c = $serial->getChar(1);
            if ($next_c == ord('N')) {
                my $last_c = $serial->getChar(1);
                if ($last_c == ord('D')) {
                    print "Firmware dump completed. Received $total_received bytes.\n";
                    last;
                }
            }
        }
    }

    close($fh);
    print "Memory dump saved to '$bin_file' ($total_received bytes)\n";
}


sub receive_dump {
    my $bin_file = "flash.bin";
    open(my $fh, '>:raw', $bin_file) or die "Cannot open file '$bin_file': $!";

    my $buffer;
    my $bytes_received = 0;
    my $expected_size = 1024;  # 1024 bytes to receive

    while ($bytes_received < $expected_size) {
        my $c = $serial->getChar(1);
        if ($c > -1) {
            print $fh chr($c);  # Write byte to file
            $bytes_received++;
        }
    }

    close($fh);
    print "Memory dump saved to '$bin_file' ($bytes_received bytes)\n";

    # Wait for "OK" response to ensure complete transmission
    while (1) {
        my $c = $serial->getChar(1);
        last if $c == ord('O');
        last if $c == ord('K');
    }
}