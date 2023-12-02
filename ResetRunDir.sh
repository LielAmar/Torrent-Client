#!/bin/bash

abspath=$PWD
rundir=$abspath/RunDir
cleanrundir=$abspath/RunDirClean

rm -r $rundir
cp -r $cleanrundir $rundir
