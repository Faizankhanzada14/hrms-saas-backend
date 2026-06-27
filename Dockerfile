# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn -q clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S hrms && adduser -S hrms -G hrms
COPY --from=build /app/target/hrms-backend.jar app.jar
RUN chown hrms:hrms app.jar
USER hrms

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=dev

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]
