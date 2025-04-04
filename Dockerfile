FROM openjdk:17

LABEL authors="Noah"

# Copy the local wait-for-it.sh script into the Docker image
COPY wait-for-it.sh /wait-for-it.sh

# Add the Spring Boot application JAR file to the container
ADD target/dbbServer.jar dbbServer.jar

# Ensure execute permissions for the script
RUN chmod +x /wait-for-it.sh

# Define entrypoint with `wait-for-it`
ENTRYPOINT ["sh", "/wait-for-it.sh", "mysqldb:3306", "--", "java", "-jar", "/dbbServer.jar"]