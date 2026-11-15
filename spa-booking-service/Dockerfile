# Stage 1: Build JAR bằng Gradle (Java 21)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy Gradle wrapper và file cấu hình trước để cache dependencies
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

# Copy source code và build file .jar (bỏ qua test)
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Chạy JAR trên JRE 21 nhẹ
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]