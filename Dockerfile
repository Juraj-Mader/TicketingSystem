# syntax=docker/dockerfile:1
# One Dockerfile for all services. The service is chosen with the build argument MODULE.

# Stage 1: build the JAR
FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl ${MODULE} -am package -DskipTests

# Stage 2: run the JAR
FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
COPY --from=build /app/${MODULE}/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]