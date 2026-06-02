# Easy-Proxy Benchmark 工具使用说明

本项目是一个基于 Java 25 虚拟线程构建的高性能压测工具，专门用于测试 `easy-proxy` 转发功能的稳定性、吞吐量和延迟。

## 1. 环境准备

### 1.1 操作系统优化 (Linux/macOS)
在高并发测试前，必须增加文件描述符限制，否则会遇到 `Too many open files` 错误。
```bash
# 临时生效
ulimit -n 65535
```

### 1.2 编译
在项目根目录下执行：
```bash
mvn clean package -pl easy-proxy-benchmark -am
```
编译完成后，在 `easy-proxy-benchmark/target/` 目录下会生成 `easy-proxy-benchmark-0.0.1-SNAPSHOT.jar`。

## 2. 测试操作详解

### 2.1 准备目标服务
在**客户端 (Client)** 所在机器上，启动一个简单的 HTTP 服务作为测试目标：
```bash
# 使用 python 快速启动一个 HTTP 服务
python3 -m http.server 8080
```
确保该服务已通过 `easy-proxy` 成功映射到**服务端 (Server)** 的公网端口（假设为 `21092`）。

### 2.2 执行吞吐量测试 (Throughput)
测量代理转发大文件时的带宽利用率。
- **操作命令**：
  ```bash
  java -jar easy-proxy-benchmark-0.0.1-SNAPSHOT.jar throughput <并发数> <总请求数> <代理URL>
  ```
- **示例**：
  ```bash
  # 50并发，总计1000次请求，下载10MB文件
  java -jar easy-proxy-benchmark-0.0.1-SNAPSHOT.jar throughput 50 1000 http://server-ip:21092/big-file.zip
  ```

### 2.3 执行延迟测试 (Latency)
测量代理转发带来的额外耗时（使用 HEAD 请求以排除数据传输干扰）。
- **操作命令**：
  ```bash
  java -jar easy-proxy-benchmark-0.0.1-SNAPSHOT.jar latency <并发数> <总请求数> <代理URL>
  ```
- **示例**：
  ```bash
  # 10并发，总计5000次请求
  java -jar easy-proxy-benchmark-0.0.1-SNAPSHOT.jar latency 10 5000 http://server-ip:21092/
  ```

### 2.4 执行并发连接测试 (Connection)
测试系统能维持的最大同时在线连接数。
- **操作命令**：
  ```bash
  java -jar easy-proxy-benchmark-0.0.1-SNAPSHOT.jar connection <目标连接数> 0 <代理URL>
  ```
- **示例**：
  ```bash
  # 尝试建立并维持 10000 个长连接
  java -jar easy-proxy-benchmark-0.0.1-SNAPSHOT.jar connection 10000 0 http://server-ip:21092/
  ```

### 2.5 执行稳定性测试 (Stability)
验证系统在长时间运行（Soak Test）下的稳定性，主要观察是否有内存泄漏或连接异常断开。
- **操作命令**：
  ```bash
  java -jar easy-proxy-benchmark-0.0.1-SNAPSHOT.jar stability <并发数> <持续时间(秒)> <代理URL>
  ```
- **示例**：
  ```bash
  # 使用 50 并发，持续运行 24 小时 (86400秒)
  java -jar easy-proxy-benchmark-0.0.1-SNAPSHOT.jar stability 50 86400 http://server-ip:21092/big-file
  ```
- **监控重点**：
  - **内存曲线**：通过 `jstat -gcutil <pid> 1000` 观察堆内存回收情况。
  - **句柄数**：观察 `lsof -p <pid> | wc -l` 是否随时间持续增长。
  - **错误率**：查看报告中的 `Failure` 计数。

## 3. 监控与分析

### 3.1 资源监控
建议在压测期间使用 `top` 或 `htop` 观察 `easy-proxy-server` 的 CPU 和内存占用。

### 3.2 使用 JFR 进行深度分析
如果需要排查性能瓶颈或内存泄漏，可以开启 Java Flight Recorder：

#### 本地运行
```bash
java -XX:StartFlightRecording=duration=60s,filename=myrecording.jfr -jar easy-proxy-benchmark-0.0.1-SNAPSHOT.jar ...
```

#### Docker 容器内运行 (Server 端)
`easy-proxy-server` 的 Docker 镜像已切换至 JDK 版本，支持直接开启 JFR。
- **启动时开启**：
  ```bash
  docker run -e JAVA_OPTS="-XX:StartFlightRecording=duration=60s,filename=/app/logs/server_profile.jfr" ...
  ```
- **运行中动态开启 (使用 jcmd)**：
  ```bash
  # 进入容器
  docker exec -it <container_id> jcmd app.jar JFR.start duration=60s filename=/app/logs/dynamic.jfr
  ```
之后使用 JDK 自带的 `jvisualvm` 或 `Mission Control` 打开 `.jfr` 文件分析。

## 4. 结果解读
- **Total Transferred**: 总传输数据量。
- **Avg Latency**: 平均每次请求的响应时间（单位 ms）。
- **Throughput**: 实际吞吐率（单位 MB/s），应接近网络带宽上限。
- **Failure**: 失败请求数，正常情况下应为 0。
