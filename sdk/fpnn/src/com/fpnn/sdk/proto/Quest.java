package com.fpnn.sdk.proto;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Created by shiwangxing on 2017/11/29.
 */

public class Quest extends Message {

    private int seqNum;
    private boolean isOneWay;
    private String method;

    private static class SeqNumGenerator {

        static private int count = 0;

        static public synchronized int gen() {
            if (count == 0)
                count = (int)(System.currentTimeMillis() % 1000000);

            return ++count;
        }
    }

    public Quest(String method) {
        this.method = method;
        this.isOneWay = false;
        this.seqNum = SeqNumGenerator.gen();
    }

    public Quest(String method, boolean isOneWay) {
        this.method = method;
        this.isOneWay = isOneWay;
        this.seqNum = SeqNumGenerator.gen();
    }

    public Quest(String method, int seqNum, boolean isOneWay, Map payload) {
        super(payload);
        this.method = method;
        this.isOneWay = isOneWay;
        this.seqNum = seqNum;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public String method() {
        return method;
    }

    public boolean isOneWay() {
        return isOneWay;
    }

    public boolean isTwoWay() {
        return !isOneWay;
    }

    public ByteBuffer rawData() throws IOException {

        ProtocolPackageBuilder builder = new ProtocolPackageBuilder();
        if (isOneWay == false)
            return builder.buildTwoWayQuestHeader(method, seqNum, this);
        else
            return builder.buildOneWayQuestHeader(method, this);
    }
}
