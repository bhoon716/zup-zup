FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup --system --gid 10001 sugang-user \
    && adduser --system --uid 10001 --ingroup sugang-user --home /app sugang-user \
    && mkdir -p /app/config /app/data/uploads \
    && chown -R 10001:10001 /app

# CI에서 생성한 bootJar를 그대로 사용한다.
ARG JAR_FILE=build/libs/app.jar
COPY --chown=10001:10001 ${JAR_FILE} /app/app.jar

EXPOSE 8080
USER 10001:10001
ENTRYPOINT ["java", "-Djava.io.tmpdir=/tmp", "-Dspring.config.additional-location=file:/app/config/", "-jar", "/app/app.jar"]
