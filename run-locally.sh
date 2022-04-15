docker image prune -y
docker volume prune -y
docker build -f logs/grafana/Dockerfile -t preconfigured-grafana . -y
docker build -f metrics-collector/Dockerfile -t metrics-collector . -y
docker build -f generator/Dockerfile -t generator . -y
docker build -f structured-streaming/Dockerfile -t driver . -y
docker compose up -d -y