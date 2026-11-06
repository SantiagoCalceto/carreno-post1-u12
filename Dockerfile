# ── Etapa 1: compilacion con Maven ──────────────────────────────────
# Imagen oficial de Maven sobre Eclipse Temurin JDK 21 (Alpine). La imagen
# eclipse-temurin:21-jdk-alpine sola NO incluye Maven, por eso "mvn" fallaria.
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copiar pom.xml primero para aprovechar la cache de capas de Docker.
# Si las dependencias no cambian, Maven no las vuelve a descargar en builds siguientes.
COPY pom.xml .
RUN mvn dependency:go-offline -q -B

COPY src ./src
RUN mvn clean package -DskipTests -q -B

# ── Etapa 2: imagen de ejecucion (solo JRE) ─────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario no root: buena practica de seguridad en contenedores
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Copiar unicamente el JAR compilado desde la etapa builder
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# Docker (y Render/Railway) pueden consultar la salud del contenedor con Actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:${PORT:-8080}/actuator/health | grep -q '"status":"UP"' || exit 1

# MaxRAMPercentage: la JVM respeta el limite de memoria del contenedor (p. ej. 512 MB en Render free)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
