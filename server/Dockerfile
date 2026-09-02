# ── Build stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy gradle wrapper first (layer cache)
COPY ecommerce/gradlew ecommerce/gradlew.bat ./
COPY ecommerce/gradle/ gradle/
RUN chmod +x gradlew

# Download deps (cached if build.gradle unchanged)
COPY ecommerce/build.gradle ecommerce/settings.gradle ./
RUN ./gradlew dependencies --no-daemon -q || true

# Build
COPY ecommerce/ ./
RUN ./gradlew bootJar --no-daemon -x test

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /app/build/libs/*.jar app.jar
RUN chown spring:spring app.jar
USER spring
EXPOSE 6000
ENTRYPOINT ["java", "-jar", "app.jar"]
