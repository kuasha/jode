#!/usr/bin/perl -w
#
# jcpp Copyright (C) 1999-2001 Jochen Hoenicke.
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
# $Id: jcpp.pl 1340 2001-08-08 17:59:08Z hoenicke $

# This is a program to allow conditional compiled code in java files.
# The key idea is, not to run the file always through the
# preprocessor, but to modify the java files directly and make use of
# comments.
#
# The comments all have the form /// and start at the beginning of the
# line to distinguish them from normal comments. You must not use
# such comments for other purposes.
#
# Usage is simple:  jcpp -Ddefine1 -Ddefine2 first.java second.java
# The files should contain comments of the form
#
#  ///#ifdef JDK12
#      jdk1.2 code
#  ///#else
#      jdk1.1 code
#  ///#endif
#
# After running jcpp the false branch is commented out.  If the true
# branch was commented out it will get commented in.
#
# jcpp can also change definitions, useful for package renaming.  The
# java file should look like this:
#
# ///#def COLLECTIONS java.util
# import java.util.Vector
# import java.util.ArrayList
# ///#enddef
#
# If jcpp is then called with -DCOLLECTIONS=gnu.java.util.collections 
# it will replace every occurence of java.util (the string in the #def
# line) with the new value:
#
# ///#def COLLECTIONS gnu.java.util.collections
# import gnu.java.util.collections.Vector
# import gnu.java.util.collections.ArrayList
# ///#enddef

my @files;
my %defs;

for (@ARGV) {
    if ($_ =~ /^-D([^=]*)$/) {
	$defs{$1} = 1;
    } elsif ($_ =~ /^-D([^=]*)=([^=]*)$/) {
	$defs{$1} = $2;
    } else {
	push @files, $_;
    }
}

for (@files) {
    # Number of nested #if directives.  Initially 0, will be increased
    # on every #if directive and decreased on every #endif directive.
    my $level = 0;

    # The number of the outermost level, whose #if directive was
    # false.  This is 0, if there wasn't an false #if directive, yet.
    # As long as it is != 0, we comment every line, and ignore
    # directives except for increasing/decreasing $level.  
    my $falselevel = 0;

    # Tells if an error occured and the transformation shouldn't
    # be done.
    my $error = 0;
    my $changes = 0;

    # The list of #def replacements, @replold is the previous value, 
    # @replnew the new one.
    my @replold = ();
    my @replnew = ();

    my $file = $_;
    open OLD, "<$file" or do {
	print STDERR "Can't open file $file\n"; 
	next;
    };
    open NEW, ">$file.tmp" or do {
	print STDERR "Can't open tmp file $file.tmp\n"; 
	next;
    };
    my $linenr = 0;
  LINE:
    while (<OLD>) {
	$linenr++;
	if (m'^///#') {
	    # This is a directive.  First we print it out.
	    if (m'^///#\s*if') {
		$level++;
		if (m'^///#\s*ifdef\s+(\w+)\s*$') {
		    # If there was an outer false #if directive, we ignore the
		    # condition.
		    next LINE if ($falselevel);

		    my $label=$1;

		    # An ifdef directive, look if -D is defined.
		    $falselevel = $level
			unless (defined $defs{$label});
		} elsif (m'^///#\s*ifndef\s+(\w+)\s*$') {
		    # If there was an outer false #if directive, we ignore the
		    # condition.
		    next LINE if ($falselevel);
		    
		    my $label=$1;
		    # An ifndef directive, look if -D is defined
		    $falselevel = $level 
		        if (defined $defs{$label});
		} elsif (m'^///#\s*if\s+(\w+)\s*(==|!=)\s*(\S+)\s*$') {
		    # If there was an outer false #if directive, we ignore the
		    # condition.
		    next LINE if ($falselevel);
		    
		    my $label=$1;
		    my $value=$3;

		    # An ifdef directive, look if -D is defined.
		    $falselevel = $level
			unless ($2 eq "==" ? $defs{$label} eq $value
				: $defs{$label} ne $value);
		} elsif (m'^///#\s*if\s+(\w+)\s*(>=|<=|>|<)\s*(\S+)\s*$') {
		    # If there was an outer false #if directive, we ignore the
		    # condition.
		    next LINE if ($falselevel);
		    
		    my $label=$1;
		    my $value=$3;

		    # An ifdef directive, look if -D is defined.
		    $falselevel = $level
			unless ($2 eq ">=" ? $defs{$label} >= $value
				: $2 eq "<=" ? $defs{$label} <= $value
				: $2 eq ">" ? $defs{$label} > $value
				: $defs{$label} < $value);
		}
	    } elsif (m'^///#\s*else\s*$') {
		# An else directive.  We switch from true to false and 
		# if level is falselevel we switch from false to true
		if ($level == 0) {
		    # An else outside of any directives; warn.
		    print STDERR "$file: $linenr: unmatched $_";
		    $error = 1;
		} elsif ($falselevel == $level) {
		    $falselevel = 0;
		} elsif ($falselevel == 0) {
		    $falselevel = $level;
		}
	    } elsif (m'^///#\s*endif\s*$') {
		# set $falselevel to 0, if the false branch is over now.
		$falselevel = 0 if ($falselevel == $level);
		# decrease level.
		if ($level == 0) {
		    print STDERR "$file: $linenr: unmatched $_";
		    $error = 1;
		} else {
		    $level--;
		}
	    } elsif (m'^///#\s*def\s+(\w+)\s+(\S*)$') {
		my $label = $1;
                my $old = $2;
		my $new = $defs{$label};
                if (defined $new && $new ne $old) {
		    push @replold, "$old";
		    push @replnew, "$new";
		    $changes = 1;
                } else {
		    push @replnew, "";
		    push @replold, "";
		}
            } elsif (m'^///#\s*enddef\s*$') {
		pop @replold;
		pop @replnew;
	    } else {
		print STDERR "$file: $linenr: ignoring unknown directive $_";
		$error = 1;
	    }
	} elsif (m'^///(.*)') {
	    $line = $1;
	    if ($falselevel == 0 && $level > 0) {
		# remove comments in true branch, but not in outermost level.
		$_ = "$line\n";
		$changes = 1;
	    }
	} else {
	    if ($falselevel != 0) {
		# add comments in false branch
		$_ = "///$_";
		$changes = 1;
	    }
	}
	for ($i = 0; $i < @replold; $i++) {
	    $_ =~ s/\Q$replold[$i]\E/$replnew[$i]/ if ($replold[$i] ne "");
	}
	print NEW $_;
    }

    if ($level != 0 || $falselevel != 0) {
	# something got wrong
	print STDERR "$file: unmatched directives: level $level, ".
	    "falselevel $falselevel\n";
	$error = 1;
    }

    if ($error == 0) {
	if ($changes == 0) {
	    unlink "$file.tmp";
	} else {
	    (unlink "$file" and rename "$file.tmp", "$file") 
		or print STDERR "$file: Couldn't rename files.\n";
	}
    } else {
	print STDERR "$file: errors occured, file not transformed.\n";
    }
}
