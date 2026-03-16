#!/bin/bash

# Find the jar file
if [ -f "app.jar" ]; then
    JAR_PATH="app.jar"
elif [ -f "/app/app.jar" ]; then
    JAR_PATH="/app/app.jar"
else
    # Try finding in current or target directories (for local dev)
    JAR_PATH=$(find . -name "easy-proxy-server-*-fat.jar" | head -n 1)
    if [ -z "$JAR_PATH" ]; then
        JAR_PATH=$(find ../target -name "easy-proxy-server-*-fat.jar" 2>/dev/null | head -n 1)
    fi
fi

if [ -z "$JAR_PATH" ]; then
    echo "Error: Jar file not found."
    exit 1
fi

echo "Using JAR: $JAR_PATH"

# Create logs directory
mkdir -p logs

# JVM Options
if [ -z "$JAVA_OPTS" ]; then
    JAVA_OPTS="-Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Shanghai \
    -Xlog:gc*:file=./logs/gc.log:time,tid,tags:filecount=2,filesize=10m \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=./logs/"
fi

MAIN_CLASS="com.cky.proxy.server.ProxyServer"

# Check mode
MODE=$1

if [ "$MODE" = "run" ]; then
    echo "Starting in foreground mode..."
    exec java $JAVA_OPTS -cp "$JAR_PATH" $MAIN_CLASS
else
    echo "Starting in background mode..."
    nohup java $JAVA_OPTS -cp "$JAR_PATH" $MAIN_CLASS > /dev/null 2>&1 &
    echo $! > tpid
    echo "Started with PID $!"
fi
