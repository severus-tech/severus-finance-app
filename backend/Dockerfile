# Stage 1: Build application
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY src src

RUN chmod +x mvnw
RUN ./mvnw package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /deployments

RUN addgroup -S quarkus && adduser -S quarkus -G quarkus

COPY --chown=quarkus:quarkus --from=builder /workspace/target/quarkus-app/lib/ /deployments/quarkus-app/lib/
COPY --chown=quarkus:quarkus --from=builder /workspace/target/quarkus-app/*.jar /deployments/quarkus-app/
COPY --chown=quarkus:quarkus --from=builder /workspace/target/quarkus-app/app/ /deployments/quarkus-app/app/
COPY --chown=quarkus:quarkus --from=builder /workspace/target/quarkus-app/quarkus/ /deployments/quarkus-app/quarkus/

USER quarkus

EXPOSE 8080

ENV PORT=8080
ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dquarkus.http.host=0.0.0.0 -Dquarkus.http.port=${PORT} -jar quarkus-app/quarkus-run.jar"]
