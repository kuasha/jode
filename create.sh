#!/bin/sh

create_jar() {
jar -xvf $HOME/java/jars/getopt.jar
rm -rf META-INF
jar -cvf ../jode-1.0.92-$1.jar AUTHORS COPYING README INSTALL NEWS doc/*.{html,jos,perl,gif} `find jode -name \*.class` gnu
rm -rf gnu
}

create_first() {
# first 1.1 version.
tar -xvzf jode-1.0.92.tar.gz
cd jode-1.0.92
CLASSPATH=$HOME/java/jars/getopt.jar:/usr/local/1.1collections/lib/collections.jar:/usr/local/swing-1.1/swingall.jar \
  ./configure --with-java=/usr/lib/java --with-jikes=/home/jochen/bin 
make
create_jar 1.1
cd ..
rm -rf jode-1.0.92
}

create_second() {
# now 1.2 version.
tar -xvzf jode-1.0.92.tar.gz
cd jode-1.0.92
find -name \*.java -o -name \*.java.in | xargs jcpp -DJDK12
CLASSPATH=$HOME/java/jars/getopt.jar \
  ./configure --with-java=/usr/local/jdk1.2 --with-jikes=/home/jochen/bin 
make
create_jar 1.2
cd ..
rm -rf jode-1.0.92
}

create_applet() {
cat <<EOF >jode-applet.jos
# JODE Optimizer Script
strip = "unreach","source","lnt","lvt","inner"
load = new WildCard { value = "jode" },
       new WildCard { value = "gnu" }
preserve = new WildCard { value = "jode.JodeApplet.<init>.()V" }
renamer = new StrongRenamer {
  charsetStart = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
  charsetPart = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_$"
  charsetPackage = "abcdefghijklmnopqrstuvwxyz"
  charsetClass = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
}
analyzer = new ConstantAnalyzer
post = new LocalOptimizer, new RemovePopAnalyzer
EOF

CLASSPATH=jode-1.0.92-1.1.jar:$CLASSPATH java jode.obfuscator.Main \
  --cp jode-1.0.92-1.1.jar --dest jode-applet.jar jode-applet.jos
}

#create_first
#create_second
create_applet
