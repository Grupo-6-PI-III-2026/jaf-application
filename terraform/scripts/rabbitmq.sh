#!/bin/bash
# Sem "set -x": evita gravar credenciais do broker no log do cloud-init.
set -eu
dnf update -y
dnf install -y docker ec2-instance-connect
systemctl enable --now docker

docker run -d \
  --name rabbitmq \
  --restart always \
  -p 5672:5672 \
  -e RABBITMQ_DEFAULT_USER="${rabbitmq_user}" \
  -e RABBITMQ_DEFAULT_PASS="${rabbitmq_password}" \
  -v rabbitmq_data:/var/lib/rabbitmq \
  rabbitmq:3
