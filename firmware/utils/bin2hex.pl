#!/bin/perl
use warnings;
use strict;

open F, "<".$ARGV[0];
binmode F;
while (!eof(F)) {
	read F, my $buf, 1024;
	print bin2hex($buf);
}
close F;

sub bin2hex {
	my $hex = shift;
	$hex =~ s/([\W\w])/sprintf("%02X", ord($1))/ge;
	$hex =~ s/(.{64})/$1\n/g;
	return $hex;
}
