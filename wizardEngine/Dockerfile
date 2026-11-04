# Stage 1: Stage application and dependencies via sbt-native-packager
FROM sbtscala/scala-sbt:eclipse-temurin-17.0.13_11_1.10.7_3.6.3 AS builder
WORKDIR /app

# Cache dependencies by copying build definitions first
COPY build.sbt ./
COPY project ./project/
COPY .scalafmt.conf .scalafix.conf ./
RUN sbt update

# Copy source code and stage runtime scripts + libraries
COPY src ./src/
RUN sbt stage

# Stage 2: Minimal JRE image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/target/universal/stage /app
EXPOSE 8080
ENTRYPOINT ["/app/bin/wizardcardgame"]
