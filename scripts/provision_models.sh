#!/usr/bin/env bash
# Provision upscale models into the upscayl-models Docker volume.
# Run once on the stack host AFTER the muttcode-deploy stack is up (so the
# volume exists). Idempotent — skips models already present.
#
# Models:
#   realesrgan-x4plus               (diffuse/seamless) — original Real-ESRGAN ncnn release
#   4x_NMKD-Superscale-SP_178000_G  (normals)          — upscayl/custom-models
#
# Usage:
#   bash provision_models.sh                       # auto-detect volume
#   bash provision_models.sh muttcode-deploy_upscayl-models
set -euo pipefail

VOLUME="${1:-$(docker volume ls --format '{{.Name}}' | grep 'upscayl-models' | head -1)}"
if [ -z "$VOLUME" ]; then
    echo "ERROR: could not find an upscayl-models volume; pass it explicitly."
    docker volume ls
    exit 1
fi
echo "Using volume: $VOLUME"

REALESRGAN_ZIP="https://github.com/xinntao/Real-ESRGAN/releases/download/v0.2.5.0/realesrgan-ncnn-vulkan-20220424-ubuntu.zip"
CUSTOM_BASE="https://media.githubusercontent.com/media/upscayl/custom-models/main/models"
NMKD="4x_NMKD-Superscale-SP_178000_G"

docker run --rm -v "${VOLUME}:/models" --network host alpine sh -s <<EOF
set -e
apk add --no-cache wget unzip >/dev/null 2>&1
cd /models

if [ -f realesrgan-x4plus.param ] && [ -f realesrgan-x4plus.bin ]; then
    echo "realesrgan-x4plus already present."
else
    echo "Fetching realesrgan-x4plus (Real-ESRGAN ncnn release)..."
    wget -q -O /tmp/re.zip "${REALESRGAN_ZIP}"
    unzip -joq /tmp/re.zip '*realesrgan-x4plus.param' '*realesrgan-x4plus.bin' -d /models
    rm -f /tmp/re.zip
fi

if [ -f ${NMKD}.param ] && [ -f ${NMKD}.bin ]; then
    echo "${NMKD} already present."
else
    echo "Fetching ${NMKD} (upscayl/custom-models)..."
    wget -q -O /models/${NMKD}.param "${CUSTOM_BASE}/${NMKD}.param"
    wget -q -O /models/${NMKD}.bin   "${CUSTOM_BASE}/${NMKD}.bin"
fi

echo "Installed models:"
ls -1 /models/*.param 2>/dev/null | sed 's#.*/##; s/\.param$//'
EOF

echo "Done. Restart the upscayl container to pick up new models:"
echo "  docker restart \$(docker ps -qf name=upscayl)"
