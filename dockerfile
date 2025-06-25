FROM {{ REGISTRY UCHILE}}:8085/maven:3-jdk-8-slim AS build
#FROM maven:3-openjdk-11-slim AS build
#FROM maven:3-openjdk-17-slim AS build
WORKDIR /app

COPY src /app/src
COPY pom.xml /app/pom.xml
COPY settings.xml /root/.m2/settings.xml

RUN mvn clean package
RUN mv /app/target/*-jar-with-dependencies.jar /app/target/app.jar

FROM {{ REGISTRY UCHILE}}:8085/openjdk:8-jdk-alpine-tz2022c
#FROM openjdk:11-jdk-slim
#FROM openjdk:17-jdk-slim
WORKDIR /app

COPY --from=build /app/target/app.jar /app/app.jar

USER 1001

#ENTRYPOINT [ "java", "-server", "-Djava.security.egd=file:/dev/./urandom", "-Duser.timezone=America/Santiago", "-Xms128m", "-Xmx1g", "-jar", "/app/app.jar" ]
ENTRYPOINT [ "java", "-server", "-Djava.security.egd=file:/dev/./urandom", "-Duser.timezone=America/Santiago", "-Xms128m", "-Xmx1g", "-XX:+UseParNewGC", "-XX:+UseConcMarkSweepGC", "-XX:+CMSClassUnloadingEnabled", "-XX:+CMSParallelRemarkEnabled", "-XX:+DisableExplicitGC", "-jar", "/app/app.jar" ]
#ENTRYPOINT [ "java", "-server", "-Djava.security.egd=file:/dev/./urandom", "-Duser.timezone=America/Santiago", "-Xms128m", "-Xmx1g", "-XX:+UseParallelGC", "-jar", "/app/app.jar" ]
