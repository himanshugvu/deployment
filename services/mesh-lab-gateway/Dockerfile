FROM maven:3.9.11-amazoncorretto-25 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src src

RUN mvn -q -DskipTests package

FROM amazoncorretto:25-alpine
WORKDIR /app

COPY --from=build /workspace/target/mesh-lab-gateway-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# Heap sized from the container memory limit, not from host RAM.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
