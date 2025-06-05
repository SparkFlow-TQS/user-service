FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

COPY pom.xml mvnw ./
COPY .mvn ./.mvn
RUN ./mvnw dependency:go-offline

COPY . .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app
COPY --from=build /app/target/user-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
