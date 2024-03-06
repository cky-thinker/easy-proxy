package com.cky.proxy.common.domain;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

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
    public static final int CHECK_VALUE = 0xF0F0F0F0;
    public static final int CHECK_LENGTH = 4;
    public static final int HEADER_LENGTH = 1 + 4;

    private int check;       // 校验位：4
    private byte type;        // 1
    private int tokenLength;  // 4
    private String token;     // ?
    private int dataLength;  // 4
    private byte[] data;      // ?

    public static Buffer createConnectMsg(String token) {
        return encodeMsg(CONNECT, token, new byte[]{});
    }

    public static Buffer createConnectMsg(String token, String address) {
        return encodeMsg(CONNECT, token, address.getBytes(StandardCharsets.UTF_8));
    }

    public static Buffer createDisConnectMsg(String token) {
        return encodeMsg(DISCONNECT, token, new byte[]{});
    }

    public static Buffer createAuthMsg(String token) {
        return encodeMsg(AUTH, token, new byte[]{});
    }

    public static Buffer createDataMsg(String token, byte[] data) {
        return encodeMsg(DATA, token, data);
    }

    private static Buffer encodeMsg(byte type, String token, byte[] data) {
        byte[] tokenBytes = token.getBytes(StandardCharsets.UTF_8);
        return Buffer.buffer().appendInt(CHECK_VALUE).appendByte(type).appendInt(tokenBytes.length).appendBytes(tokenBytes).appendInt(data.length).appendBytes(data);
    }

    public static void decodeMsg(NetSocket dataSocket, Handler<Message> msgHandler) {
        RecordParser parser = RecordParser.newFixed(CHECK_LENGTH, dataSocket);
        parser.handler(checkBf -> {
            parseCheck(checkBf, parser, msgHandler);
        });
        parser.exceptionHandler(t -> {
            log.error(t.getMessage(), t);
            nextLoop(parser, msgHandler);
        });
    }

    public static Future<Message> decodeMsgPromise(NetSocket dataSocket) {
        RecordParser parser = RecordParser.newFixed(CHECK_LENGTH, dataSocket);
        parser.exceptionHandler(t -> {
            log.error(t.getMessage(), t);
            decodeMsg(parser);
        });
        return decodeMsg(parser);
    }

    public static Future<Message> decodeMsg(RecordParser parser) {
        Promise<Message> checkPromise = Promise.promise();
        parser.fixedSizeMode(CHECK_LENGTH);
        parser.handler(checkBf -> {
            int check = checkBf.getInt(0);
            if (check != CHECK_VALUE) {
                checkPromise.fail("校验头不匹配 " + check);
                decodeMsg(parser);
            } else {
                Message message = new Message();
                message.setCheck(check);
                checkPromise.complete(message);
            }
        });
        return checkPromise.future().transform(result -> {
            Promise<Message> promise = Promise.promise();
            Message message = result.result();
            parser.fixedSizeMode(HEADER_LENGTH);
            parser.handler(headerBuffer -> {
                message.setType(headerBuffer.getByte(0));
                message.setTokenLength(headerBuffer.getInt(1));
                promise.complete(message);
            });
            return promise.future();
        }).transform(result -> {
            Promise<Message> promise = Promise.promise();
            Message message = result.result();
            parser.fixedSizeMode(message.getTokenLength());
            parser.handler(tokenBf -> {
                String token1 = new String(tokenBf.getBytes(), StandardCharsets.UTF_8);
                message.setToken(token1);
                promise.complete(message);
            });
            return promise.future();
        }).transform(result -> {
            Promise<Message> promise = Promise.promise();
            Message message = result.result();
            parser.fixedSizeMode(message.getTokenLength());
            parser.handler(tokenBf -> {
                String token1 = new String(tokenBf.getBytes(), StandardCharsets.UTF_8);
                message.setToken(token1);
                promise.complete(message);
            });
            return promise.future();
        }).transform(result -> {
            Promise<Message> promise = Promise.promise();
            Message message = result.result();
            parser.fixedSizeMode(4);
            parser.handler(dataLengthBf -> {
                int dataLength1 = dataLengthBf.getInt(0);
                message.setDataLength(dataLength1);
                if (dataLength1 == 0) {
                    message.setData(new byte[]{});
                    promise.complete(message);
                    decodeMsg(parser);
                } else {
                    parser.fixedSizeMode(dataLength1);
                    parser.handler(dataBf -> {
                        byte[] bytes = dataBf.getBytes();
                        message.setData(bytes);
                        promise.complete(message);
                        decodeMsg(parser);
                    });
                }
            });
            return promise.future();
        });
    }

    private static void parseCheck(Buffer checkBf, RecordParser parser, Handler<Message> msgHandler) {
        int check = checkBf.getInt(0);
        if (check != CHECK_VALUE) {
            log.error("校验头不匹配 {}", check);
            nextLoop(parser, msgHandler);
        } else {
            Message message = new Message();
            message.setCheck(check);
            parser.fixedSizeMode(HEADER_LENGTH);
            parser.handler(headerBuffer -> {
                message.setType(headerBuffer.getByte(0));
                message.setTokenLength(headerBuffer.getInt(1));
                parser.fixedSizeMode(message.getTokenLength());
                parser.handler(tokenBf -> {
                    String token = new String(tokenBf.getBytes(), StandardCharsets.UTF_8);
                    message.setToken(token);
                    parser.fixedSizeMode(4);
                    parser.handler(dataLengthBf -> {
                        int dataLength = dataLengthBf.getInt(0);
                        message.setDataLength(dataLength);
                        if (dataLength == 0) {
                            message.setData(new byte[]{});
                            nextLoop(parser, msgHandler);
                            msgHandler.handle(message);
                        } else {
                            parser.fixedSizeMode(dataLength);
                            parser.handler(dataBf -> {
                                byte[] bytes = dataBf.getBytes();
                                message.setData(bytes);
                                nextLoop(parser, msgHandler);
                                msgHandler.handle(message);
                            });
                        }
                    });
                });
            });
        }
    }

    // 将解析器还原到初始状态
    private static void nextLoop(RecordParser parser, Handler<Message> msgHandler) {
        parser.fixedSizeMode(CHECK_LENGTH);
        parser.handler(headerBf -> parseCheck(headerBf, parser, msgHandler));
    }
}
