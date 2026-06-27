# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
# Copy pom.xml and resolve dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Copy the built jar file from the builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
