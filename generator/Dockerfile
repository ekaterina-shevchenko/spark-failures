#
# Build stage
#
FROM maven:3.8.4-openjdk-11-slim AS build
COPY pom.xml /home/app/pom.xml
COPY common/src /home/app/common/src
COPY common/pom.xml /home/app/common/pom.xml
COPY generator /home/app/generator
COPY streaming/pom.xml /home/app/streaming/pom.xml
COPY metrics-collector/pom.xml /home/app/metrics-collector/pom.xml
COPY structured-streaming/src /home/app/structured-streaming/src
COPY structured-streaming/pom.xml /home/app/structured-streaming/pom.xml
RUN mvn -f /home/app/pom.xml -pl '!streaming,!structured-streaming' install

#
# Package stage
#
FROM openjdk:11-jre-slim
COPY --from=build /home/app/generator/target/generator-fat-1.0.jar /usr/local/lib/generator.jar
EXPOSE 8080
CMD java -Dbootstrap.kafka.server=$BOOTSTRAP_KAFKA_SERVER -Dthroughput=$THROUGHPUT -jar /usr/local/lib/generator.jar