FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q package -DskipTests && cp target/*.jar app.jar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd --system wargame && useradd --system --gid wargame wargame
COPY --from=build /workspace/app.jar app.jar
USER wargame

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
