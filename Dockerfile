
FROM container-registry.oracle.com/java/openjdk:21

WORKDIR /app
COPY chat-server.jar app.jar
EXPOSE 8881
ENTRYPOINT ["java", "-jar", "app.jar"]
