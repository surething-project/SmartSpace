#!/bin/bash

###
# This script creates a DS2OS CA, if a previous one does not exist already.
###

set -e
set -u

CA_DIR_NAME="ca"
CA_DIR="./${CA_DIR_NAME}"

function pressKeyExit() {
  read -rp $'\n> Press any key to exit.\n'
  exit
}

# If the directory exists and is not empty, quit the program.
if [ -d "${CA_DIR}" ] && [ -n "$(ls -A "${CA_DIR}")" ]; then
  echo "DS2OS CA already exists."
  pressKeyExit
fi

# Create a brand new directory.
rm "${CA_DIR}" -rf
mkdir -p "${CA_DIR}"

# Generate CA certificate and key.
java -jar cagen.jar "${CA_DIR}"

pressKeyExit
