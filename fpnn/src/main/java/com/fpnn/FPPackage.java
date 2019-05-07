package com.fpnn;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

public class FPPackage {

    public String getKeyCallback(FPData data) {

        return "FPNN_".concat(String.valueOf(data.getSeq()));
    }

    public boolean isHttp(FPData data) {

        return Arrays.equals(data.getMagic(), FPConfig.HTTP_MAGIC);
    }

    public boolean isTcp(FPData data) {

        return Arrays.equals(data.getMagic(), FPConfig.TCP_MAGIC);
    }

    public boolean isMsgPack(FPData data) {

        return 1 == data.getFlag();
    }

    public boolean isJson(FPData data) {

        return 0 == data.getFlag();
    }

    public boolean isOneWay(FPData data) {

        return 0 == data.getMtype();
    }

    public boolean isTwoWay(FPData data) {

        return 1 == data.getMtype();
    }

    public boolean isQuest(FPData data) {

        return this.isTwoWay(data) || this.isOneWay(data);
    }

    public boolean isAnswer(FPData data) {

        return 2 == data.getMtype();
    }

    public boolean isSupportPack(FPData data) {

        return this.isMsgPack(data) != this.isJson(data);
    }

    public boolean checkVersion(FPData data) {

        if (data.getVersion() < 0) {

            return false;
        }

        if (data.getVersion() >= FPConfig.FPNN_VERSION.length) {

            return false;
        }

        return true;
    }

    public ByteBuffer enCode(FPData data) throws IOException {

        ByteBuffer buf = null;

        if (this.isOneWay(data)) {

            buf = this.enCodeOneway(data);
        }

        if (this.isTwoWay(data)) {

            buf = this.enCodeTwoway(data);
        }

        if (this.isAnswer(data)) {

            buf = this.enCodeAnswer(data);
        }

        return buf;
    }

    public ByteBuffer enCodeOneway(FPData data) throws IOException {

        ByteBuffer buf = this.buildHeader(data, 12 + data.getSS() + data.getPsize());

        buf.put((byte) data.getSS());
        buf.put(this.Uint32BEToLEByte(data.getPsize()));
        buf.put(data.getMethod().getBytes("utf-8"));

        if (this.isJson(data)) {

            buf.put(data.jsonPayload().getBytes("utf-8"));
        }

        if (this.isMsgPack(data)) {

            buf.put(data.msgpackPayload());
        }

        return buf;
    }

    public ByteBuffer enCodeTwoway(FPData data) throws IOException {

        ByteBuffer buf = this.buildHeader(data, 16 + data.getSS() + data.getPsize());

        buf.put((byte) data.getSS());
        buf.put(this.Uint32BEToLEByte(data.getPsize()));
        buf.put(this.Uint32BEToLEByte(data.getSeq()));
        buf.put(data.getMethod().getBytes("utf-8"));

        if (this.isJson(data)) {

            buf.put(data.jsonPayload().getBytes("utf-8"));
        }

        if (this.isMsgPack(data)) {

            buf.put(data.msgpackPayload());
        }

        return buf;
    }

    public ByteBuffer enCodeAnswer(FPData data) throws IOException {

        ByteBuffer buf = this.buildHeader(data, 16 + data.getPsize());

        buf.put((byte) data.getSS());
        buf.put(this.Uint32BEToLEByte(data.getPsize()));
        buf.put(this.Uint32BEToLEByte(data.getSeq()));

        if (this.isJson(data)) {

            buf.put(data.jsonPayload().getBytes("utf-8"));
        }

        if (this.isMsgPack(data)) {

            buf.put(data.msgpackPayload());
        }

        return buf;
    }

    public FPData peekHead(byte[] bytes) {

        if (bytes.length < 12) {

            return null;
        }

        FPData peek = new FPData();

        byte[] bs = new byte[4];
        System.arraycopy(bytes, 0, bs, 0, bs.length);

        peek.setMagic(bs);
        peek.setVersion(Arrays.binarySearch(FPConfig.FPNN_VERSION, bytes[4]));

        if (bytes[5] == FPConfig.FP_FLAG[0]) {

            peek.setFlag(0);
        }

        if (bytes[5] == FPConfig.FP_FLAG[1]) {

            peek.setFlag(1);
        }

        peek.setMtype(Arrays.binarySearch(FPConfig.FP_MESSAGE_TYPE, bytes[6]));
        peek.setSS(bytes[7]);

        bs = new byte[4];
        System.arraycopy(bytes, 8, bs, 0, bs.length);

        peek.setPsize(this.LEByteToUint32BE(bs));

        return peek;
    }

