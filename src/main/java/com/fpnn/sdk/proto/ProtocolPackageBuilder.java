package com.fpnn.sdk.proto;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ProtocolPackageBuilder {

    private byte[] header;

    public ProtocolPackageBuilder() {

        header = new byte[16];

        header[0] = 0x46;
        header[1] = 0x50;
        header[2] = 0x4e;
        header[3] = 0x4e;

        header[4] = 0x1;
        header[5] = 0;
        header[5] |= 0x80;
    }

    private void fillLittleEndianInt(int value, int idx) {
        header[idx + 0] = (byte) (value & 0xFF);
        header[idx + 1] = (byte) ((value >> 8) & 0xFF);
        header[idx + 2] = (byte) ((value >> 16) & 0xFF);
        header[idx + 3] = (byte) ((value >> 24) & 0xFF);
    }

    public ByteBuffer buildOneWayQuestHeader(String method, Message message) throws IOException {

        byte[] utf8method = method.getBytes("UTF-8");

        header[6] = 0;
        header[7] = (byte)utf8method.length;

        byte[] payloadBin = message.raw();
        int length = 12 + utf8method.length + payloadBin.length;

        fillLittleEndianInt(payloadBin.length, 8);

        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.clear();

        buf.put(header, 0, 12);
        buf.put(utf8method);
        buf.put(payloadBin);

        buf.flip();
        return buf;
    }

    public ByteBuffer buildTwoWayQuestHeader(String method, int seqNum, Message message) throws IOException {

        byte[] utf8method = method.getBytes("UTF-8");

        header[6] = 1;
        header[7] = (byte)utf8method.length;

        byte[] payloadBin = message.raw();
        int length = 16 + utf8method.length + payloadBin.length;

        fillLittleEndianInt(payloadBin.length, 8);
        fillLittleEndianInt(seqNum, 12);

        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.clear();

        buf.put(header, 0, 16);
        buf.put(utf8method);
        buf.put(payloadBin);

        buf.flip();
        return buf;
    }

    public ByteBuffer buildAnswerHeader(boolean isError, int seqNum, Message message) throws IOException {

        header[6] = 2;
        header[7] = (byte)(isError ? 1 : 0);

        byte[] payloadBin = message.raw();
        int length = 16 + payloadBin.length;

        fillLittleEndianInt(payloadBin.length, 8);
        fillLittleEndianInt(seqNum, 12);

        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.clear();

        buf.put(header, 0, 16);
        buf.put(payloadBin);

        buf.flip();
        return buf;
    }
}
