FROM gradle:8.5.0-jdk21 AS build
WORKDIR /app

COPY build.gradle settings.gradle gradlew /app/
COPY gradle /app/gradle
RUN chmod +x ./gradlew \
 && ./gradlew dependencies --no-daemon --warning-mode=none

COPY . /app
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x ./gradlew && \
    ./gradlew clean bootJar --no-daemon --warning-mode=none

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]