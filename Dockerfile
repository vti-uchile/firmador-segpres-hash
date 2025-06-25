FROM maven:3-openjdk-8-slim AS build
# FROM maven:3-openjdk-11-slim AS build
# FROM maven:3-openjdk-17-slim AS build

WORKDIR /build

COPY src src
COPY pom.xml .
COPY settings.xml /root/.m2/settings.xml

RUN mvn clean package

RUN mv /build/target/*-jar-with-dependencies.jar /build/target/server.jar

FROM openjdk:8-jdk-slim
# FROM openjdk:11-jdk-slim
# FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/target/server.jar /app/server.jar

USER 1001

# ENTRYPOINT [ "java", "-server", "-Djava.security.egd=file:/dev/urandom", "-Duser.timezone=America/Santiago", "-Xms128m", "-Xmx1g", "-jar", "/app/server.jar" ]
# ENTRYPOINT [ "java", "-server", "-Djava.security.egd=file:/dev/urandom", "-Duser.timezone=America/Santiago", "-Xms128m", "-Xmx1g", "-XX:+UseParallelGC", "-jar", "/app/server.jar" ]
ENTRYPOINT [ "java", "-server", "-Djava.security.egd=file:/dev/urandom", "-Duser.timezone=America/Santiago", "-Xms128m", "-Xmx1g", "-XX:+UseParNewGC", "-XX:+UseConcMarkSweepGC", "-XX:+CMSClassUnloadingEnabled", "-XX:+CMSParallelRemarkEnabled", "-XX:+DisableExplicitGC", "-jar", "/app/server.jar" ]
