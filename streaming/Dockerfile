#
# Build stage
#
FROM maven:3.8.4-openjdk-8-slim AS build

COPY common /home/app/common
COPY generator /home/app/generator
COPY streaming /home/app/streaming
COPY structured-streaming /home/app/structured-streaming
COPY pom.xml /home/app/

# it's required for child modules to be able to find siblings modules in local maven repository
RUN cd /home/app/ && mvn clean install && cd /
RUN mvn -f /home/app/streaming/pom.xml clean package

#
# Package stage & spark download
#
FROM bitnami/spark:3.2.0

ENV SPARK_MODE=worker

COPY --from=build /home/app/streaming/target/streaming-fat-1.0.jar /usr/local/lib/streaming.jar

ENTRYPOINT ["/opt/bitnami/spark/bin/spark-submit", "--master=spark://spark-master:7077", \
            "--deploy-mode=client", "--driver-memory=3g", \
            "--executor-memory=3g", "--executor-cores=2", \
            "--conf", "spark.jars.ivy=/tmp/.ivy", \
            "--class=de.tum.spark.failures.streaming.Application", "/usr/local/lib/streaming.jar"]