#
# Build stage
#
FROM maven:3.8.4-openjdk-8-slim AS build

COPY common/src /home/app/common/src
COPY common/pom.xml /home/app/common/pom.xml
COPY generator/src /home/app/generator/src
COPY generator/pom.xml /home/app/generator/pom.xml
COPY streaming/src /home/app/streaming/src
COPY streaming/pom.xml /home/app/streaming/pom.xml
COPY structured-streaming/src /home/app/structured-streaming/src
COPY structured-streaming/pom.xml /home/app/structured-streaming/pom.xml
COPY pom.xml /home/app/

# it's required for child modules to be able to find siblings modules in local maven repository
RUN cd /home/app/ && mvn clean install && cd /

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