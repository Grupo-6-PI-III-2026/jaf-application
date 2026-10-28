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

wait_attempt=1
until docker info >/dev/null 2>&1
do
  if [ "$wait_attempt" -ge 10 ]; then
    echo "Docker não ficou disponível após 10 tentativas." >&2
    exit 1
  fi
  sleep 5
  wait_attempt=$((wait_attempt + 1))
done

login_attempt=1
until echo "${ghcr_token}" | docker login ghcr.io \
  --username "${ghcr_user}" \
  --password-stdin
do
  if [ "$login_attempt" -ge 10 ]; then
    echo "Falha ao autenticar no GHCR após 10 tentativas." >&2
    exit 1
  fi
  delay=$((login_attempt * 10))
  if [ "$delay" -gt 60 ]; then delay=60; fi
  echo "GHCR indisponível; nova tentativa em ${delay} segundos (tentativa $((login_attempt + 1))/10)." >&2
  sleep "$delay"
  login_attempt=$((login_attempt + 1))
done

pull_attempt=1
until docker pull ghcr.io/${repo_owner}/jaf-backend:latest
do
  if [ "$pull_attempt" -ge 10 ]; then
    echo "Falha ao baixar a imagem do backend após 10 tentativas." >&2
    exit 1
  fi
  delay=$((pull_attempt * 10))
  if [ "$delay" -gt 60 ]; then delay=60; fi
  echo "Imagem do backend indisponível; nova tentativa em ${delay} segundos (tentativa $((pull_attempt + 1))/10)." >&2
  sleep "$delay"
  pull_attempt=$((pull_attempt + 1))
done

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