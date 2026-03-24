package com.cky.proxy.common.domain;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 传输协议
 */
@Slf4j
@Data
public class Message {
    // -----  消息类型 -----
    public static final byte AUTH = (byte) 0x01;
    public static final byte CONNECT = (byte) 0x02;
    public static final byte DISCONNECT = (byte) 0x03;
    public static final byte DATA = (byte) 0x04;

    // -----  协议 -----
    // check | type | token-length | token | data-length | data
    public static final int CHECK_VALUE = 0xF0F0F0F0;

    private int check;       // 校验位：4
    private byte type;        // 1
    private int tokenLength;  // 4
    private String token;     // ?
    private int dataLength;  // 4
    private byte[] data;      // ?

    public static Message createConnectMsg(String token) {
        return createMsg(CONNECT, token, new byte[]{});
    }

    public static Message createConnectMsg(String token, String address) {
        return createMsg(CONNECT, token, address.getBytes(StandardCharsets.UTF_8));
    }

    public static Message createDisConnectMsg(String token) {
        return createMsg(DISCONNECT, token, new byte[]{});
    }

    public static Message createAuthMsg(String token) {
        return createMsg(AUTH, token, new byte[]{});
    }

    public static Message createDataMsg(String token, byte[] data) {
        return createMsg(DATA, token, data);
    }

    private static Message createMsg(byte type, String token, byte[] data) {
        Message msg = new Message();
        msg.setCheck(CHECK_VALUE);
        msg.setType(type);
        byte[] tokenBytes = token.getBytes(StandardCharsets.UTF_8);
        msg.setTokenLength(tokenBytes.length);
        msg.setToken(token);
        msg.setDataLength(data.length);
        msg.setData(data);
        return msg;
    }

    public static Message readMsg(DataInputStream in) throws IOException {
        int check = in.readInt();
        if (check != CHECK_VALUE) {
            throw new IOException("校验头不匹配 " + check);
        }
        Message msg = new Message();
        msg.setCheck(check);
        msg.setType(in.readByte());

        int tokenLength = in.readInt();
        msg.setTokenLength(tokenLength);
        if (tokenLength > 0) {
            byte[] tokenBytes = new byte[tokenLength];
            in.readFully(tokenBytes);
            msg.setToken(new String(tokenBytes, StandardCharsets.UTF_8));
        } else {
            msg.setToken("");
        }

        int dataLength = in.readInt();
        msg.setDataLength(dataLength);
        if (dataLength > 0) {
            byte[] dataBytes = new byte[dataLength];
            in.readFully(dataBytes);
            msg.setData(dataBytes);
        } else {
            msg.setData(new byte[]{});
        }
        return msg;
    }

    public void writeTo(DataOutputStream out) throws IOException {
        out.writeInt(this.check);
        out.writeByte(this.type);
        byte[] tokenBytes = this.token.getBytes(StandardCharsets.UTF_8);
        out.writeInt(tokenBytes.length);
        if (tokenBytes.length > 0) {
            out.write(tokenBytes);
        }
        out.writeInt(this.dataLength);
        if (this.dataLength > 0) {
            out.write(this.data);
        }
        out.flush();
    }
}