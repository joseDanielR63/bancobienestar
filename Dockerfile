FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copiar archivos de Maven wrapper
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Descargar dependencias (capa cacheable)
RUN ./mvnw dependency:go-offline

# Copiar código fuente y compilar
COPY src src
RUN ./mvnw clean package -DskipTests

# Etapa de ejecución - USAR JRE
FROM eclipse-temurin:21-jre-alpine
# Si no funciona, usar: FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copiar JAR desde la etapa de construcción
COPY --from=builder /app/target/*.jar app.jar

# Crear usuario no-root para seguridad (opcional)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 8080

# Mejor entrypoint con opciones de memoria
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]