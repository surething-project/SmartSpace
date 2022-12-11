#!/bin/bash

###
# This script initializes CA and essential certificates.
###

set -e
set -u

NUM_AGENTS=3
SERVICES=("${@}")

# Create the CA.
./createCA.sh

# Create the system certificate.
./createSystemKeyStore.sh

# Create NUM_AGENTS key stores for agents.
for ((i = 1; i <= "${NUM_AGENTS}"; i++)); do
  ./createAgentKeyStore.sh "${i}"
done

# Create a key store for each service that was identified.
for service in "${SERVICES[@]}"; do
  ./createServiceKeyStore.sh "${service}"
done
