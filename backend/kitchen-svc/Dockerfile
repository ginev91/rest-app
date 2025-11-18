FROM eclipse-temurin:17-jre

WORKDIR /app

# Allow overriding the jar name if needed
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# Reasonable default JVM options; override at runtime with env var JAVA_OPTS
ENV JAVA_OPTS="-Xms256m -Xmx512m -Dspring.profiles.active=docker"

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]