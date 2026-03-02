# syntax=docker/dockerfile:1.7
FROM gradle:8.5-jdk21 AS builder
WORKDIR /workspace

# Gradle wrapper/설정 파일을 먼저 복사해 의존성 레이어 캐시 효율을 높인다.
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew

COPY src src

# 테스트는 CI job에서 수행하므로 이미지 빌드에서는 bootJar만 생성한다.
RUN --mount=type=cache,target=/home/gradle/.gradle \
    ./gradlew bootJar -x test --no-daemon --build-cache

# plain jar를 제외한 실행 jar만 추출한다.
RUN set -eux; \
    JAR_PATH="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)"; \
    test -n "$JAR_PATH"; \
    cp "$JAR_PATH" /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/app.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.config.additional-location=file:/app/config/ -jar /app/app.jar"]
