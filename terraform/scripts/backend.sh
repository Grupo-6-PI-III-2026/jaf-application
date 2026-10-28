#!/bin/bash
# Sem "set -x": evita gravar senhas e tokens no log do cloud-init.
set -eu
dnf update -y
dnf install -y docker ec2-instance-connect openssh-server


systemctl enable docker
systemctl start docker
systemctl enable sshd
systemctl start sshd

usermod -aG docker ec2-user

echo "${ghcr_token}" | docker login ghcr.io \
  --username "${ghcr_user}" \
  --password-stdin
docker pull ghcr.io/${repo_owner}/jaf-backend:latest

docker run -d \
  --name jaf-backend \
  --restart always \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="${db_url}" \
  -e SPRING_DATASOURCE_USERNAME="${db_username}" \
  -e SPRING_DATASOURCE_PASSWORD="${db_password}" \
  -e JWT_SECRET="${jwt_secret}" \
  -e JWT_EXPIRATION="${jwt_expiration}" \
  -e RABBITMQ_HOST="${rabbitmq_host}" \
  -e RABBITMQ_PORT=5672 \
  -e RABBITMQ_USER="${rabbitmq_user}" \
  -e RABBITMQ_PASSWORD="${rabbitmq_password}" \
  ghcr.io/${repo_owner}/jaf-backend:latest