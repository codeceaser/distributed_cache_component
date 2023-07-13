FROM openjdk:11-jre-slim
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} distributed_cache_component.jar
ENTRYPOINT ["java", "-jar", "/distributed_cache_component.jar"]