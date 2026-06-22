FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B package -DskipTests \
    && cp target/*.jar app.jar

FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

RUN groupadd --system spring \
    && useradd --system --gid spring --no-create-home spring

COPY --from=build --chown=spring:spring /workspace/app.jar app.jar

USER spring:spring

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
