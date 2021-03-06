# spark-failures
This project contains all code required for testing Spark under a set of state, network, and resource level failures.

The testing environment supports two ways of deploying, on-premises in a cluster of servers and in AWS. For instructions on how to deploy the project in each environment read the sections below.
As a base image we use the latest Spark 3.2 [image](https://hub.docker.com/r/datamechanics/spark) provided by [Datamechanics](https://www.datamechanics.co/).

## AWS environment

### Usage

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

### How it works

1. Terraform connects to AWS using credentials provided in ~/.aws/credentials file or via environment variables and creates all required infrastructure (VPC, subnet, EC2 machines etc.) if it is not there.
2. Upon creation of EC2 machines, terraform installs Docker on each machine and starts docker engine (see worker_userdata.sh and manager_userdata.sh)
3. On the swarm manager node, terraform initializes swarm cluster and then uses generated token to add workers to the cluster, running a corresponding command on each worker (see manager_userdata.sh and worker_userdata.sh).
4. Once the swarm cluster is all set up, swarm manager creates docker overlay network and all required containers using docker-compose.yml file (terraform runs the command). The current cluster topology is as follows:
   * Node 1 (swarm manager) - Spark driver, Zookeeper (Spark HA mode, Kafka), Generator, Kafka
   * Node 2 (swarm worker) - Spark master
   * Node 3 (swarm worker) - Spark worker
   * Node 4 (swarm worker) - Spark worker
   * Node 5 (swarm worker) - Spark worker
5. Upon creation, generator starts to generate event records and write them to a kafka topic. Driver creates a SparkContext, specifies operator topology and submits a job to Spark. This is run by swarm commands.

**Warning**

For testing purposes, terraform script creates public S3 buckets and places EC2 instances in a public subnet. Do not upload confidential data there, since it imposes security threats.


## Local environment (on-premise)
...

### How it works

1. Terraform connects to on-premise cluster machines, installs Docker on each machine and starts docker engine. All VMs should share the same network.
2. On the swarm manager node, terraform initializes swarm cluster and then uses generated token to add workers to the cluster, running a corresponding command on each worker.
3. Once the swarm cluster is all set up, swarm manager creates docker overlay network and all required containers using docker-compose.yml file (terraform runs the command). The current cluster topology is as follows:
    * Node 1 (swarm manager) - Spark driver, Zookeeper (Spark HA mode, Kafka), Generator, Kafka
    * Node 2 (swarm worker) - Spark master
    * Node 3 (swarm worker) - Spark worker
    * Node 4 (swarm worker) - Spark worker
    * Node 5 (swarm worker) - Spark worker
4. Upon creation, generator starts to generate event records and write them to a kafka topic. Driver creates a SparkContext, specifies operator topology and submits a job to Spark. This is run by swarm commands.

## Implementation details

### Exposed ports

**Spark master**
   * 8080 - WebUI port
   * 7077 - port used by spark workers to connect to the master

**Spark worker**
   * 8081 - ...

**Kafka**
   * 9092 - advertized port for client connections

**Zookeeper**
   * 2181 - client port
   * 2888 - follower port
   * 3888 - election port
   * 8080 - AdminServer port

## Monitoring
...

## Test suite
...

## Notes

1. We should make sure that swarm load balancing will not impact distribution of events' records among Spark workers. It can be ensured via two ways:
   * Create swarm workers as different resources (on contrary to one replicated resource). Then traffic will not be load-balanced.
   * Make events' traffic going not through ingress network (load balancing is conducted by IPVS in a container created by docker swarm by default), which most probably will be the case since kafka and Spark workers will be deployed in the same overlay network.
   
2. For Generator to provide required for tests throughput, the following Kafka Producer tuning was required:
   * Disabling DEBUG logging (it produced too much IO traffic upon sending records to Kafka) via changing log level in logback.xml
   * Producer.flush() is a blocking operation that parks threads for a while (up to ~2 seconds per one flush), thus it was moved to a separate thread.
   * Producer's `buffer.memory` default value is 32 MB, which is not enough for such workload. Threads started to spend up to 65% of time waiting for memory allocation. Changing the setting to ~100 MB increased throughput drastically. (When thread filled a batch, it asks the buffer to allocate a new batch. If buffer is full, thread is parked until buffer gets enough free space).
   * `buffer.memory` should be chosen as a multiple of `batch.size`, because otherwise some residual memory would have never been allocated, since the buffer allocates memory in batches.
   * Increasing `batch.size` also increases throughput, since more data is sent "at one time".
   * By default, `max.request.size` is 1 MB, which slows down sending data to Kafka. Setting it 5 MB has increased throughput.
   * Another important component of performance tuning is compression via `compression.type` property.
   * Tuning parameter `linger.ms` hasn't shown any noticeable difference. This parameter by default is set to 0 and specifies how many `ms` data is additionally "parked" for new data to come so that they can sent together in one request (to get more data is sent per request).
   * Parameter `max.in.flight.requests.per.connection`defines how many messages can be sent to Kafka without acknowledgement. By default, it equals to 5. Increasing it may increase throughput.
   * Parameter `acks` specifies delivery guarantees and can be one of three values: `none` (fire and forget), `one` (only leader-broker for this partition acknowledges receiving), `all` (all followers [**that have offsets in-sync**](https://www.confluent.io/blog/5-things-every-kafka-developer-should-know/) acknowledge receiving for leader-broker to send "success" result back to producer).
   
3. The used Kafka docker image creates a volume at `/kafka`. If data is not deleted via Producer API call, it can lead to device-out-of-memory. If it happened, clean up the volume manually in docker's `/var/lib/docker/volumes` (follow [these](https://stackoverflow.com/questions/38532483/where-is-var-lib-docker-on-mac-os-x) instructions) and better mount a host's folder to `/kafka` to be able to delete it easily manually (via docker-compose `volumes`).

4. Optimizations applied to Spark Streaming job:
   * As TCP connections cannot be serialized and transferred via network, kafka producers must be created on executors. It can be done via creating the producer per partition:
   ```
   dstream.foreachRDD { rdd =>
      rdd.foreachPartition { partitionOfRecords =>
      val producer = createKafkaProducer()
      partitionOfRecords.foreach { message =>
         connection.send(message)
      }
      producer.close()
      }
   }
   ```
   This approach is better than creating executor per each record, but it's not very scalable either, because then every `batch.size`time interval (e.g. 2 seconds) there will be created and closed **number.of.partitions** producers.
   Better way is to use Spark broadcast mechanism. [KafkaSink](https://blog.allegro.tech/2015/08/spark-kafka-integration.html) class is a smart wrapper for a Kafka producer. Instead of sending the producer itself, we send only a ???recipe??? how to create it in an executor. 
   The class is serializable because Kafka producer is initialized just before first use on an executor. 
   Constructor of KafkaSink class takes a function which returns Kafka producer lazily when invoked. Once the Kafka producer is created, it is assigned to producer variable to avoid initialization on every send() call.
   
   ```
   val kafkaSink = sparkContext.broadcast(KafkaSink(conf))

   dstream.foreachRDD { rdd =>
      rdd.foreach { message =>
         kafkaSink.value.send(message)
      }
   }
   ```