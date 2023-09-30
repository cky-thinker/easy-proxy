## Easy proxy

一个简单的内网穿透工具

## 开发

### 环境要求

- JDK 8
- Maven 3.6.0+

### mvn命令

To launch your tests:

```
mvn clean test
```

To package your application:

```
mvn clean package
```

To run your application:

```
mvn clean compile exec:java
```

## 业务流程

1. server start (system channel: 10090)
2. client start
3. client connect
4. client auth
5. server create data channel (data port: 10090)

