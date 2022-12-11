#!/bin/bash

###
# This script generates a keystore for a service (containing its private key and public key certificate).
###

set -e
set -u

CA_DIR_NAME="ca"
CA_DIR="./${CA_DIR_NAME}"
SERVICES_DIR_NAME="services"
SERVICES_DIR="./${SERVICES_DIR_NAME}"

function pressKeyExit() {
  read -rp $'\n> Press any key to exit.\n'
  exit
}

if [ $# -ne 1 ]; then
  echo "Usage: ./createServiceKeyStore.sh <serviceName>"
  exit
fi

# Directory where service key stores are placed.
mkdir -p "${SERVICES_DIR}"

# Check if the file doesn't exist already.
if [ -f "${SERVICES_DIR}/${1}.jks" ]; then
  echo "Keystore for service ${1} already exists."
  pressKeyExit
fi

# Generate the certificate.
java -jar certgen.jar "${CA_DIR}/ca.crt" "${CA_DIR}/ca.key" "$1" service "${SERVICES_DIR}"
pressKeyExit
