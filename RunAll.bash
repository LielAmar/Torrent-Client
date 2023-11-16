#!/bin/bash

mainfile="project.PeerProcess"
abspath=$(dirname "$(realpath "$0")")

builddir="$abspath/Build"
rundir="$abspath/RunDir"

peerIDStart=1001
peerIDEnd=1002

for ((i = peerIDStart; i <= peerIDEnd; i++)); do
    ping -c 1 192.0.2.2 > /dev/null 2>&1
    java -Duser.dir="$rundir" -cp "$builddir" "$mainfile" "$i" &
done

# Wait for all background processes to finish
wait