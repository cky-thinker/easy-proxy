export class Message {
    static readonly AUTH = 0x01;
    static readonly CONNECT = 0x02;
    static readonly DISCONNECT = 0x03;
    static readonly DATA = 0x04;
    static readonly CHECK_VALUE = 0xf0f0f0f0;

    constructor(
        public type: number,
        public token: string,
        public data: Buffer = Buffer.alloc(0),
    ) {}

    static createConnectMsg(token: string, address?: string): Message {
        const data = address ? Buffer.from(address, "utf-8") : Buffer.alloc(0);
        return new Message(this.CONNECT, token, data);
    }

    static createDisConnectMsg(token: string): Message {
        return new Message(this.DISCONNECT, token);
    }

    static createAuthMsg(token: string): Message {
        return new Message(this.AUTH, token);
    }

    static createDataMsg(token: string, data: Buffer): Message {
        return new Message(this.DATA, token, data);
    }

    encode(): Buffer {
        const tokenBuf = Buffer.from(this.token, "utf-8");
        const buffer = Buffer.alloc(
            4 + 1 + 4 + tokenBuf.length + 4 + this.data.length,
        );

        let offset = 0;
        buffer.writeUInt32BE(Message.CHECK_VALUE, offset);
        offset += 4;

        buffer.writeUInt8(this.type, offset);
        offset += 1;

        buffer.writeInt32BE(tokenBuf.length, offset);
        offset += 4;

        tokenBuf.copy(buffer, offset);
        offset += tokenBuf.length;

        buffer.writeInt32BE(this.data.length, offset);
        offset += 4;

        this.data.copy(buffer, offset);

        return buffer;
    }
}

export class MessageDecoder {
    private buffer: Buffer = Buffer.alloc(0);

    append(data: Buffer) {
        this.buffer = Buffer.concat([this.buffer, data]);
    }

    decode(): Message | null {
        if (this.buffer.length < 13) return null; // 4 + 1 + 4 + 0 + 4 + 0 = 13 bytes min

        let offset = 0;
        const check = this.buffer.readUInt32BE(offset);
        if (check !== Message.CHECK_VALUE) {
            // Find next valid check value to recover
            const nextCheckIndex = this.buffer.indexOf(
                Buffer.from([0xf0, 0xf0, 0xf0, 0xf0]),
                1,
            );
            if (nextCheckIndex !== -1) {
                this.buffer = this.buffer.subarray(nextCheckIndex);
                return this.decode();
            } else {
                this.buffer = Buffer.alloc(0);
                throw new Error(`Invalid check value: ${check.toString(16)}`);
            }
        }
        offset += 4;

        const type = this.buffer.readUInt8(offset);
        offset += 1;

        const tokenLength = this.buffer.readInt32BE(offset);
        offset += 4;

        if (this.buffer.length < offset + tokenLength) return null;

        const token = this.buffer.toString(
            "utf-8",
            offset,
            offset + tokenLength,
        );
        offset += tokenLength;

        // need 4 bytes for dataLength
        if (this.buffer.length < offset + 4) return null;
        const dataLength = this.buffer.readInt32BE(offset);
        offset += 4;

        if (this.buffer.length < offset + dataLength) return null;

        const data = Buffer.from(
            this.buffer.subarray(offset, offset + dataLength),
        );
        offset += dataLength;

        this.buffer = this.buffer.subarray(offset);

        return new Message(type, token, data);
    }
}
