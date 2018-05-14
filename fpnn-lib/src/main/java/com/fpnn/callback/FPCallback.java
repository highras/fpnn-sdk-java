package com.fpnn.callback;

import com.fpnn.FPData;

import java.util.Map;

public class FPCallback {

    public interface ICallback {

        void callback(FPCallback fpcb);
    }


    private FPData _data = null;

    public FPData getData() {

        return this._data;
    }

    public FPCallback(FPData data) {

        this._data = data;
    }


    private Exception _exception = null;

    public Exception getException() {

        return this._exception;
    }

    public FPCallback(Exception ex) {

        this._exception = ex;
    }


    private Object _payload = null;

    public Object getPayload() {

        return this._payload;
    }

    public FPCallback(Object payload) {

        this._payload = payload;
    }


    public void checkException(Map data) {

        if (this._exception == null) {

            if (data == null) {

                this._exception = new Exception("data is null!");
            } else if (data.containsKey("code") && data.containsKey("ex")) {

                this._exception = new Exception("code: ".concat(data.get("code").toString()).concat(", ex: ").concat(data.get("ex").toString()));
            }
        }

        if (this._exception == null) {

            this._payload = data;
        }

        this._data = null;
    }
}
