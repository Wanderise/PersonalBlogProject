# syntax=docker/dockerfile:1.7
ARG DOCKER_IMAGE_PREFIX=
FROM ${DOCKER_IMAGE_PREFIX}library/maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY common/pom.xml common/pom.xml
COPY pojo/pom.xml pojo/pom.xml
COPY server/pom.xml server/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -B -nsu -N install -DskipTests
COPY pojo/src pojo/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -nsu -pl pojo install -DskipTests
COPY common/src common/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -nsu -pl common install -DskipTests
COPY server/src server/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -nsu -pl server package -DskipTests

FROM ${DOCKER_IMAGE_PREFIX}library/eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/server/target/server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
