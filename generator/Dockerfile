#
# Build stage
#
FROM maven:3.8.4-openjdk-11-slim AS build
COPY common /home/app/common
COPY generator /home/app/generator
COPY streaming /home/app/streaming
COPY structured-streaming /home/app/structured-streaming
COPY pom.xml /home/app/
RUN mvn -f /home/app/generator/pom.xml clean package

#
# Package stage
#
FROM openjdk:11-jre-slim
COPY --from=build /home/app/generator/target/generator-fat-1.0.jar /usr/local/lib/generator.jar
EXPOSE 8080
ENTRYPOINT ["java","-Dbootstrap.kafka.server=$BOOTSTRAP_KAFKA_SERVER","-jar","/usr/local/lib/generator.jar"]