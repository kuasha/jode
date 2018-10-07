#!/usr/bin/perl
# createStackDelta.pl Copyright (C) 1999 Jochen Hoenicke.
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2, or (at your option)
# any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; see the file COPYING.  If not, write to
# the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
#
# $Id: createStackDelta.pl 1269 2000-10-02 13:04:09Z hoenicke $

# This perl script creates the stackDelta string needed by
# jode.bytecode.Instruction.

my $OPCODEFILE="jode/bytecode/Opcodes.java";
my @delta;
my $lastOpcode = 0;
my $nr;

open OPCODES, "<$OPCODEFILE";
while (<OPCODES>) {
    next unless /opc_([a-z0-9_]+)\s*=\s*(\d+)/;
    
    $_  = $1;
    $nr = $2;
    if (/^(nop|iinc)$/) {
	# no pop, no push
	$delta[$nr] = "000";
    } elsif (/^([aif](const|load).*|[sb]ipush|ldc(_w)?)$/) {
	# no pop, one push
	$delta[$nr] = "010";
    } elsif (/^([ld](const|load).*|ldc2_w)$/) {
	# no pop, two push
	$delta[$nr] = "020";
    } elsif (/^([aif]store.*|pop)$/) {
	# one pop, no push
	$delta[$nr] = "001";
    } elsif (/^([ld]store.*|pop2)$/) {
	# two pop, no push
	$delta[$nr] = "002";
    } elsif (/^[aifbcs]aload$/) {
	# two pop, one push
	$delta[$nr] = "012";
    } elsif (/^[dl]aload$/) {
	# two pop, two push
	$delta[$nr] = "022";
    } elsif (/^[aifbcs]astore$/) {
	# three pop, no push
	$delta[$nr] = "003";
    } elsif (/^[dl]astore$/) {
	# four pop, no push
	$delta[$nr] = "004";
    } elsif (/^dup(2)?(_x([12]))?$/) {
	$count = $1 ? 2 : 1;
	$depth = $2 ? $3 : 0;
	$pop   = $count + $depth;
	$push  = $pop + $count;
	$delta[$nr] = "0".$push.$pop;
    } elsif (/^swap$/) {
	# two pop, two push
	$delta[$nr] = "022";
    } elsif (/^[if](add|sub|mul|div|rem|u?sh[lr]|and|or|xor)$/) {
	# two pop, one push
	$delta[$nr] = "012";
    } elsif (/^[ld](add|sub|mul|div|rem|and|or|xor)$/) {
	# four pop, two push
	$delta[$nr] = "024";
    } elsif (/^[if]neg$/) {
	# one pop, one push
	$delta[$nr] = "011";
    } elsif (/^[ld]neg$/) {
	# two pop, two push
	$delta[$nr] = "022";
    } elsif (/^lu?sh[lr]$/) {
	# 3 pop, two push
	$delta[$nr] = "023";
    } elsif (/^[if]2[ifbcs]$/) {
	# one pop, one push
	$delta[$nr] = "011";
    } elsif (/^[if]2[ld]$/) {
	# one pop, two push
	$delta[$nr] = "021";
    } elsif (/^[ld]2[if]$/) {
	# two pop, one push
	$delta[$nr] = "012";
    } elsif (/^[ld]2[ld]$/) {
	# two pop, two push
	$delta[$nr] = "022";
    } elsif (/^fcmp[lg]$/) {
	$delta[$nr] = "012";
    } elsif (/^[ld]cmp[lg]?$/) {
	$delta[$nr] = "014";
    } elsif (/^if_[ia]cmp(eq|ne|lt|ge|le|gt)$/) {
	$delta[$nr] = "002";
    } elsif (/^(if(eq|ne|lt|ge|le|gt|(non)?null)|tableswitch|lookupswitch)$/) {
	# order does matter
	$delta[$nr] = "001";
    } elsif (/^(goto(_w)?|jsr(_w)?|ret|return)$/) {
	$delta[$nr] = "000";
    } elsif (/^([ifa]return)$/) {
	$delta[$nr] = "001";
    } elsif (/^([ld]return)$/) {
	$delta[$nr] = "002";
    } elsif (/^(new)$/) {
	$delta[$nr] = "010";
    } elsif (/^(multianewarray|(get|put|invoke).*)$/) {
	# unknown 
	$delta[$nr] = "100";
    } elsif (/^(athrow|monitor(enter|exit))$/) {
	$delta[$nr] = "001";
    } elsif (/^(a?newarray|arraylength|checkcast|instanceof)$/) {
	$delta[$nr] = "011";
    } else {
	# illegal
	next;
    }
    $lastOpcode = $nr 
	if ($nr > $lastOpcode);
}

print "    private final static String stackDelta = \n\t\"";
for ($nr = 0; $nr <= $lastOpcode; $nr ++) {
    defined $delta[$nr] or $delta[$nr] = "177";
    print "\\$delta[$nr]";
}
print "\";\n";
