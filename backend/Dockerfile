# syntax=docker/dockerfile:1
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw
COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:25-jre
RUN useradd --system --no-create-home --uid 10001 assessflow
WORKDIR /app
COPY --from=build /src/target/assessflow-backend-0.1.0.jar app.jar
USER 10001
EXPOSE 8080
ENV SERVER_PORT=8080
ARG SOURCE=https://github.com/DaviGama08/assessflow
ARG REVISION=unknown
ARG VERSION=0.1.0
LABEL org.opencontainers.image.title="AssessFlow API" \
      org.opencontainers.image.source=$SOURCE \
      org.opencontainers.image.revision=$REVISION \
      org.opencontainers.image.version=$VERSION
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
