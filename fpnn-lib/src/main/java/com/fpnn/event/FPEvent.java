package com.fpnn.event;

import com.fpnn.FPData;

import java.nio.channels.SocketChannel;
import java.util.EventListener;
import java.util.EventObject;

public class FPEvent extends EventObject {

    public interface IListener extends EventListener {

        void fpEvent(FPEvent event);
    }


    private String _type;

    public String getType() {

        return this._type;
    }

    public FPEvent(Object source, String type) {

        super(source);
        this._type = type;
    }


    private FPData _data = null;

    public FPData getData() {

        return this._data;
    }

    public FPEvent(Object source, String type, FPData data) {

        super(source);
        this._type = type;
        this._data = data;
    }


    private SocketChannel _socket = null;

    public SocketChannel getSocket() {

        return this._socket;
    }

    public FPEvent(Object source, String type, SocketChannel socket) {

        super(source);
        this._type = type;
        this._socket = socket;
    }


    private Exception _exception = null;

    public Exception getException() {

        return this._exception;
    }

    public FPEvent(Object source, String type, Exception ex) {

        super(source);
        this._type = type;
        this._exception = ex;
    }


    private long _timestamp = 0;

    public long getTimestamp() {

        return this._timestamp;
    }

    public FPEvent(Object source, String type, long timestamp) {

        super(source);
        this._type = type;
        this._timestamp = timestamp;
    }


    private Object _payload;

    public Object getPayload() {

        return this._payload;
    }

    public FPEvent(Object source, String type, Object payload) {

        super(source);
        this._type = type;
        this._payload = payload;
    }
}
