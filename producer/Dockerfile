# Build from the repository root:  docker build -f producer/Dockerfile .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY common common
COPY producer producer
# The other modules' poms are needed so the multi-module reactor resolves
COPY producer/pom.xml producer/pom.xml
COPY risk-service/pom.xml risk-service/pom.xml
RUN mvn -q -B -pl producer -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/producer/target/producer-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
