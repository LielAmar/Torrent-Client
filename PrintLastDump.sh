#!/bin/bash

num=$(./GetPeerNum.sh)

debugPath=RunDir/peer_$num/debug.log
firstLine=$(cat $debugPath | grep -n "Preparing to dump entire state" | tail -n 1 | cut -f1 -d":")
lastLine=$(cat $debugPath | grep -n "Finished dumping entire state" | tail -n 1 | cut -f1 -d":")

sed -n $firstLine,${lastLine}p $debugPath || (echo -n "First: " && echo $firstLine && echo -n "Last: " && echo $lastLine)
