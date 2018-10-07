#!/usr/bin/perl -s -w
#
# javaDependencies Copyright (C) 1999 Jochen Hoenicke.
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
# $Id: javaDependencies.pl 1354 2001-11-19 14:36:47Z hoenicke $

# This scripts create Makefile dependencies out of class files.  It
# simply scans the constant pool of the class files, finding all
# references to other classes and adding a dependency to that class.
#
# It doesn't do a perfect job, since it can't handle dependencies to 
# constant values in different classes:  The compiler inlines the 
# constant and thus doesn't include a reference to the class.
#
# Usage:  
#  javaDependencies.pl -classpath <cp> [-dependdir <depdir> [-subdir <subdir>]]
#                      [-depfile <depfile>]
#                      <classfiles>
# 
#  cp:     colon separated paths to the java files we should depend on.
#  depdir: if set, use this path as path to the java files when printing
#          dependencies, not the path where the java files were found.
#          useful, if you want to make use of VPATH settings in Makefile.
#  subdir: if set, this is the path from depdir to the current directory. 
#          Use it to remove unneccessary ../../$subdir/ 
#  depfile: the name of the dependency file, default is "Makefile.dep".
#  class:  The class files (not inner classes) for which the dependencies
#          should be generated.  We will also look for inner and anon 
#          classes.

my $buff;

sub readInBuff ($) {
    my $count = $_[0];
    my $offset = 0;
    while ($count > 0) {
	my $result;
        $result = read FILE, $buff, $count, $offset or return 0;
	$offset += $result;
	$count -= $result;
    }
    $offset;
}

sub readUTF () {
    readInBuff 2 or die "Can't read UTF8 length";
    my $ulength = unpack("n", $buff) & 0xffff;
    return "" if $ulength == 0;
    readInBuff $ulength or die "Can't read UTF8 string $ulength";
    unpack("a$ulength", $buff);
}

$depfile = "Makefile.dep" if (!defined($depfile));
open DEPFILE, ">$depfile";
print DEPFILE <<EOF;
# This dependency file is automatically created by $0 from class files.
# Do not edit.

EOF
foreach $clazz (@ARGV) {
    next if $clazz =~ (/^.*\$.*\.class/);
    $clazz =~ /([^\$]*)(\$.*)?\.class/ or die "not a class file";
    $base = $1;
    my ($filename, %done);
    %done=();
    for $filename ($clazz, glob("$base\\\$*.class")) {
	open FILE, $filename;
	binmode FILE;

	readInBuff 8 or die "Can't read header";
	my ($magic, $major, $minor) = unpack("Nnn", $buff);

	die "Wrong magic $magic" if $magic != 0xcafebabe;
	die "Wrong major $major" if $major != 3;
	die "Wrong minor $minor" if $minor < 45;
	
	readInBuff 2 or die "Can't read cpool length";
	
	
	my ($length) = unpack("n", $buff) & 0xffff;

	my $number;
	my @strings = ();
	my @clazzes;
	for ($number = 1; $number < $length; $number++) {
	    
	    readInBuff 1 or die "Can't read constant tag";
	    my ($tag) = unpack("C", $buff);

	    #print STDERR "$number/$length: $tag";
	  tags:
	    for ($tag) {
		/^1$/ && do { 
		    # UTF 8
		    $strings[$number] = &readUTF();
		    #print STDERR ": $strings[$number]";
		    last tags;
		}; 
		
		/^(3|4|9|10|11|12)$/ && do {
		    # INTEGER, FLOAT, FIELDREF, METHODREF, IFACEREF, NAMEANDTYPE
		    readInBuff 4;
		    last tags;
		}; 
		
		/^(5|6)$/ && do {
		    # LONG, DOUBLE
		    readInBuff 8;
		    $number++;
		    last tags;
		};
		
		/^7$/ && do {
		    # CLASS
		    readInBuff 2;
		    push @clazzes, (unpack("n", $buff) & 0xffff);
		    last tags;
		}; 
		
		/^8$/ && do {
		    # STRING
		    readInBuff 2;
		    last tags;
		};
		die "Unknown tag: $tag, $number/$length, $filename";
	    }
	    #print STDERR "\n";
	}
	

	my @deplist = ();
      clazz:
	for $c (@clazzes) {
	    $clzz = $strings[$c];
	    next if $clzz =~ /^\[/;
	    next if defined $done{"$clzz"};
	    $done{$clzz} = 1;

	    my $p;
	    for $p (split ':', $classpath) {
		if (-e "$p/$clzz.java") {
		    my $path="$p/";
		    if (defined $dependdir) {
			$path = "$dependdir/";
			if (defined $subdir) {
			    my $currsubdir = "$subdir/";
			    while ($currsubdir =~ m<^([A-Za-z0-9]+)/+(.*)>) {
				$currsubdir = $2;
				my $firstcomp = $1;
				if ($clzz =~ m<$firstcomp/(.*)>) {
				    my $remain = $1;
				    if ($path =~ m<^(|.*/)\.\./+$>) {
					$path = $1;
					$clzz = $remain;
				    }
				}
			    }
			}
		    }
		    push @deplist, "$path$clzz.java";
		    next clazz;
		}
	    }
	}
	if (@deplist) {
	    print DEPFILE "$clazz: " . join (" ", @deplist) . "\n";
	}
    }
}
close DEPFILE;
