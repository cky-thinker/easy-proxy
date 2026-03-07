FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*-fat.jar app.jar
COPY src/main/resources/config.yaml config.yaml
ENTRYPOINT ["java", "-cp", "app.jar", "com.cky.proxy.client.ProxyClient"]
