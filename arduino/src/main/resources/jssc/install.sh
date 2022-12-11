#!/bin/bash

###
# This script deploys all relevant modules locally.
###

set -e
set -u

GROUP_ID=io.github.java-native.jssc
ARTIFACT_ID=jssc
VERSION=2.9.1

mvn install:install-file \
-Dfile=./jssc-2.9.1.jar \
-DgroupId=${GROUP_ID} \
-DartifactId="${ARTIFACT_ID}" \
-Dversion="${VERSION}" \
-Dpackaging=jar
