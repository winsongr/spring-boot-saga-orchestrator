FROM gradle:8-jdk21-alpine AS builder
WORKDIR /build
COPY . .
RUN gradle :services:inventory:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=builder /build/services/inventory/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
