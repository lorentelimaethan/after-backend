FROM maven:3.9.13-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN mkdir logs && chown -R 10001:10001 /app
COPY --from=build --chown=10001:10001 /workspace/target/after-0.0.1-SNAPSHOT.jar app.jar

USER 10001
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
