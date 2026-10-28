#!/bin/bash
set -eux
dnf update -y
dnf install -y docker ec2-instance-connect openssh-server
systemctl enable docker
systemctl start docker
systemctl enable --now sshd
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
  echo "GHCR indisponível; nova tentativa em $${delay} segundos (tentativa $((login_attempt + 1))/10)." >&2
  sleep "$delay"
  login_attempt=$((login_attempt + 1))
done

pull_attempt=1
until docker pull ghcr.io/${repo_owner}/jaf-frontend:latest
do
  if [ "$pull_attempt" -ge 10 ]; then
    echo "Falha ao baixar a imagem do frontend após 10 tentativas." >&2
    exit 1
  fi
  delay=$((pull_attempt * 10))
  if [ "$delay" -gt 60 ]; then delay=60; fi
  echo "Imagem do frontend indisponível; nova tentativa em $${delay} segundos (tentativa $((pull_attempt + 1))/10)." >&2
  sleep "$delay"
  pull_attempt=$((pull_attempt + 1))
done

# BACKEND_HOST alimenta o template do Nginx; o DNS do ALB interno é resolvido periodicamente.
docker run -d \
  --name jaf-frontend \
  --restart always \
  -p 80:80 \
  -e BACKEND_HOST="${backend_host}" \
  ghcr.io/${repo_owner}/jaf-frontend:latest