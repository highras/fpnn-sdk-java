package com.fpnn.sdk;

import com.fpnn.sdk.proto.Answer;

/**
 * Created by shiwangxing on 2017/11/29.
 */

public abstract class AnswerCallback {

    public abstract void onAnswer(Answer answer);
    public abstract void onException(Answer answer, int errorCode);

    private long sentMilliseconds;
    private long answeredMilliseconds;
    private long timeoutMilliseconds;
    private int seqNum;

    private FunctionalAnswerCallback functionalCallback;

    public AnswerCallback() {
        sentMilliseconds = 0;
        answeredMilliseconds = 0;
        timeoutMilliseconds = 0;
        seqNum = 0;

        functionalCallback = null;
    }

    void setSentTime() {
        sentMilliseconds = System.currentTimeMillis();
    }

    void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }

    void setTimeout(int timeoutInSeconds) {
        timeoutMilliseconds = System.currentTimeMillis() + timeoutInSeconds * 1000;
    }

    void setFuncationalAnswerCallback(FunctionalAnswerCallback callback) {
        functionalCallback = callback;
    }

    long getTimeoutTime() {
        return timeoutMilliseconds;
    }

    int getSeqNum() {
        return seqNum;
    }

    public long getSentTime() {
        return sentMilliseconds;
    }

    public long getAnsweredTime() {
        return answeredMilliseconds;
    }

    public final void fillResult(Answer answer, int errorCode) {

        answeredMilliseconds = System.currentTimeMillis();

        if (functionalCallback != null) {
            functionalCallback.onAnswer(answer, errorCode);
            return;
        }

        if (errorCode == ErrorCode.FPNN_EC_OK.value())
            onAnswer(answer);
        else
            onException(answer, errorCode);
    }
}
