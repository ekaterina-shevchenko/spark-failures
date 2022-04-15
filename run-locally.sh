docker image prune
docker volume prune
docker build -f logs/grafana/Dockerfile -t preconfigured-grafana .
docker build -f metrics-collector/Dockerfile -t metrics-collector .
docker build -f generator/Dockerfile -t generator .
docker build -f structured-streaming/Dockerfile -t driver .
docker compose up -d