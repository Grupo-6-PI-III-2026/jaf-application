#!/bin/bash
# Sem "set -x": evita gravar tokens e chaves no log do cloud-init.
set -eu
dnf update -y
dnf install -y docker ec2-instance-connect
systemctl enable --now docker
usermod -aG docker ec2-user

echo "${ghcr_token}" | docker login ghcr.io \
  --username "${ghcr_user}" \
  --password-stdin
docker pull ghcr.io/${repo_owner}/jaf-ocr:latest

# O restart reconecta o worker enquanto o broker ainda está iniciando.
docker run -d \
  --name jaf-ocr-worker \
  --restart always \
  -e RABBITMQ_HOST="${rabbitmq_host}" \
  -e RABBITMQ_PORT=5672 \
  -e RABBITMQ_USER="${rabbitmq_user}" \
  -e RABBITMQ_PASSWORD="${rabbitmq_password}" \
  -e OCR_SPACE_API_KEY="${ocr_space_api_key}" \
  ghcr.io/${repo_owner}/jaf-ocr:latest \
  python -u -m app.infrastructure.messaging.consumer
