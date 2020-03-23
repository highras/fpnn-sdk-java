package com.fpnn.sdk;

/**
 * Created by shiwangxing on 2017/11/29.
 */

public interface ErrorRecorderInterface {

    void recordError(String message);
    void recordError(Exception e);
    void recordError(String message, Exception e);

}
