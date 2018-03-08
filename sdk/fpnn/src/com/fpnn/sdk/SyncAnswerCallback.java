package com.fpnn.sdk;

import com.fpnn.sdk.proto.Answer;

public class SyncAnswerCallback extends AnswerCallback {

    private Answer answer;

    public void onAnswer(Answer answer) {

        synchronized (this) {
            this.answer = answer;
            notifyAll();
        }
    }
    public void onException(Answer answer, int errorCode) {

        if (answer == null) {
            answer = new Answer(getSeqNum());
            answer.fillErrorCode(errorCode);
        }

        synchronized (this) {
            this.answer = answer;
            notifyAll();
        }
    }

    public SyncAnswerCallback() {
        answer = null;
    }

    public Answer getAnswer() throws InterruptedException {

        synchronized (this) {
            while (answer == null)
                wait();
        }
        return answer;
    }
}
