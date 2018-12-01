#!/bin/sh

#newVersion=$1
newVersion=$1-SNAPSHOT

mvn --batch-mode release:update-versions -DdevelopmentVersion=$newVersion

sed -i '' "s/coat-libs-\(.*\)\.jar/coat-libs-$newVersion.jar/" ./build-coatjava.sh

