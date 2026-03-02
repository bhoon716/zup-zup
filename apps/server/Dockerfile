FROM gradle:8.5-jdk21 AS builder
WORKDIR /app

# 1. 의존성 캐싱을 위한 파일 복사
COPY build.gradle settings.gradle ./
COPY gradle gradle
COPY gradlew ./

# 2. 의존성 미리 다운로드 (소스코드 변경 시 재사용됨)
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

# 3. 소스코드 복사 및 빌드
COPY src src
RUN ./gradlew clean build -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
