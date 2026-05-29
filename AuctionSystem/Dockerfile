# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/AuctionSystem-1.0-SNAPSHOT.jar ./server.jar
COPY --from=builder /app/target/dependency ./dependency

# Set default environment variables
ENV DB_URL=jdbc:mysql://localhost:3306/auction_system?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh
ENV DB_USERNAME=root
ENV DB_PASSWORD=rootpass
ENV SERVER_HOST=0.0.0.0
ENV SERVER_PORT=18368
ENV API_BASE_URL=http://localhost:8080/api/

EXPOSE 8080
EXPOSE 18368

# Run Backend Server
CMD ["java", "-cp", "server.jar:dependency/*", "com.team4.server.Server"]
