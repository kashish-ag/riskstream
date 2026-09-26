# Build from the repository root:  docker build -f risk-service/Dockerfile .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY common common
COPY risk-service risk-service
# The other modules' poms are needed so the multi-module reactor resolves
COPY producer/pom.xml producer/pom.xml
COPY risk-service/pom.xml risk-service/pom.xml
RUN mvn -q -B -pl risk-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/risk-service/target/risk-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
