FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src ./src
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q -DskipTests package
RUN test -f target/*.jar || (echo "Maven не собрал JAR — см. логи выше" && exit 1)

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=builder /build/target/*.jar /app/app.jar
RUN test -f /app/app.jar || (echo "app.jar не найден в образе" && exit 1)
EXPOSE 8080
# НЕ /app.jar в корне FS — файл лежит в /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
