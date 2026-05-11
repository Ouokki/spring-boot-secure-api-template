#!/usr/bin/env bash
# Generates a 2048-bit RSA key pair for local development JWT signing.
# Output: dev-keys/private.pem and dev-keys/public.pem
# NEVER use these keys in production — generate fresh keys per environment.
set -euo pipefail

KEYS_DIR="$(cd "$(dirname "$0")/.." && pwd)/dev-keys"
mkdir -p "$KEYS_DIR"

echo "Generating RSA-2048 key pair in $KEYS_DIR ..."

# Generate private key in PKCS#8 format (required by JJWT's RsaKeyAlgorithm)
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$KEYS_DIR/private.pem"

# Extract public key
openssl rsa -pubout -in "$KEYS_DIR/private.pem" -out "$KEYS_DIR/public.pem"

echo "Done. Files written:"
echo "  $KEYS_DIR/private.pem"
echo "  $KEYS_DIR/public.pem"
echo ""
echo "These keys are gitignored. Re-run this script on any new checkout."
