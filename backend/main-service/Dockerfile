# Build main-service from backend root so the parent pom and modules are available.
FROM maven:3.8.8-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy the whole backend into the build context so parent POM and modules resolve
COPY . .

# Build only the main-service module and its dependencies
RUN mvn -B -DskipTests clean package -pl main-service -am

FROM eclipse-temurin:17-jre
WORKDIR /app
# copy the built jar (adjust if your artifact name differs)
COPY --from=build /workspace/main-service/target/*.jar app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m -Dspring.profiles.active=docker"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]