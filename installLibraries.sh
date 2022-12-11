#!/bin/bash

# This scripts installs the following libraries required to run SureSpace:
# - DS2OS;
# - MATLAB Engine API for Java.

set -e
set -u

# Handle DS2OS.
MODULES=('core' 'databind-mapper' 'java8-connector' 'net-utils' 'rest-connector')
for module in "${MODULES[@]}"; do
  mvn install:install-file \
  -Dfile=./libraries/DS2OS/"${module}"/target/"${module}"-0.1.jar \
  -DgroupId=org.ds2os.vsl \
  -DartifactId="${module}" \
  -DpomFile=./libraries/DS2OS/"${module}"/pom.xml \
  -Dversion=0.1 \
  -Dpackaging=jar
done

# Handle MATLAB Engine API for Java.
mvn install:install-file \
-Dfile=./libraries/MATLAB/engine.jar \
-DgroupId=com.mathworks \
-DartifactId=matlab \
-Dversion=1.0 \
-Dpackaging=jar
