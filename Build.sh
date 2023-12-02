#!/bin/bash

mainfile=project.PeerProcess
abspath=$PWD
builddir=$abspath/Build
rundir=$abspath/RunDir
cleanrundir=$abspath/RunDirClean
srcpath=$abspath/src

mainfilepath=${mainfile//\./\/}.java
echo $builddir

echo $rundir
echo $mainfilepath

./Clean.sh

mkdir $builddir
cp -r $cleanrundir $rundir

cd "$srcpath"
javac -d "$builddir" "$mainfilepath"
cd "$abspath"

