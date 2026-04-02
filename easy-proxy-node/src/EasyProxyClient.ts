import * as tls from "tls";
import * as net from "net";
import * as fs from "fs";
import { Message, MessageDecoder } from "./Message";
import { CertDownloader } from "./CertDownloader";
import { EventEmitter } from "events";

export interface EasyProxyClientConfig {
    serverIp: string;
    serverPort: number;
    webPort: number;
    token: string;
    configDir?: string;
}

export class EasyProxyClient extends EventEmitter {
    private mngSocket: tls.TLSSocket | null = null;
    private config: EasyProxyClientConfig;
    private configDir: string;
    private waitTime = 1000;
    private cert: Buffer | null = null;

    private dataSockets: Map<string, tls.TLSSocket> = new Map();
    private proxySockets: Map<string, net.Socket> = new Map();

    constructor(config: EasyProxyClientConfig) {
        super();
        this.config = config;
        this.configDir = config.configDir || "./config";
    }

    public async start() {
        try {
            const certPath = await CertDownloader.downloadIfNotExists(
                this.config.serverIp,
                this.config.webPort,
                this.configDir,
            );
            this.cert = fs.readFileSync(certPath);
            this.connectMngServer();
        } catch (error) {
            console.error("Failed to start EasyProxyClient:", error);
            this.emit("error", error);
            this.retry();
        }
    }

    private connectMngServer() {
        console.debug("EP>>ClientMng>> Connect mng server");

        if (!this.cert) {
            this.retry();
            return;
        }

        this.mngSocket = tls.connect({
            host: this.config.serverIp,
            port: this.config.serverPort,
            ca: [this.cert],
            checkServerIdentity: () => undefined, // Bypass hostname check, but verify against downloaded cert
        });

        const decoder = new MessageDecoder();

        this.mngSocket.on("secureConnect", () => {
            console.debug("EP>>ClientMng>> Connect success");
            this.waitTime = 1000;
            this.sendAuth();
            this.emit("connected");
        });

        this.mngSocket.on("data", (data: Buffer) => {
            try {
                decoder.append(data);
                let msg: Message | null;
                while ((msg = decoder.decode()) !== null) {
                    this.handleMngMessage(msg);
                }
            } catch (err: any) {
                console.error(
                    `EP>>ClientMng>> Decode error: ${err.message}. Disconnecting...`,
                );
                if (this.mngSocket) {
                    this.mngSocket.destroy();
                }
            }
        });

        this.mngSocket.on("close", () => {
            console.error("EP>>ClientMng>> Connect closed");
            this.handleMngClose();
            this.retry();
        });

        this.mngSocket.on("error", (err) => {
            console.error("EP>>ClientMng>> Connect error:", err.message);
            // close event will follow
        });
    }

    private sendAuth() {
        if (this.mngSocket && !this.mngSocket.destroyed) {
            const msg = Message.createAuthMsg(this.config.token);
            this.mngSocket.write(msg.encode());
        }
    }

    private handleMngMessage(msg: Message) {
        console.debug(`EP>>ClientMng>> read type: ${msg.type}`);
        switch (msg.type) {
            case Message.CONNECT:
                this.processConnect(msg);
                break;
            case Message.DISCONNECT:
                this.processDisconnect(msg);
                break;
        }
    }

    private processConnect(msg: Message) {
        console.debug("EP>>ClientMng>> connect");
        const userId = msg.token;
        const proxyAddress = msg.data.toString("utf-8");

        // Parse address: "ip:port"
        const parts = proxyAddress.split(":");
        if (parts.length !== 2) {
            console.error(
                `EP>>ClientMng>> Proxy address '${proxyAddress}' is not valid`,
            );
            this.sendDisconnect(userId);
            return;
        }

        const ip = parts[0];
        const port = parseInt(parts[1], 10);

        if (isNaN(port)) {
            console.error(
                `EP>>ClientMng>> Proxy address '${proxyAddress}' port is not valid`,
            );
            this.sendDisconnect(userId);
            return;
        }

        console.debug("EP>>ClientMng>> Create app proxy socket");

        const proxySocket = new net.Socket();

        proxySocket.connect(port, ip, () => {
            this.initProxyDataConnection(userId, proxySocket);
        });

        proxySocket.on("error", (err) => {
            console.error(
                `EP>>ClientMng>> Proxy socket '${proxyAddress}' connect fail, ${err.message}`,
            );
            this.sendDisconnect(userId);
        });
    }

