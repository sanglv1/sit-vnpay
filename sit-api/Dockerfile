FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY config ./config
COPY src ./src
RUN mvn -B -DskipTests -Dcheckstyle.skip=true package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r sit && useradd -r -g sit sit
COPY --from=build /app/target/sit-api-*.jar /app/app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh && chown -R sit:sit /app
USER sit
EXPOSE 8001
ENTRYPOINT ["/app/docker-entrypoint.sh"]
