FROM gradle:8-jdk21-alpine AS builder
WORKDIR /build
COPY . .
RUN gradle :services:notification:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=builder /build/services/notification/build/libs/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
