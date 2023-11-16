#!/bin/bash

mainfile="project.PeerProcess"
abspath=$(dirname "$(realpath "$0")")

builddir="$abspath/Build"
if [ ! -d "$builddir" ]; then
    mkdir -p "$builddir"
fi

rundir="$abspath/RunDir"

cp -R "$abspath/src"/* "$builddir"
echo "compiling"
cd "$abspath/src" || exit

find . -name "*.java" -exec javac -d "$builddir" {} +

# Run your Java command if needed
# java -Duser.dir="$rundir" -cp "$builddir" "$mainfile" 1001

cd "$abspath" || exit
