#!/bin/bash

###
# This script generates a keystore for an agent (containing its private key and public key certificate).
###

set -e
set -u

CA_DIR_NAME="ca"
CA_DIR="./${CA_DIR_NAME}"
AGENTS_DIR_NAME="agents"
AGENTS_DIR="./${AGENTS_DIR_NAME}"

function pressKeyExit() {
  read -rp $'\n> Press any key to exit.\n'
  exit
}

if [ $# -ne 1 ]; then
  echo "Usage: ./createAgentKeyStore.sh <agentID>"
  exit
fi

# Directory where agent key stores are placed.
mkdir -p "${AGENTS_DIR}"

# Check if the file doesn't exist already.
if [ -f "${AGENTS_DIR}/agent${1}.jks" ]; then
  echo "Keystore for agent ${1} already exists."
  pressKeyExit
fi

# Generate the certificate.
java -jar certgen.jar "${CA_DIR}/ca.crt" "${CA_DIR}/ca.key" "agent${1}" agent "${AGENTS_DIR}"

pressKeyExit
