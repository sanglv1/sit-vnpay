FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY config ./config
COPY src ./src
RUN mvn -B -DskipTests -Dcheckstyle.skip=true package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S sit && adduser -S sit -G sit
COPY --from=build /app/target/sit-api-*.jar /app/app.jar
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh && chown -R sit:sit /app
USER sit
EXPOSE 8001
ENTRYPOINT ["/docker-entrypoint.sh"]
