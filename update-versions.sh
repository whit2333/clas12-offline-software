#!/bin/sh

#newVersion=$1
newVersion=$1-SNAPSHOT

batch="mvn --batch-mode release:update-versions -DdevelopmentVersion=$newVersion"
interactive="mvn release:update-versions"
 
cd common-tools
$batch
cd -

sed -i '' "s/coat-libs-\(.*\)\.jar/coat-libs-$newVersion.jar/" ./build-coatjava.sh
sed -i '' "s/coat-libs-\(.*\)\.jar/coat-libs-$newVersion.jar/" ./common-tools/coat-lib/deployDistribution.sh

