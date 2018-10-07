#!/usr/bin/perl -w

my $num = 0;
for (@ARGV) {
  next if $_ !~ /\.php$/;
  $html = $php = $_;
  $html =~ s/\.php$/.html/;
  
  $ENV{extension}="html";
  if (! -e "$html" || (-M "$php" <= -M "$html")) {
    $num++;
    system "php -f $php >$html";
  }
}
print $num . " html files updated.\n";
