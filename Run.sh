#!/bin/bash

mainfile=project.PeerProcess
abspath=$PWD
builddir=$abspath/Build
rundir=$abspath/RunDir

num=$(./GetPeerNum.sh)
#echo peerNum:
#echo $num

java -Duser.dir="$rundir" -cp "$builddir" $mainfile $num