    public boolean deCode(FPData data) {

        byte[] bytes = data.buffer.array();

        byte[] bs = new byte[bytes.length - 12];
        System.arraycopy(bytes, 12, bs, 0, bs.length);

        if (this.isOneWay(data)) {

            return this.deCodeOneWay(bs, data);
        }

        if (this.isTwoWay(data)) {

            return this.deCodeTwoWay(bs, data);
        }

        if (this.isAnswer(data)) {

            return this.deCodeAnswer(bs, data);
        }

        return false;
    }

    public boolean deCodeOneWay(byte[] bytes, FPData data) {

        if (bytes.length != data.getSS() + data.getPsize()) {

            return false;
        }

        byte[] bs = new byte[data.getSS()];
        System.arraycopy(bytes, 0, bs, 0, bs.length);

        data.setMethod(this.bufferToString(ByteBuffer.wrap(bs)));

        bs = new byte[data.getPsize()];
        System.arraycopy(bytes, data.getSS(), bs, 0, bs.length);

        if (this.isJson(data)) {

            data.setPayload(this.bufferToString(ByteBuffer.wrap(bs)));
        }

        if (this.isMsgPack(data)) {

            data.setPayload(bs);
        }

        return true;
    }

    public boolean deCodeTwoWay(byte[] bytes, FPData data) {

        if (bytes.length != 4 + data.getSS() + data.getPsize()) {

            return false;
        }

        byte[] bs = new byte[4];
        System.arraycopy(bytes, 0, bs, 0, bs.length);

        data.setSeq(this.LEByteToUint32BE(bs));

        bs = new byte[data.getSS()];
        System.arraycopy(bytes, 4, bs, 0, bs.length);

        data.setMethod(this.bufferToString(ByteBuffer.wrap(bs)));

        bs = new byte[data.getPsize()];
        System.arraycopy(bytes, 4 + data.getSS(), bs, 0, bs.length);

        if (this.isJson(data)) {

            data.setPayload(this.bufferToString(ByteBuffer.wrap(bs)));
        }

        if (this.isMsgPack(data)) {

            data.setPayload(bs);
        }

        return true;
    }

    public boolean deCodeAnswer(byte[] bytes, FPData data) {

        if (bytes.length != 4 + data.getPsize()) {

            return false;
        }

        byte[] bs = new byte[4];
        System.arraycopy(bytes, 0, bs, 0, bs.length);

        data.setSeq(this.LEByteToUint32BE(bs));

        bs = new byte[data.getPsize()];
        System.arraycopy(bytes, 4, bs, 0, bs.length);

        if (this.isJson(data)) {

            data.setPayload(this.bufferToString(ByteBuffer.wrap(bs)));
        }

        if (this.isMsgPack(data)) {

            data.setPayload(bs);
        }

        return true;
    }

    private ByteBuffer buildHeader(FPData data, int size) {

        ByteBuffer buf = ByteBuffer.allocate(size);

        if (this.isHttp(data)) {

            buf.put(FPConfig.HTTP_MAGIC);
        }

        if (this.isTcp(data)) {

            buf.put(FPConfig.TCP_MAGIC);
        }

        buf.put(FPConfig.FPNN_VERSION[data.getVersion()]);

        if (this.isJson(data)) {

            buf.put(FPConfig.FP_FLAG[data.getFlag()]);
        }

        if (this.isMsgPack(data)) {

            buf.put(FPConfig.FP_FLAG[data.getFlag()]);
        }

        buf.put(FPConfig.FP_MESSAGE_TYPE[data.getMtype()]);

        return buf;
    }

    public byte[] Uint32BEToLEByte(int value) {

        byte[] b = new byte[4];
        b[0] = (byte) (value & 0xFF);
        b[1] = (byte) ((value >> 8) & 0xFF);
        b[2] = (byte) ((value >> 16) & 0xFF);
        b[3] = (byte) ((value >> 24) & 0xFF);

        return b;
    }

    public int LEByteToUint32BE(byte[] b) {

        return (b[0] & 0xFF)
                | ((b[1] & 0xFF) << 8)
                | ((b[2] & 0xFF) << 16)
                | ((b[3] & 0xFF) << 24);
    }

    public String bufferToString(ByteBuffer buf) {

        Charset charset = Charset.forName("utf-8");
        return charset.decode(buf).toString();
    }
}
