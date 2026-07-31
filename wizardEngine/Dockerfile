# Stage 1: Build the fat JAR using sbt-assembly
FROM sbtscala/scala-sbt:eclipse-temurin-17.0.13_11_1.10.7_3.6.3 AS builder
WORKDIR /app
COPY . .
RUN sbt assembly

# Stage 2: Minimal JRE image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Copia esattamente il fat jar (WizardCardGame.jar) ignorando il jar base
COPY --from=builder /app/target/scala-*/WizardCardGame.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
