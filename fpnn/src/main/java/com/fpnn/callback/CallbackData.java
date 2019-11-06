package com.fpnn.callback;

import java.util.Map;
import com.fpnn.FPData;

public class CallbackData {

    private FPData _data = null;

    public FPData getData() {
        return this._data;
    }

    public CallbackData(FPData data) {
        this._data = data;
    }


    private Exception _exception = null;

    public Exception getException() {
        return this._exception;
    }

    public CallbackData(Exception ex) {
        this._exception = ex;
    }

    private long _mid = 0;

    public long getMid() {
        return this._mid;
    }

    public void setMid(long value) {
        this._mid = value;
    }

    private Object _payload = null;

    public Object getPayload() {
        return this._payload;
    }

    public CallbackData(Object payload) {
        this._payload = payload;
    }


    public void checkException(boolean isAnswerException, Map data) {
        if (data == null && this._exception == null) {
            this._exception = new Exception("data is null!");
            this._payload = null;
        }

        if (this._exception == null && isAnswerException) {
            if (data.containsKey("code") && data.containsKey("ex")) {
                StringBuilder sb = new StringBuilder(30);
                sb.append(data.get("code"));
                sb.append(" : ");
                sb.append(data.get("ex"));
                this._exception = new Exception(sb.toString());
                this._payload = null;
            }
        }

        if (this._exception == null) {
            this._payload = data;
        }

        this._data = null;
    }
}
