import * as dotenv from "dotenv";
import { EasyProxyClient, EasyProxyClientConfig } from "./EasyProxyClient";
import * as path from "path";

// Load environment variables from .env file
dotenv.config();

function start() {
    const config: EasyProxyClientConfig = {
        serverIp: process.env.EP_SERVER_IP || "127.0.0.1",
        serverPort: parseInt(process.env.EP_SERVER_PORT || "21090", 10),
        webPort: parseInt(process.env.EP_WEB_PORT || "21092", 10),
        token: process.env.EP_TOKEN || "default-token",
        configDir:
            process.env.EP_CONFIG_DIR || path.join(process.cwd(), "config"),
    };

    console.log("Starting EasyProxyClient with config:", {
        ...config,
        token: "***", // Hide token in logs
    });

    const client = new EasyProxyClient(config);

    client.on("connected", () => {
        console.info("Successfully connected to Easy Proxy Server");
    });

    client.on("error", (err) => {
        console.error("Client encountered an error:", err);
    });

    client.start().catch((err) => {
        console.error("Failed to start client:", err);
        process.exit(1);
    });

    // Handle process termination
    const shutdown = () => {
        console.info("Shutting down...");
        client.stop();
        process.exit(0);
    };

    process.on("SIGINT", shutdown);
    process.on("SIGTERM", shutdown);
}

start();
