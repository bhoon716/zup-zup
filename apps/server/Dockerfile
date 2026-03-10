FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# CI에서 생성한 bootJar를 그대로 사용한다.
ARG JAR_FILE=build/libs/app.jar
COPY ${JAR_FILE} /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.config.additional-location=file:/app/config/", "-jar", "/app/app.jar"]
