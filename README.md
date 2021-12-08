# spark-failures
This project contains all code required for testing Spark under a set of state, network, and resource level failures.

The testing environment supports two ways of deploying, on-premises in a cluster of servers and in AWS. For instructions on how to deploy the project in each environment read the sections below.

## AWS environment

###Usage

* Download [terraform](https://www.terraform.io/downloads.html) executable and add its location to PATH.
* Provide AWS account credentials (in ~/.aws/credentials file or via environment variables)
* Generate ssh-key (using `ssh-keygen` called **ec2/ec2.pub** and store it to **terraform/keypair** directory)
* Navigate to **terraform/aws** directory and run the following commands from there. 
* Initialize terraform (this needs to be run only once)
```sh
terraform init
```
* To build or update the infrastructure, run
```sh
terraform apply
```  
* When infrastructure is not required anymore, destroy it running
```sh
terraform destroy
```

###How it works

1. Terraform connects to AWS using credentials provided in ~/.aws/credentials file or via environment variables and creates all required infrastructure (VPC, subnet, EC2 machines etc.) if it is not there.
2. Upon creation of EC2 machines, terraform installs Docker on each machine and starts docker engine (see worker_userdata.sh)
3. On the swarm manager node, terraform initializes swarm cluster and then uses generated token to add workers to the cluster, running a corresponding command on each worker (see manager_userdata.sh and worker_userdata.sh).
4. Terraform creates docker overlay network.
5. Once the swarm cluster is all set up, swarm manager creates all required containers using docker-compose.yml file (terraform runs the command). The current cluster topology is as follows:
   * Node 1 (swarm manager) - Spark driver, Zookeeper (Spark HA mode)
   * Node 2 (swarm worker) - Generator, Kafka
   * Node 3 (swarm worker) - Spark master
   * Node 4 (swarm worker) - Spark worker
   * Node 5 (swarm worker) - Spark worker
   * Node 6 (swarm worker) - Spark worker
6. Upon creation, generator starts to generate event records and write them to a kafka topic. Driver creates a SparkContext, specifies operator topology and submits a job to Spark. This is run by swarm commands.

**Warning**

For testing purposes, terraform script creates public S3 buckets and places EC2 instances in a public subnet. Do not upload confidential data there, since it imposes security threats.


## Local environment (on-premise)
...

###How it works

1. Terraform connects to on-premise cluster machines, installs Docker on each machine and starts docker engine. All VMs should share the same network.
2. On the swarm manager node, terraform initializes swarm cluster and then uses generated token to add workers to the cluster, running a corresponding command on each worker.
4. Terraform creates docker overlay network.
5. Once the swarm cluster is all set up, swarm manager creates all required containers using docker-compose.yml file (terraform runs the command). The current cluster topology is as follows:
    * Node 1 (swarm manager) - Spark driver, Zookeeper (Spark HA mode)
    * Node 2 (swarm worker) - Generator, Kafka
    * Node 3 (swarm worker) - Spark master
    * Node 4 (swarm worker) - Spark worker
    * Node 5 (swarm worker) - Spark worker
    * Node 6 (swarm worker) - Spark worker
6. Upon creation, generator starts to generate event records and write them to a kafka topic. Driver creates a SparkContext, specifies operator topology and submits a job to Spark. This is run by swarm commands.

## Monitoring
...

## Test suite
...

## Notes

1. We should make sure that swarm load balancing will not impact distribution of events' records among Spark workers. It can be ensured via two ways:
    * Create swarm workers as different resources (on contrary to one replicated resource). Then traffic will not be load-balanced.
    * Make events' traffic going not through ingress network (load balancing is conducted by IPVS in a container created by docker swarm by default), which most probably will be the case since kafka and Spark workers will be deployed in the same overlay network.