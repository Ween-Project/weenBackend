# Build stage
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine AS runner
WORKDIR /app

# Add a non-root user for security
RUN addgroup -S spring && adduser -S springboot -G spring
USER springboot:spring

COPY --from=build --chown=springboot:spring /app/target/ween-backend-*.jar app.jar

ENV PORT=5050
EXPOSE ${PORT}

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]
