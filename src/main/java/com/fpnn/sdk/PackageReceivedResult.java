package com.fpnn.sdk;

import com.fpnn.sdk.proto.Answer;
import com.fpnn.sdk.proto.MessagePayloadUnpacker;
import com.fpnn.sdk.proto.Quest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by shiwangxing on 2017/12/5.
 */

public class PackageReceivedResult {
    public boolean success;
    public int errorCode;

    private LinkedList<ByteBuffer> caches;
    private LinkedList<Answer> answerList;
    private LinkedList<Quest> questList;

     PackageReceivedResult() {
        success = true;
        errorCode = ErrorCode.FPNN_EC_OK.value();
        caches = null;
        answerList = null;
        questList = null;
    }

    public void setError(int errorCode) {
        this.success = false;
        this.errorCode = errorCode;
    }

    public void addPackage(ByteBuffer header, ByteBuffer body) {
        if (caches == null)
            caches = new LinkedList<>();

        caches.add(header);
        caches.add(body);
    }

    private int getSeqNum(ByteBuffer bodyBuffer) {
        return (bodyBuffer.get(0) & 0xFF)
                | ((bodyBuffer.get(1) & 0xFF) << 8)
                | ((bodyBuffer.get(2) & 0xFF) << 16)
                | ((bodyBuffer.get(3) & 0xFF) << 24);
    }
    public void processPackage() {

        if (caches == null)
            return;

        answerList = new LinkedList<>();
        questList = new LinkedList<>();
        
        while (caches.size() > 0) {
            ByteBuffer headerBuffer = caches.getFirst();
            caches.remove();
            ByteBuffer bodyBuffer = caches.getFirst();
            caches.remove();

            byte mtype = headerBuffer.get(6);
            byte ss = headerBuffer.get(7);
            int payloadLength = (headerBuffer.get(8) & 0xFF)
                    | ((headerBuffer.get(9) & 0xFF) << 8)
                    | ((headerBuffer.get(10) & 0xFF) << 16)
                    | ((headerBuffer.get(11) & 0xFF) << 24);

            String packageType = "unknown";
            try {
                if (mtype == 2) {
                    packageType = "Answer";

                    int seqNum = getSeqNum(bodyBuffer);

                    byte[] data = bodyBuffer.array();
                    MessagePayloadUnpacker unpacker = new MessagePayloadUnpacker(data, 4, payloadLength);
                    Map payload = unpacker.unpack();

                    Answer answer = new Answer(seqNum, ss != 0, payload);
                    answerList.add(answer);

                } else {
                    if (!success)
                        continue;

                    if (mtype == 1) {
                        packageType = "Two Way Quest";

                        int seqNum = getSeqNum(bodyBuffer);

                        byte[] data = bodyBuffer.array();
                        String method = new String(data, 4, ss, Charset.forName("UTF-8"));
                        MessagePayloadUnpacker unpacker = new MessagePayloadUnpacker(data, 4 + ss, payloadLength);
                        Map payload = unpacker.unpack();

                        Quest quest = new Quest(method, seqNum, false, payload);
                        questList.add(quest);

                    } else if (mtype == 0) {
                        packageType = "One Way Quest";

                        byte[] data = bodyBuffer.array();
                        String method = new String(data, 0, ss, Charset.forName("UTF-8"));
                        MessagePayloadUnpacker unpacker = new MessagePayloadUnpacker(data, ss, payloadLength);
                        Map payload = unpacker.unpack();

                        Quest quest = new Quest(method, 0, true, payload);
                        questList.add(quest);

                    } else {
                        ErrorRecorder.record("Unsupported package type. package payload length: "
                                + payloadLength + ". mType: " + mtype);
                    }
                }
            } catch (IOException e) {
                ErrorRecorder.record("Decoding package exception. package payload length: "
                        + payloadLength + ". Package type: " + packageType, e);
            }
        }
    }

    public LinkedList<Answer> getAnswerList() {
         return answerList;
    }
    public LinkedList<Quest> getQuestList() {
         return questList;
    }
}
