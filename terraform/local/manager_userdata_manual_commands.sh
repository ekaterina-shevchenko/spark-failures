# Docker installation via the convenience script
# For other methods follow the link https://docs.docker.com/engine/install/ubuntu/
sudo apt-get update
curl -fsSL https://get.docker.com -o get-docker.sh
sh ./get-docker.sh

# Start docker
sudo service docker start

# Initialize swarm cluster and submit token for adding swarm worker nodes
sudo mkdir token
sudo chmod 777 token # Allows rwx access to all user groups
sudo docker swarm init | grep 'docker swarm join --token' > token/token.txt

# Copy contents of the file manually to other machines or use remote storage e.g. S3

# wait for swarm workers to connect to the cluster
while true; do nodes=$( docker node ls | awk 'NR > 1 {print $1}' | wc -l ); if ((nodes == 5)); then break; fi; sleep 2; done

# label other swarm nodes as workers except one
sudo docker node ls | grep -v 'Leader' | awk 'NR > 2 {print $1}' | while read line; do sudo docker node update --label-add role=worker $line; done
# can be checked via "sudo docker node ls -f "node.label=role=worker""

# label one swarm node as master
sudo docker node ls | grep -v 'Leader' | awk 'NR == 2 {print $1}' | while read line; do sudo docker node update --label-add role=master $line; done
# can be checked via "sudo docker node ls -f "node.label=role=master""

# label current swarm node as swarm manager
sudo docker node ls | grep 'Leader' | awk '{print $1}' | while read line; do sudo docker node update --label-add role=manager $line; done
# can be checked via "sudo docker node ls -f "node.label=role=manager""

# You can check that all nodes were labeled correctly by the command
# sudo docker node ls -q | sudo xargs docker node inspect -f '{{ .ID }} [{{ .Description.Hostname }}]: {{ range $k, $v := .Spec.Labels }}{{ $k }}={{ $v }} {{end}}'

# clone git project
git clone https://github.com/ekaterina-shevchenko/spark-failures.git
# or update if it already presents
git pull origin main

# build driver image (pick one of the two)
sudo docker build -f streaming/Dockerfile -t driver .
sudo docker build -f structured-streaming/Dockerfile -t driver .

# run docker-compose stack to deploy Spark workers on the attached swarm nodes
sudo docker stack deploy --compose-file docker-compose.yml swarm-cluster

# list swarm services
sudo docker service ls

# inspect specific service
sudo docker service inspect --pretty SERVICE_NAME