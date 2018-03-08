package com.fpnn.sdk.proto;

import com.fpnn.sdk.ErrorCode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by shiwangxing on 2017/11/29.
 */

public class Answer extends Message {

    private boolean errorAnswer;
    private int seqNum;

    public Answer(Quest quest) {
        this.errorAnswer = false;
        this.seqNum = quest.getSeqNum();
    }

    public Answer(int seqNum) {
        this.errorAnswer = false;
        this.seqNum = seqNum;
    }

    public Answer(int seqNum, boolean error, Map payload) {
        this.errorAnswer = error;
        this.seqNum = seqNum;
        this.payload = payload;
    }

    public void fillErrorCode(int errorCode) {
        this.errorAnswer = true;
        payload = new HashMap();
        payload.put("code", errorCode);
    }

    public void fillErrorInfo(int errorCode) {
        fillErrorCode(errorCode);
    }

    public void fillErrorInfo(int errorCode, String message) {
        fillErrorInfo(errorCode);
        payload.put("ex", message);
    }

    public void fillErrorInfo(int errorCode, String message, String raiser) {
        fillErrorInfo(errorCode);
        payload.put("ex", message);
        payload.put("raiser", raiser);
    }

    public ByteBuffer rawData() throws IOException {

        ProtocolPackageBuilder builder = new ProtocolPackageBuilder();
        return builder.buildAnswerHeader(errorAnswer, seqNum, this);
    }

    public int getErrorCode() {
        if (!errorAnswer)
            return ErrorCode.FPNN_EC_OK.value();

        Object obj = get("code");
        if (obj != null) {
            if (obj instanceof Long)
                return (int)obj;

            if (obj instanceof Integer)
                return (int)obj;

            if (obj instanceof Short)
                return (int)obj;
        }

        return ErrorCode.FPNN_EC_CORE_UNKNOWN_ERROR.value();
    }

    public String getErrorMessage() {

        if (!errorAnswer)
            return null;

        return (String) get("ex", "");
    }

    public boolean isErrorAnswer() {
        return errorAnswer;
    }

    public int getSeqNum() {
        return seqNum;
    }
}
