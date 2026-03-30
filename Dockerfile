# Multi-stage build for optimized image size
# Stage 1: Build Stage (Maven Build)
FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests

# Runtime stage
FROM --platform=linux/amd64 eclipse-temurin:17-jre-alpine

# Set working directory
WORKDIR /app
# ສ້າງ logs directory ກ່ອນ (ໃນຖານະ root)
RUN mkdir -p /app/logs

# Create non-root user for security
#RUN addgroup -S spring && adduser -S spring -G spring
#USER spring:spring

# ສ້າງ logs directory ແລະໃຫ້ສິດ
# RUN mkdir -p /app/logs && \
#     chown -R spring:spring /app/logs && \
#     chmod -R 755 /app/logs
#       docker build -t api-gateway:1.0.0 .

# Copy JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]


#docker build --platform linux/amd64  -t 172.16.4.62:5000/customer/lmps-service-1:1.0.0
#docker push 172.16.4.62:5000/customer/lmps-service-1:1.0.0