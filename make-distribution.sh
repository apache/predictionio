#!/usr/bin/env bash

set -e

FWDIR="$(cd `dirname $0`; pwd)"
DISTDIR="$FWDIR/dist"

rm -rf $DISTDIR
mkdir -p $DISTDIR/bin
mkdir -p $DISTDIR/conf
mkdir -p $DISTDIR/lib

cp $FWDIR/bin/* $DISTDIR/bin
cp $FWDIR/conf/* $DISTDIR/conf
cp $FWDIR/tools/target/scala*/*assembly*jar $DISTDIR/lib

rm $DISTDIR/conf/pio-env.sh

touch $DISTDIR/RELEASE

TARDIR=imagine
cp -r $DISTDIR $TARDIR

tar zcvf imagine.tar.gz $TARDIR
rm -rf $TARDIR
