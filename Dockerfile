FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copiar archivos del proyecto
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# Dar permisos de ejecución al Maven Wrapper
RUN chmod +x ./mvnw

# Descargar dependencias (capa cacheable)
RUN ./mvnw dependency:go-offline

# Compilar la aplicación
RUN ./mvnw clean package -DskipTests

# Etapa de ejecución
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Instalar curl para healthcheck
RUN apk add --no-cache curl

# Copiar el JAR desde la etapa de construcción
COPY --from=builder /app/target/*.jar app.jar

# Crear usuario no-root (opcional pero recomendado)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Puerto de la aplicación
EXPOSE 8080

# Healthcheck para Coolify
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Ejecutar la aplicación con opciones optimizadas
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]