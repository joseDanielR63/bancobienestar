FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Instalar curl para healthcheck
RUN apk add --no-cache curl

COPY --from=builder /app/target/*.jar app.jar

# Crear usuario no-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 8080

# Healthcheck CORREGIDO - sin comentarios en la misma línea
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080 || exit 1

ENTRYPOINT ["java", "-Dserver.address=0.0.0.0", "-jar", "app.jar"]