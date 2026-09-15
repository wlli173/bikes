# syntax=docker/dockerfile:1

# =============================================================================
# Estágio 1 — Build: JDK + Maven para compilar a aplicação e gerar o JAR
# =============================================================================
FROM maven:3.9.16-eclipse-temurin-25-alpine AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:resolve

COPY src ./src
RUN mvn -B -DskipTests package \
    && APP_JAR="$(ls target/*.jar | grep -Ev 'original|sources|javadoc' | head -n 1)" \
    && cp "${APP_JAR}" /workspace/app.jar

# =============================================================================
# Estágio 2 — Runtime: imagem enxuta contendo apenas o JRE de produção
# =============================================================================
FROM eclipse-temurin:25-jre-alpine AS runtime

RUN apk add --no-cache tzdata wget \
    && addgroup -S spring \
    && adduser -S spring -G spring

ENV TZ=America/Sao_Paulo \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

WORKDIR /app
COPY --from=build --chown=spring:spring /workspace/app.jar /app/app.jar

USER spring:spring
EXPOSE 8080

HEALTHCHECK --interval=20s --timeout=5s --start-period=60s --retries=8 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
