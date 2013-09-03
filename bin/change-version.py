#!/usr/bin/env python

import glob
import os
import shutil
import sys

def change(filename, oldversion, newversion):
    tempfile = filename + '.tmp'
    fi = open(filename, 'r')
    fo = open(tempfile, 'w')
    for line in fi:
        nl = line.replace(oldversion, newversion)
        fo.write(nl)
    fo.close()
    fi.close()
    shutil.copymode(filename, tempfile)
    shutil.move(tempfile, filename)
    print "Processed " + filename

if len(sys.argv) < 3:
    print """
Usage: bin/change-version.py <old-version> <new-version>
    """
    sys.exit(-1)

oldversion = sys.argv[1]
newversion = sys.argv[2]
files = [
    'bin/common.sh',
    'commons/build.sbt',
    'dist/bin/*',
    'dist/bin/*/*',
    'dist/conf/init.json',
    'dist/conf/predictionio.conf',
    'output/build.sbt',
    'process/commons/hadoop/scalding/build.sbt',
    'process/engines/itemrec/algorithms/hadoop/scalding/build.sbt',
    'process/engines/itemrec/algorithms/hadoop/scalding/*/build.sbt',
    'process/engines/itemrec/algorithms/scala/mahout/build.sbt',
    'process/engines/itemrec/algorithms/scala/mahout/commons/build.sbt',
    'process/engines/itemrec/evaluations/hadoop/scalding/build.sbt',
    'process/engines/itemrec/evaluations/hadoop/scalding/metrics/map/build.sbt',
    'process/engines/itemrec/evaluations/hadoop/scalding/trainingtestsplit/build.sbt',
    'process/engines/itemrec/evaluations/scala/*/build.sbt',
    'process/engines/itemsim/algorithms/hadoop/scalding/build.sbt',
    'process/engines/itemsim/evaluations/hadoop/scalding/build.sbt',
    'process/engines/itemsim/evaluations/scala/*/build.sbt',
    'servers/*/project/Build.scala',
    'servers/scheduler/conf/application.conf',
    'tools/*/build.sbt',
    'tools/migration/*/*/build.sbt',
    'tools/softwaremanager/src/main/scala/io/prediction/tools/softwaremanager/*.scala'
]

for f in files:
    for rf in glob.glob(f):
        if os.path.isfile(rf):
            change(rf, oldversion, newversion)
