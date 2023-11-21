#!/bin/bash

mainfile="project.PeerProcess"
abspath="$(cd "$(dirname "$0")" && pwd)"
# echo "$abspath"

builddir="$abspath/Build"
if [ ! -d "$builddir" ]; then
    mkdir -p "$builddir"
fi
rundir="$abspath/RunDir"

# echo "$builddir"
# echo "$rundir"

cp -r "$abspath/src"/* "$builddir"  # Copy contents from src to builddir
echo "compiling"
cd "$abspath/src" || exit

javac -d "$builddir" "${mainfile//./\/}.java"
# echo "Running"
# java -Duser.dir="$rundir" -cp "$builddir" "$mainfile" 1001
# rm -f "$builddir/empty.class"
cd "$abspath" || exit

rm -rf RunDir
cp -r RunDirClean RunDir
