#!/bin/bash
# Sem "set -x": evita gravar credenciais do broker no log do cloud-init.
set -eu
dnf update -y
dnf install -y docker ec2-instance-connect
systemctl enable --now docker

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

pull_attempt=1
until docker pull rabbitmq:3
do
  if [ "$pull_attempt" -ge 10 ]; then
    echo "Falha ao baixar a imagem do RabbitMQ após 10 tentativas." >&2
    exit 1
  fi
  delay=$((pull_attempt * 10))
  if [ "$delay" -gt 60 ]; then delay=60; fi
  echo "Imagem do RabbitMQ indisponível; nova tentativa em ${delay} segundos (tentativa $((pull_attempt + 1))/10)." >&2
  sleep "$delay"
  pull_attempt=$((pull_attempt + 1))
done

docker run -d \
  --name rabbitmq \
  --restart always \
  -p 5672:5672 \
  -e RABBITMQ_DEFAULT_USER="${rabbitmq_user}" \
  -e RABBITMQ_DEFAULT_PASS="${rabbitmq_password}" \
  -v rabbitmq_data:/var/lib/rabbitmq \
  rabbitmq:3
