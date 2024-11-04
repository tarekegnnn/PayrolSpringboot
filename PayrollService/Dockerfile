# Base image
FROM openjdk:21-jdk-slim
# Set the working directory
WORKDIR /app
# Copy the application JAR file to the container
COPY target/PayrollService-0.0.1-SNAPSHOT.jar app.jar
# Expose the port the application runs on
EXPOSE 5252
# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]