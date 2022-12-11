#!/bin/bash

###
# This script creates separate directories for each KA.
###

set -e
set -u

# Some well-known files.
CONFIG="config.txt"
CONFIG_UPDATED=false
KA="ka.jar"
ORGANIZE="organize.sh"
KEYSTORE_EXT=".jks"

while read -r line; do
  agentName=$(echo "${line//${KEYSTORE_EXT}/}" | sed 's/.\///')

  mkdir -p "${agentName}"

  # Append a new line to the old config to make sure there's exactly one SLMR.
  if [ "$CONFIG_UPDATED" = false ] && [ "${agentName}" != "agent1" ]; then
    CONFIG_UPDATED=true
    echo $'\nmodelRepository.isSLMR=0' >>"${CONFIG}"
  fi

  cp -f {"${CONFIG}","${KA}","${agentName}${KEYSTORE_EXT}"} "${agentName}"

  rm "${agentName}${KEYSTORE_EXT}"
done < <(find ./ -type f -iname "*${KEYSTORE_EXT}")

rm "${CONFIG}" "${KA}" "${ORGANIZE}"
