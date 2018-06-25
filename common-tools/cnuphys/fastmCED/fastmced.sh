#!/bin/bash

SCRIPT_DIR=`dirname $0`

echo $SCRIPT_DIR
JARNAME=$SCRIPT_DIR/fastmced.jar
MAIN=cnuphys.ced.frame.FatsMCed
VARGS="-Dsun.java2d.pmoffscreen=false -Xmx1024M -Xss512k"
java $VARGS -jar $JARNAME $MAIN
