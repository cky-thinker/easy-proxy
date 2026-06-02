# 使用 JDK 版本以支持 JFR (Java Flight Recorder) 和 jcmd 等诊断工具
FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY target/*-fat.jar app.jar
COPY deploy/start.sh /app/start.sh
RUN chmod +x /app/start.sh
EXPOSE 21090 21091
RUN mkdir -p /app/logs
RUN mkdir -p /app/config
ENV TZ=Asia/Shanghai

# 默认 JVM 参数，可通过 docker run -e JAVA_OPTS="..." 覆盖
# 开启 JFR 示例: -XX:StartFlightRecording=duration=60s,filename=/app/logs/recording.jfr
ENV JAVA_OPTS="-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai -Xlog:gc*:file=/app/logs/gc.log:time,tid,tags:filecount=2,filesize=10m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/"

ENTRYPOINT ["/app/start.sh", "run"]
