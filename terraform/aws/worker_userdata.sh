sudo yum install docker -y
sudo service docker start

# wait for token to appear in the s3 bucket
while true; do token=$( sudo aws s3 ls s3://spark-failures-bucket | grep "token.txt" | wc -l ); if ((token == 1)); then break; fi; sleep 2; done

# download token from s3 bucket
sudo aws s3 cp s3://spark-failures-bucket/token.txt ./token.txt

# add itself to the swarm cluster using the token
bash token.txt