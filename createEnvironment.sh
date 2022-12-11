#!/bin/bash

# This script creates a new local environment in your computer containing all the files needed to:
# - deploy a new Knowledge Agent (three agents are made available to you);
# - start DS2OS services (using the context models under models/)
#   . console to debug the VSL
#   . adaptation services
#   . orchestration service (THOUGH IT IS COPIED, DON'T RUN IT)
#

set -e
set -u

function pressKey() {
  read -rp $'\n> Press any key to continue.\n'
}

function pressKeyExit() {
  read -rp $'\n> Press any key to exit.\n'
  exit
}

# Check if necessary arguments were provided.
if [ $# != 1 ]; then
  echo "Usage: ./createEnvironment.sh <directory>"
  pressKeyExit
fi

# Important constants.
DIR=$(realpath "${1}")/

# Create directories for: CA, KAs, services, and models.
mkdir -p "${DIR}"{ca,agents,services,models}

# Start by looking for DS2OS services.
KNOWN_SERVICES=()
while read -r line; do
  # We must, in the first place, fix the name of the file.
  fixedJarName=$(echo "${line//-jar-with-dependencies/}" | sed 's/.*\/.*\///g')

  # Take the opportunity to copy JAR files.
  cp -f "$line" "${DIR}"services/"$fixedJarName"

  # But we need to fix the service name (it should be camel case).
  serviceName=$(echo "${fixedJarName}" | sed 's/-//g' | awk -F - '{print tolower($0)}')

  # And, then, we are going to add this as a known service.
  KNOWN_SERVICES+=("${serviceName//.jar/}")
done < <(find ./ds2os/ -type f -iname "*-jar-with-dependencies.jar")

# Create key stores for all system entities (CA, agents, and services).
cd ./ds2os/src/main/resources/dist/certificates
./createKeyStores.sh "${KNOWN_SERVICES[@]}"

# Copy CA private key and public key certificate.
cp -f ./ca/* "${DIR}"ca

# Copy agents information.
cd ..
cp -f {./agents/*,./certificates/agents/*} "${DIR}"agents

# Copy services.
cp -f {./services/*,./certificates/services/*,./certificates/system/*} "${DIR}"services

# Copy models.
cp -rf ./models/* "${DIR}"models

pressKey

# Run script to create separate folders for each agent.
cd "${DIR}"agents
./organize.sh

pressKeyExit
