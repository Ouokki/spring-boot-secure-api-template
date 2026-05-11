# ─── Stage 1: build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy wrapper first so the Gradle distribution layer is cached separately
# from source changes.
COPY gradle/ gradle/
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon --quiet

COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test -x spotlessCheck -x checkstyleMain

# ─── Stage 2: runtime ─────────────────────────────────────────────────────────
# eclipse-temurin:21-jre-alpine is ~80 MB, well under the 200 MB target.
FROM eclipse-temurin:21-jre-alpine AS runtime

# Non-root user — running as root in a container is a CIS benchmark finding.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder --chown=appuser:appgroup /app/build/libs/*.jar app.jar

USER appuser

# JVM tuning: use the container's cgroup CPU/memory limits instead of host values.
# -XX:+UseContainerSupport is on by default in JDK 11+; explicit for documentation.
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
