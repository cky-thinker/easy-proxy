# Easy Proxy

English | [中文](./README.md)

A simple and efficient intranet penetration tool.

## Introduction

Easy Proxy is a high-performance intranet penetration tool developed based on Vert.x, supporting TCP traffic forwarding. It includes a server, a client, and a Web management interface, aiming to provide easy-to-use intranet penetration services.

## Project Structure

- `easy-proxy-server`: Server side, handles client connections, request forwarding, and Web management APIs.
- `easy-proxy-client`: Client side, deployed in the intranet, establishes connections with the server and forwards local service traffic.
- `easy-proxy-web`: Web management interface, developed with Vue 3 + Element Plus.
- `easy-proxy-common`: Common module, contains shared utility classes and data models.

## Requirements

- JDK 17
- Maven 3.6.0+
- Node.js 20+ (for Web development)

## Quick Start

### Server Deployment

```bash
# api server
docker run -d \
  --name easy-proxy-server \
  --network host \
  -v $(pwd)/config:/app/config \
  -v $(pwd)/data:/app/data \
  easy-proxy-server

# web management interface
docker run -d \
  --name easy-proxy-web \
  --network host \
  easy-proxy-web
```

### Client Deployment

```bash
docker run -d \
  --name easy-proxy-client \
  --network host \
  -v $(pwd)/config:/app/config \
  -v $(pwd)/data:/app/data \
  easy-proxy-client
```

## Tech Stack

- **Backend**: Java 17, Vert.x 4
- **Frontend**: Vue 3, TypeScript, Element Plus, Vite
- **Build Tools**: Maven, npm
