FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*-fat.jar app.jar
COPY deploy/start.sh /app/start.sh
RUN chmod +x /app/start.sh
RUN mkdir -p /app/logs
RUN mkdir -p /app/config
ENV TZ=Asia/Shanghai
ENTRYPOINT ["/app/start.sh", "run"]
