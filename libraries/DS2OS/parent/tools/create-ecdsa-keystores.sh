#!/bin/sh
#
# Simple script to create two keystores for debugging purposes.
#
# Author: felix

set -e -u

# global stuff
keystorePassword="K3yst0r3"

# could be used for batch mode, too
export CERT_C="${CERT_C:-DE}"
export CERT_ST="${CERT_ST:-Germany}"
export CERT_L="${CERT_L:-Munich}"
export CERT_O="${CERT_O:-TUM}"
export CERT_OU="${CERT_OU:-I8}"
export CERT_CN="${CERT_CN:-DS2OS CA}"
export CERT_NAME="${CERT_NAME:-}"
export CERT_EMAIL="${CERT_EMAIL:-}"
export CERT_PASSWORD="${CERT_PASSWORD:-}"
export GLOBAL_CERT_SERVICE_MANIFEST="${CERT_SERVICE_MANIFEST:-}"
export CERT_SERVICE_MANIFEST="${GLOBAL_CERT_SERVICE_MANIFEST}"
export GLOBAL_CERT_ACCESS_IDS="${CERT_ACCESS_IDS:-}"
export CERT_ACCESS_IDS="${GLOBAL_CERT_ACCESS_IDS}"

if [ $# -ge 1 ] ; then
	create_certs="$@"
else
	create_certs="agent1 agent2 agent3 service system geoservice"
fi

# where to put all crap
rm -rf ./ssl-keys
mkdir -p ./ssl-keys
cp openssl.cnf ./ssl-keys/openssl.cnf
cd ./ssl-keys

echo "=== creating CA ==="
openssl ecparam -out ca.key -name secp256r1 -genkey
openssl req -batch -config openssl.cnf -days 3650 -new -nodes -sha256 -x509 -key ca.key -out ca.crt
chmod 600 ca.key
echo 01 >serial
echo -n "" >index.txt
touch index.txt.attr

create_cert() {
	local commonName="${1}"

	echo "=== create cert: ${commonName} ==="
	export CERT_CN="${commonName}"
	if echo "${commonName}" | grep "agent" >/dev/null ; then
		local extensions="ka_cert"
	else
		if [ $# -gt 1 ] ; then
			local serviceManifest="${2}"
			local accessIDs="${3}"
		else
			local serviceManifest="${GLOBAL_CERT_SERVICE_MANIFEST:-sha256:${commonName}Manifest}"
			local accessIDs="${GLOBAL_CERT_ACCESS_IDS:-${commonName}}"
		fi
		local extensions="service_cert"
		export CERT_SERVICE_MANIFEST="${serviceManifest}"
		export CERT_ACCESS_IDS="${accessIDs}"
	fi

	openssl ecparam -out "${commonName}.key" -name secp256r1 -genkey
	openssl req -batch -config openssl.cnf -new -nodes -key "${commonName}.key" -out "${commonName}.csr"
	openssl ca -batch -config openssl.cnf -extensions "${extensions}" -md sha256 -startdate "$( date --date yesterday +%Y%m%d000000Z )" -days 365 -cert ca.crt -out "${commonName}.crt" -in "${commonName}.csr"

	echo "=== create keystore bundle: ${commonName} ==="
	cat "${commonName}.crt" ca.crt >"${commonName}.pem"
	openssl pkcs12 -export -password "pass:${keystorePassword}" -inkey "${commonName}.key" -in "${commonName}.crt" -certfile "${commonName}.pem" -name "${commonName}" -CAfile ca.crt -caname root -out "${commonName}.p12"
	yes | LC_ALL=C keytool -importcert -storepass "${keystorePassword}" -keystore "${commonName}.jks" -alias "ca" -file ca.crt >/dev/null
	keytool -importkeystore -deststorepass "${keystorePassword}" -destkeypass "${keystorePassword}" -destkeystore "${commonName}.jks" -srckeystore "${commonName}.p12" -srcstoretype PKCS12 -srcstorepass "${keystorePassword}" -alias "${commonName}"

	return 0
}

# certificates to create
for commonName in ${create_certs} ; do
	create_cert "${commonName}"
done
