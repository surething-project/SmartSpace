#!/bin/bash

###
# This script generates a keystore for an agent (containing its private key and public key certificate).
###

set -e
set -u

CA_DIR_NAME="ca"
CA_DIR="./${CA_DIR_NAME}"
SYSTEM_DIR_NAME="system"
SYSTEM_DIR="./${SYSTEM_DIR_NAME}"

function pressKeyExit() {
  read -rp $'\n> Press any key to exit.\n'
  exit
}

# Directory where agent key stores are placed.
mkdir -p "${SYSTEM_DIR}"

# Check if the file doesn't exist already.
if [ -f "${SYSTEM_DIR}/system.jks" ]; then
  echo "Keystore for system already exists."
  pressKeyExit
fi

# Generate the certificate.
java -jar certgen.jar "${CA_DIR}/ca.crt" "${CA_DIR}/ca.key" system agent "${SYSTEM_DIR}"

pressKeyExit
