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

# Copiar el JAR desde la etapa de construcción
COPY --from=builder /app/target/*.jar app.jar

# Puerto de la aplicación
EXPOSE 8080

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]