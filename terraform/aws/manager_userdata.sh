sudo yum install docker -y
sudo service docker start

# initialize swarm cluster and submit token for adding swarm worker nodes
sudo mkdir token
sudo docker swarm init | grep 'docker swarm join --token' > token/token.txt
sudo aws s3 cp token/token.txt s3://spark-failures-bucket/token.txt

# wait for swarm workers to connect to the cluster
while true; do nodes=$( docker node ls | awk 'NR > 1 {print $1}' | wc -l ); if ((nodes == 5)); then break; fi; sleep 2; done

# label other swarm nodes as workers
sudo docker node ls | grep -v 'Leader' | awk 'NR > 1 {print $1}' | while read line; do docker node update --label-add role=worker $line; done

# label current swarm node as swarm master
sudo docker node update --label-add role=master

# download docker-compose.yml file from s3 bucket (before that it should be uploaded there via terraform)
sudo aws s3 cp s3://spark-failures-bucket/docker-compose.yml ./docker-compose.yml

# run docker-compose stack to deploy Spark workers on the attached swarm nodes
docker stack deploy --compose-file docker-compose.yml swarm-cluster