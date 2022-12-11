#!/bin/bash
#
# Creates all certificates necessary for the ilab
#
# Author: liebald

set -e -u

args=()
for i in {1..6}; do
    args+=("agent$i")
done
for i in {1..6}; do
    args+=("tempOut$i")
done
for i in {1..6}; do
    args+=("tempIn$i")
done
for i in {1..6}; do
    args+=("lumiSens$i")
done
for i in {1..6}; do
    args+=("windowControl$i")
done
for i in {1..6}; do
    args+=("movement$i")
done
for i in {1..6}; do
    args+=("lightControl$i")
done
for i in {1..6}; do
    args+=("questioningService$i")
done
args+=("weatherStation")
args+=("heatingControl")
args+=("presenceGenerator")
args+=("userPlace")
args+=("bathControl")
args+=("system")
args+=("service") 
for i in {1..6}; do
    args+=("logger$i")
done
for i in {1..6}; do
    args+=("sphinx$i")
done
args+=("echidna")


./create-keystores.sh "${args[@]}"
cd ssl-keys
rm *.pem
rm *.key
rm *.crt
rm *.p12
rm *.csr
rm index.*
rm serial*
rm openssl.cnf
