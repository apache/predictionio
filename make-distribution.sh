#!/usr/bin/env bash

set -e

FWDIR="$(cd `dirname $0`; pwd)"
DISTDIR="$FWDIR/dist"

cd $FWDIR
sbt/sbt core/publishLocal data/publishLocal engines/publishLocal engines/assemblyPackageDependency tools/assembly

cd $FWDIR
rm -rf $DISTDIR
mkdir -p $DISTDIR/bin
mkdir -p $DISTDIR/conf
mkdir -p $DISTDIR/lib

cp $FWDIR/bin/* $DISTDIR/bin
cp $FWDIR/conf/* $DISTDIR/conf
cp $FWDIR/assembly/*assembly*jar $DISTDIR/lib
cp $FWDIR/engines/target/scala-2.10/engines*jar $DISTDIR/lib

rm -f $DISTDIR/lib/*javadoc.jar
rm -f $DISTDIR/lib/*sources.jar
rm -f $DISTDIR/conf/pio-env.sh

touch $DISTDIR/RELEASE

TARNAME=imagine.tar.gz
TARDIR=imagine
cp -r $DISTDIR $TARDIR

tar zcvf $TARNAME $TARDIR
rm -rf $TARDIR

echo "PredictionIO binary distribution created at $TARNAME"
