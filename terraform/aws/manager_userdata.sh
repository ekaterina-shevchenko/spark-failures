sudo yum install docker -y
sudo service docker start
sudo docker swarm init #> parse output and send token to s3 bucket
# wait for all workers to connect to the cluster (maybe for-loop with if)