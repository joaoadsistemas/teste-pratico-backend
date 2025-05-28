FROM eclipse-temurin:17-jdk-jammy AS builder

RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]