    private initProxyDataConnection(userId: string, proxySocket: net.Socket) {
        console.debug(
            `EP>>ClientProxy>> Proxy socket ${proxySocket.remoteAddress}:${proxySocket.remotePort} create success`,
        );
        this.proxySockets.set(userId, proxySocket);

        console.debug("EP>>ClientProxy>> Create data socket");

        const dataSocket = tls.connect({
            host: this.config.serverIp,
            port: this.config.serverPort,
            ca: [this.cert!],
            checkServerIdentity: () => undefined,
        });

        this.dataSockets.set(userId, dataSocket);

        const dataDecoder = new MessageDecoder();

        dataSocket.on("secureConnect", () => {
            // Send CONNECT message
            dataSocket.write(Message.createConnectMsg(userId).encode());
        });

        dataSocket.on("data", (data: Buffer) => {
            try {
                dataDecoder.append(data);
                let msg: Message | null;
                while ((msg = dataDecoder.decode()) !== null) {
                    if (msg.type === Message.DATA) {
                        proxySocket.write(msg.data);
                    }
                }
            } catch (err: any) {
                console.error(
                    `EP>>ClientProxy>> Decode error: ${err.message}. Disconnecting...`,
                );
                dataSocket.destroy();
                proxySocket.destroy();
            }
        });

        proxySocket.on("data", (data: Buffer) => {
            if (!dataSocket.destroyed) {
                dataSocket.write(Message.createDataMsg(userId, data).encode());
            }
        });

        const cleanup = () => {
            this.closeProxyData(userId);
        };

        dataSocket.on("close", cleanup);
        dataSocket.on("error", (err) => {
            console.debug(
                `EP>>ClientProxy>> Data socket error: ${err.message}`,
            );
        });

        proxySocket.on("close", cleanup);
        proxySocket.on("error", (err) => {
            console.debug(
                `EP>>ClientProxy>> Proxy socket error: ${err.message}`,
            );
        });
    }

    private sendDisconnect(userId: string) {
        if (this.mngSocket && !this.mngSocket.destroyed) {
            this.mngSocket.write(Message.createDisConnectMsg(userId).encode());
        }
    }

    private processDisconnect(msg: Message) {
        console.debug("EP>>ClientMng>> Disconnect");
        const userId = msg.token;
        this.closeProxyData(userId);
    }

    private closeProxyData(userId: string) {
        const proxySocket = this.proxySockets.get(userId);
        if (proxySocket) {
            proxySocket.destroy();
            this.proxySockets.delete(userId);
        }

        const dataSocket = this.dataSockets.get(userId);
        if (dataSocket) {
            dataSocket.destroy();
            this.dataSockets.delete(userId);
        }

        this.sendDisconnect(userId);
    }

    private handleMngClose() {
        console.info("EP>>ClientMng>> Mng socket closed");
        if (this.mngSocket) {
            this.mngSocket.destroy();
            this.mngSocket = null;
        }

        // Close all proxy and data sockets
        for (const userId of this.dataSockets.keys()) {
            this.closeProxyData(userId);
        }
    }

    private retry() {
        if (this.waitTime > 60000) {
            this.waitTime = 1000;
        }
        this.waitTime = this.waitTime * 2;
        console.info(`next connect wait time ${this.waitTime}ms`);

        setTimeout(() => {
            console.info(
                `try connect ${this.config.serverIp}:${this.config.serverPort}...`,
            );
            this.connectMngServer();
        }, this.waitTime);
    }

    public stop() {
        this.handleMngClose();
        this.removeAllListeners();
    }
}
