FROM gradle:8-jdk21-alpine AS builder
WORKDIR /build
COPY . .
RUN gradle :services:shipping:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=builder /build/services/shipping/build/libs/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
