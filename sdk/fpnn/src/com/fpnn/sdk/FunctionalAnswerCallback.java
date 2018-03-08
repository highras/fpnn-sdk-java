package com.fpnn.sdk;

import com.fpnn.sdk.proto.Answer;

/**
 * Created by shiwangxing on 2017/11/30.
 */

@FunctionalInterface
public interface FunctionalAnswerCallback {
    void onAnswer(Answer answer, int errorCode);
}
