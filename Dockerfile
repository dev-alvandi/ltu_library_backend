FROM ubuntu:latest
LABEL authors="noah"

# Base image for Java 21 applications
FROM openjdk:21-jdk-slim

# Label for documentation (optional)
LABEL authors="noah"

# Set the working directory inside the container
WORKDIR /app

# Copy the compiled JAR file from host into the container
COPY target/dbbServer-0.0.1-SNAPSHOT.jar app.jar

# Copy the wait-for-it.sh script and make it executable
COPY .env .env
COPY wait-for-it.sh wait-for-it.sh
RUN chmod +x wait-for-it.sh

# Default command: wait for MySQL to be ready, then start the app
ENTRYPOINT ["sh", "-c", "./wait-for-it.sh mysql:3306 -- java -jar app.jar"]