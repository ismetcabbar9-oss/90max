#!/usr/bin/env bash
# Kalıcı sürüm imzalama anahtarı üretir ve GitHub Secrets'a girilecek değerleri yazdırır.
# Kendi bilgisayarında çalıştır. Oluşan .jks dosyasını ve şifreyi güvenli bir yerde sakla;
# kaybedersen aynı uygulamaya güncelleme yayımlayamazsın. Bu dosyayı ASLA depoya ekleme.
set -euo pipefail
KS="${1:-90max-release.jks}"
ALIAS="${2:-90max}"
read -r -s -p "Anahtar deposu şifresi (en az 6 karakter): " PASS; echo
keytool -genkeypair -v -keystore "$KS" -alias "$ALIAS" -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "$PASS" -keypass "$PASS" -dname "CN=90Max"
echo
echo "GitHub > Settings > Secrets and variables > Actions > New repository secret:"
echo "  SIGNING_KEYSTORE_BASE64  = (aşağıdaki tek satır)"
echo "  SIGNING_STORE_PASSWORD   = girdiğin şifre"
echo "  SIGNING_KEY_ALIAS        = $ALIAS"
echo "  SIGNING_KEY_PASSWORD     = girdiğin şifre"
echo
base64 -w0 "$KS" 2>/dev/null || base64 "$KS" | tr -d '\n'
echo
