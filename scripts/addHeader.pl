#!/usr/bin/perl
# addHeader.pl Copyright (C) 1999 Jochen Hoenicke.
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
# $Id: addHeader.pl 1368 2002-05-29 12:07:39Z hoenicke $

# This perl script just adds the copyright header to all given java files,
# removing a possible previous header.


for (@ARGV) {
    my $file = $_;
    $file =~ m=([^/]*)\.java(\.in)?$= or do {
	print STDERR "$file is not a java file";
	next;
    };
    my $class = $1;
    my $curyear = `date +%Y`;
    chomp $curyear;
#    my $firstcheckin = `rlog $file 2>/dev/null |grep date| tail -1`;
#    my $firstyear =
#	($firstcheckin =~ m=date: ([0-9]+)/[0-9]+/[0-9]+=) ? $1 : $curyear;
#      my $lastcheckin = `rlog $file 2>/dev/null |grep date| head -1`;
#      my $lastyear =
#  	($firstcheckin =~ m=date: ([0-9]+)/[0-9]+/[0-9]+=) ? $1 : $curyear;
    open FILE, "<$file";
    while (<FILE>) {
	$firstyear = $1 if /Copyright \(C\) (\d{4})/;
	last if /^package/;
    }

    $firstyear = $curyear if ! $firstyear;
    my $lastyear = $curyear;
    my $years = ($firstyear == $lastyear) 
	? $firstyear : "$firstyear-$lastyear";
    my $lesser = "";
    my $dotlesser = "";

    if ($file =~ m!jode/(util|bytecode|jvm|flow|expr|decompiler
			 |GlobalOptions|AssertError)!x) {
	$lesser = " Lesser";
	$dotlesser = ".LESSER";
    }

    rename "$file", "$file.orig" or do {
	print STDERR "Can't open file $file\n"; 
	next;
    };
    open OLD, "<$file.orig";
    open NEW, ">$file";
    print NEW <<"EOF";
/* $class Copyright (C) $years Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU$lesser General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU$lesser General Public License
 * along with this program; see the file COPYING$dotlesser.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * \$Id\$
 */

EOF

    while (<OLD>) {
	/^package/ and last;
    }
    print NEW $_;
    while (<OLD>) {
	print NEW $_;
    }
}

