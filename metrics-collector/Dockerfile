#
# Build stage
#
FROM maven:3.8.4-openjdk-11-slim AS build
COPY pom.xml /home/app/pom.xml
COPY common/pom.xml /home/app/common/pom.xml
COPY generator/pom.xml /home/app/generator/pom.xml
COPY streaming/pom.xml /home/app/streaming/pom.xml
COPY structured-streaming/pom.xml /home/app/structured-streaming/pom.xml
COPY metrics-collector/src /home/app/metrics-collector/src
COPY metrics-collector/pom.xml /home/app/metrics-collector/pom.xml
RUN mvn -f /home/app/pom.xml -pl '!streaming,!structured-streaming,!generator,!common' install

#
# Package stage
#
FROM openjdk:11-jre-slim
COPY --from=build /home/app/metrics-collector/target/metrics-collector-fat-1.0.jar /usr/local/lib/metrics-collector.jar
CMD java -Dwait=180000 -DkafkaUrl=kafka:9092 -DjdbcUrl=jdbc:mysql://spark-mariadb:3306/spark -jar /usr/local/lib/metrics-collector.jar
