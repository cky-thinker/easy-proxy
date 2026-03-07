FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*-fat.jar app.jar
COPY src/main/resources/config.yaml config.yaml
EXPOSE 10090 10091
ENTRYPOINT ["java", "-cp", "app.jar", "com.cky.proxy.server.ProxyServer"]
