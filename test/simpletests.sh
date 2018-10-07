#!/bin/sh

TEMP=`mktemp -d tmp.XXXXXX`

if echo $JAVAC | grep jikes >/dev/null; then
    compiler=JIKES;
    version=`$JAVAC 2>&1 | grep Version | \
		perl -pe's/^.*Version \"?([0-9]+)\.([0-9]+).*$/\1/'`
elif echo $JAVAC | grep javac  >/dev/null; then
    compiler=JAVAC
    version=`$JAVAC -J-version 2>&1 | grep version | \
		perl -pe's/^.*version \"?([0-9]+)\.([0-9]+).*$/\1\2/'`
else
    compiler=UNKNOWN
    version=""
fi

echo "detected compiler $compiler"

error=""

EXPECT_FAIL=""

for testclass in \
ArrayCloneTest.java \
ArrayTest.java \
AssignOp.java \
ClassOpTest.java \
ConstantTypes.java \
Expressions.java \
Flow.java \
For.java \
HintTypeTest.java \
IfCombine.java \
LocalTypes.java \
ResolveConflicts.java \
TriadicExpr.java \
TryCatch.java \
Unreach.java \
AnonymousClass.java \
InnerClass.java \
InnerCompat.java \
NestedAnon.java 
do
    cp $srcdir/$testclass $TEMP
    $PERL $top_srcdir/scripts/jcpp.pl -D$compiler -D$compiler$version \
         $TEMP/$testclass
    CLASSPATH=$CLASSPATH:$CLASSLIB $JAVAC $JFLAGS -d $TEMP $TEMP/$testclass
    CLASSPATH=$CLASSPATH:$CLASSLIB $JAVA jode.Decompiler \
         --classpath=$TEMP --dest=$TEMP ${testclass%.java} > $testclass.log 2>&1
    if ! CLASSPATH=$TEMP:$CLASSPATH $JAVAC $JFLAGS -d $TEMP $TEMP/$testclass >> $testclass.log 2>&1 ; then
       cat $TEMP/$testclass >> $testclass.log
       CLASSPATH=$TEMP:$CLASSPATH javap -c ${testclass%.java} >> $testclass.log
       if ! echo $EXPECT_FAIL | grep $testclass >/dev/null ; then
         error="$error $testclass";
         echo "FAIL: $testclass"
       else
         echo "EXPECTED FAIL: $testclass"
       fi
    else
       echo "PASS: $testclass"
       rm $testclass.log
    fi
    #rm -rf $TEMP/*
done

rm -rf $TEMP;

if [ -n "$error" ]; then
    exit 1;
fi
