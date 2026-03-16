FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*-fat.jar app.jar
RUN mkdir -p /app/logs
RUN mkdir -p /app/config
ENV TZ=Asia/Shanghai
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-Duser.timezone=Asia/Shanghai", "-Xlog:gc*:file=/app/logs/gc.log:time,tid,tags:filecount=2,filesize=10m", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/app/logs/", "-cp", "app.jar", "com.cky.proxy.client.ProxyClient"]
