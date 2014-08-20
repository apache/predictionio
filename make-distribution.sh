#!/usr/bin/env bash

set -e

FWDIR="$(cd `dirname $0`; pwd)"
DISTDIR="$FWDIR/dist"

cd $FWDIR
sbt/sbt core/publishLocal tools/assembly

cd $FWDIR/engines
../sbt/sbt publishLocal

cd $FWDIR
rm -rf $DISTDIR
mkdir -p $DISTDIR/bin
mkdir -p $DISTDIR/conf
mkdir -p $DISTDIR/lib

cp $FWDIR/bin/* $DISTDIR/bin
cp $FWDIR/conf/* $DISTDIR/conf
cp $FWDIR/assembly/*assembly*jar $DISTDIR/lib

rm $DISTDIR/conf/pio-env.sh

touch $DISTDIR/RELEASE

TARNAME=imagine.tar.gz
TARDIR=imagine
cp -r $DISTDIR $TARDIR

tar zcvf $TARNAME $TARDIR
rm -rf $TARDIR

echo "PredictionIO binary distribution created at $TARNAME"
