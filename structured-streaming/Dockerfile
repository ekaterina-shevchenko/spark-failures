#
# Build stage
#
FROM maven:3.8.4-openjdk-11-slim AS build

COPY common /home/app/common
COPY generator/pom.xml /home/app/generator/pom.xml
COPY streaming/pom.xml /home/app/streaming/pom.xml
COPY structured-streaming /home/app/structured-streaming
COPY pom.xml /home/app/
COPY metrics-collector/pom.xml /home/app/metrics-collector/pom.xml

# it's required for child modules to be able to find siblings modules in local maven repository
RUN cd /home/app/ && mvn -projects !streaming,!metrics-collector,!generator package

#
# Package stage & spark download
#
FROM bitnami/spark:3.2.0

ENV SPARK_MODE=worker

COPY --from=build /home/app/structured-streaming/target/structured-streaming-fat-1.0.jar /usr/local/lib/structured-streaming.jar
COPY configs/spark/metrics.properties /opt/bitnami/spark/conf/metrics.properties

ENTRYPOINT ["/opt/bitnami/spark/bin/spark-submit", "--master=spark://spark-master:7077", \
            "--deploy-mode=client", "--driver-memory=5g", "--num-executors=6", \
            "--executor-memory=5g", "--executor-cores=3", \
            "--conf", "spark.jars.ivy=/tmp/.ivy", \
            "--conf", "spark.ui.prometheus.enabled=true", \
            "--conf", "spark.executor.processTreeMetrics.enabled=true", \
            "--class=de.tum.spark.failures.structured.streaming.Application", "/usr/local/lib/structured-streaming.jar"]