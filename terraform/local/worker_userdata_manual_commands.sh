# Docker installation via the convenience script
# For other methods follow the link https://docs.docker.com/engine/install/ubuntu/
sudo apt-get update
curl -fsSL https://get.docker.com -o get-docker.sh
sh ./get-docker.sh

# Start docker
sudo service docker start

# Join the cluster
sudo docker swarm join --token TOKEN SWARM_MANAGER_IP_ADDRESS:2377